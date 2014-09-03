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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import divconq.hub.Hub;
import divconq.util.StringUtil;
import divconq.work.TaskRun;

public class ReplyService implements IService {
	protected ConcurrentMap<String, ServiceResult> handlers = new ConcurrentHashMap<String, ServiceResult>();
	protected AtomicLong nextTag = new AtomicLong();
	
	public ReplyService() {
	}
	
	public String serviceName() {
		return "Replies";
	}

	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String tag = msg.getFieldAsString("Tag");
		
		if (StringUtil.isNotEmpty(tag)) {
			ServiceResult resp = this.handlers.remove(tag);
	
			try {
				if (resp != null) {				
					Hub.instance.getCountManager().allocateSetNumberCounter("dcBusReplyHandlers", this.handlers.size());
					
					resp.setReply(msg);
					resp.completed();
				}
			}
			catch (Exception x) {	
				// TODO log
			}
		}
		
		request.complete();
	}

	public String registerForReply(Message msg, ServiceResult resp) {
		String tag = this.nextTag.getAndIncrement() + "";	
		
		msg.setField("RespondTo", this.serviceName());
		msg.setField("RespondTag", tag);
		
		this.handlers.put(tag, resp);		// TODO consider keeping a handle to the task context so we can restore it in handler
		
		Hub.instance.getCountManager().allocateSetNumberCounter("dcBusReplyHandlers", this.handlers.size());
		
		return tag;
	}

	// "RespondTag" header
	public void clearReply(String tag) {
		if (StringUtil.isEmpty(tag))
			return;
		
		ServiceResult resp = this.handlers.remove(tag);

		if (resp == null)
			return;
		
		Hub.instance.getCountManager().allocateSetNumberCounter("dcBusReplyHandlers", this.handlers.size());
	}

	public int size() {
		return this.handlers.size();
	}
}
