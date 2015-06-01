/* ************************************************************************
#
#  DivConq
#
#  http://divconq.com/
#
#  Copyright:
#    Copyright 2014 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package divconq.web;

import divconq.bus.Message;
import divconq.bus.MessageUtil;
import divconq.bus.ServiceResult;
import divconq.lang.Memory;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationResult;
import divconq.log.Logger;
import divconq.struct.CompositeParser;
import divconq.struct.CompositeStruct;
import divconq.struct.RecordStruct;

public class RpcHandler implements IBodyCallback {
	protected HttpContext context = null;

	public RpcHandler(HttpContext ctx) {
		this.context = ctx;
	}
	
	@Override
	public void fail() {
		this.context.sendRequestBad();
	}
	
	@Override
	public void ready(Memory mem) {
		FuncResult<CompositeStruct> pres = CompositeParser.parseJson(mem);
		
		if (pres.hasErrors()) {
			this.context.sendRequestBad();
			return;
		}
		
		CompositeStruct croot = pres.getResult();
		
		if ((croot == null) || !(croot instanceof RecordStruct)) {
			this.context.sendRequestBad();
			return;
		}
		
		this.context.getSession().setContext();
		
		RecordStruct mrec = (RecordStruct) croot;
		
		// check that the request conforms to the schema for RpcMessage
		OperationResult rootres = mrec.validate("RpcMessage");
		
		if (rootres.hasErrors()) {
			this.context.sendRequestBad();
			return;
		}
		
		// if so convert the Record into a Message for transport over our bus
		Message msg = MessageUtil.fromRecord(mrec);  
		
		//System.out.println("got rpc message: " + msg);
		
    	String sessionid = msg.getFieldAsString("Session");

    	msg.removeField("Session");
		
		// for SendForget don't wait for a callback, just return success
    	if ("SendForget".equals(msg.getFieldAsString("RespondTag"))) {
    		// send to bus
    		this.context.getSession().sendMessage(msg);
    		
    		Message rmsg = MessageUtil.success();
    		
			String currsessid = RpcHandler.this.context.getSession().getId();
			
			rmsg.setField("Session", currsessid);
			
			if ((sessionid != null) && !currsessid.equals(sessionid))
				rmsg.setField("SessionChanged", true);
    		
			// TODO pickup from mailbox
			
    		// reply to client, don't wait for response
    		this.context.send(rmsg);
			
			return;
    	}
    	
    	this.context.getSession().sendMessageWait(msg, new ServiceResult() {			
			@Override
			public void callback() {
				try {
					// if we did not get an official reply to the request then
					// it may have been a timeout.  regardless, collect messages
					// and prepare to return any payload
					Message rmsg = this.toLogMessage();
					
					// add the body (payload) if any
					Message reply = this.getResult();
					
					if (reply != null) {
						if (reply.hasField("Service"))
							rmsg.setField("Service", reply.getField("Service"));
						
						if (reply.hasField("Feature"))
							rmsg.setField("Feature", reply.getField("Feature"));
						
						if (reply.hasField("Op"))
							rmsg.setField("Op", reply.getField("Op"));
						
						if (reply.hasField("Tag"))
							rmsg.setField("Tag", reply.getField("Tag"));
						
						if (reply.hasField("FromHub"))
							rmsg.setField("FromHub", reply.getField("FromHub"));
						
						//if (reply.hasField("Body"))  - always even if null
						rmsg.setField("Body", reply.getField("Body"));
					}
					
					String currsessid = RpcHandler.this.context.getSession().getId();
					
					rmsg.setField("Session", currsessid);
					
					if ((sessionid != null) && !currsessid.equals(sessionid))
						rmsg.setField("SessionChanged", true);
					
					//System.out.println("outgoing rpc: " + rmsg);
					
					RpcHandler.this.context.send(rmsg);
				}
				catch (Exception x) {
					Logger.info("Error replying to RPC request: " + x);
					RpcHandler.this.context.sendInternalError();
				}
			}
		});				
	}
}
