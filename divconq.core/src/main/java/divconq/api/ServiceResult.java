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

		this.opcontext.logResult(v);
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
