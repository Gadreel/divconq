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
package divconq.bus;

import divconq.bus.Message;
import divconq.bus.net.StreamMessage;
import divconq.lang.op.OperationResult;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.work.TaskRun;

public class MessageUtil {
	static public Message success(String... flds) {
		Message m = new Message();
		m.setField("Result", 0);
		
		if (flds.length > 0) {
			RecordStruct body = new RecordStruct();
			
			String name = null;
			
			for (String o : flds) {
				if (o == null)
					continue;
				
				if (name != null) {
					body.setField(name, o);
					name = null;
				}
				else {
					name = o;
				}
			}
			
			m.setField("Body", body);
		}
		
		return m;
	}

	public static Message success(Struct body) {
		Message m = new Message();
		m.setField("Result", 0);
		m.setField("Body", body);
		return m;
	}
	
	static public Message error(int code, String msg) {
		OperationResult or = new OperationResult();
		or.error(code, msg);
		
		return MessageUtil.messages(or);
	}
	
	static public Message errorTr(int code, Object... params) {
		OperationResult or = new OperationResult();
		or.errorTr(code, params);
		
		return MessageUtil.messages(or);
	}
	
	static public Message messages(OperationResult ri) {
		Message m = new Message();
		
		if (ri != null) {
			m.setField("Result", ri.getCode());
			m.setField("Message", ri.getMessage());
			m.setField("Messages", ri.getMessages());
		}
		
		return m;
	}
	
	static public Message fromRecord(RecordStruct msg) {
		Message m = new Message();
		
		m.copyFields(msg);
		
		return m;
	}
	
    static boolean addressReply(Message msg, RecordStruct original) {
    	if ((msg == null) || original.isFieldEmpty("RespondTo"))
    		return false;
    	
    	msg.setField("ToHub", original.getFieldAsString("FromHub"));
    	msg.setField("Service", original.getFieldAsString("RespondTo"));
    	msg.setField("Feature", "Reply");
    	msg.setField("Op", "Deliver");
    	msg.setField("Tag", original.getFieldAsString("RespondTag"));
    	
    	return true;
    }
    
	static public StreamMessage streamError(int code, String msg) {
		OperationResult or = new OperationResult();
		or.error(code, msg);
		
		return MessageUtil.streamMessages(or);
	}
	
	static public StreamMessage streamErrorTr(int code, Object... params) {
		OperationResult or = new OperationResult();
		or.errorTr(code, params);
		
		return MessageUtil.streamMessages(or);
	}
	
	static public StreamMessage streamMessages(OperationResult ri) {
		StreamMessage m = new StreamMessage();
		
		if (ri != null) {
			m.setField("Result", ri.getCode());
			m.setField("Message", ri.getMessage());
			m.setField("Messages", ri.getMessages());
		}
		
		return m;
	}
	
	static public StreamMessage streamError(OperationResult ri, int code, String msg) {
		if (ri != null) 
			return MessageUtil.streamMessages(ri);

		StreamMessage m = new StreamMessage();
		
		m.setField("Result", code);
		m.setField("Message", msg);
		
		return m;
	}
	
	static public StreamMessage streamFromRecord(RecordStruct msg) {
		StreamMessage m = new StreamMessage();
		
		m.copyFields(msg);
		
		return m;
	}
	
    static public void streamAddressReply(RecordStruct msg, RecordStruct original) {
    	if (msg == null) 
    		return;
    	
    	msg.setField("ToHub", original.getFieldAsString("Hub"));
    	msg.setField("ToSession", original.getFieldAsString("Session"));
    	msg.setField("ToChannel", original.getFieldAsString("Channel"));
    }
    
    // assumes body will be in StreamMessage format
    static public void streamAddressReply(Message msg, RecordStruct original) {
    	if (msg == null) 
    		return;
    	
    	msg.setToHub(original.getFieldAsString("Hub"));
    	
    	RecordStruct body = msg.getFieldAsRecord("Body");
    	
    	if (body == null) {
    		body = new RecordStruct();
    		msg.setField("Body", body);
    	}
	
    	body.setField("ToHub", original.getFieldAsString("Hub"));
    	body.setField("ToSession", original.getFieldAsString("Session"));
    	body.setField("ToChannel", original.getFieldAsString("Channel"));
    }

	public static StreamMessage streamFinal() {
		return new StreamMessage("Final");
	}

	public static Message message(TaskRun request) {
		return (Message) request.getTask().getParams();
	}

	public static RecordStruct bodyAsRecord(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		return msg.getFieldAsRecord("Body");
	}

	public static ListStruct bodyAsList(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		return msg.getFieldAsList("Body");
	}
}
