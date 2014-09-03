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

import divconq.hub.Hub;
import divconq.lang.FuncCallback;
import divconq.lang.OperationContext;
import divconq.lang.TimeoutPlan;
import divconq.lang.UserContext;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.work.TaskRun;

abstract public class ServiceResult extends FuncCallback<Message> {
	protected String replytag = null;
	
	public void setReplyTag(String v) {
		this.replytag = v;
	}
	
	// timeout on regular schedule  
	public ServiceResult() {
		super(TimeoutPlan.Regular);
	}
	
	public ServiceResult(TimeoutPlan plan) {
		super(plan);
	}
	
	// timeout on regular schedule  
	public ServiceResult(TaskRun run) {
		super(run, TimeoutPlan.Regular);
	}
	
	public ServiceResult(TaskRun run, TimeoutPlan plan) {
		super(run, plan);
	}
	
	@Override
	public boolean abandon() {
		if (super.abandon()) {
			Hub.instance.getBus().getLocalHub().getReplyService().clearReply(this.replytag);
			return true;
		}
		
		return false;
	}
	
	public void setReply(Message v) {
		this.setResult(v);

		if (this.code == 0) {
			this.code = v.getFieldAsInteger("Result");
			this.message = v.getFieldAsString("Message");
		}
		
		ListStruct h = v.getFieldAsList("Messages");
		
		if (h != null)
			for (Struct st : h.getItems()) {
				RecordStruct msg = (RecordStruct)st;
				this.messages.add(msg);
				
				if ("Error".equals(msg.getFieldAsString("Level"))) {
					if (this.code == 0) {
						this.code = msg.getFieldAsInteger("Code");
						this.message = msg.getFieldAsString("Message");
					}
				}
			}
		
		UserContext usr = OperationContext.get().getUserContext(); 
		
		// TODO change only if usr != this.opcontext.getUser  
		
		// switch the user without switching the operation context, and not elevating
		this.opcontext = OperationContext.allocate(usr, this.opcontext.toBuilder());

		if (this.run != null)
			this.run.setContext(this.opcontext);
	}
	
	/**
	 * @return the service result as String
	 */
	public String getBodyAsString() {
		return this.getResult().getFieldAsString("Body");
	}
	
	/**
	 * @return the service result as Integer
	 */
	public Long getBodyAsInteger() {
		return this.getResult().getFieldAsInteger("Body");
	}

	/**
	 * @return the service result as RecordStruct
	 */
	public RecordStruct getBodyAsRec() {
		return this.getResult().getFieldAsRecord("Body");
	}

	/**
	 * @return the service result as ListStruct
	 */
	public ListStruct getBodyAsList() {
		return this.getResult().getFieldAsList("Body");
	}
}
