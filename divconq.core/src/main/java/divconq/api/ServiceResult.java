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
package divconq.api;

import divconq.bus.Message;
import divconq.lang.TimeoutPlan;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;

abstract public class ServiceResult extends divconq.bus.ServiceResult {
	protected ApiSession capi = null;
	
	public void setSession(ApiSession v) {
		this.capi = v;
	}
	
	// timeout on regular schedule  
	public ServiceResult() {
		super(TimeoutPlan.Regular);
	}
	
	public ServiceResult(TimeoutPlan plan) {
		super(plan);
	}
	
	public void setReply(Message v) {
		this.setResult(v);

		// TODO review, does not appear to do anything useful
		//if (!v.isFieldEmpty("Result") && !v.isFieldEmpty("Message")) 
		//	this.exit(v.getFieldAsInteger("Result", 0), v.getFieldAsString("Message"));
		
		ListStruct h = v.getFieldAsList("Messages");
		
		if (h != null)
			for (Struct st : h.getItems()) {
				RecordStruct msg = (RecordStruct)st;
				
				this.opcontext.log(msg);
			}
	}
	
	@Override
	public boolean abandon() {
		if (super.abandon()) {
			this.capi.getReplyService().clearReply(this.replytag);
			return true;
		}
		
		return false;
	}
}
