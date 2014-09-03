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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import divconq.bus.Message;

public class ReplyService {
	protected ConcurrentMap<String, ServiceResult> handlers = new ConcurrentHashMap<>();
	protected AtomicLong nextTag = new AtomicLong();

	public void handle(Message msg) {
		String tag = msg.getFieldAsString("Tag");
		
		if (tag == null)
			return;
		
		ServiceResult resp = this.handlers.remove(tag);
		
		// we need to restore the local operation context
		// which happens naturally via .completed()
		if (resp != null) 
			try {
				resp.setReply(msg);
				resp.completed();
			}
			catch (Exception x) {	
				// TODO log
			}
		
		return;
	}

	public void registerForReply(Message msg, ServiceResult resp) {
		String tag = this.nextTag.getAndIncrement() + "";	
		
		msg.setField("RespondTo", "Replies");
		msg.setField("RespondTag", tag);
		
		resp.setReplyTag(tag);
		
		this.handlers.put(tag, resp);
	}

	public void registerForReplySerial(Message msg, ServiceResult resp) {
		String tag = "SendWait";	
		
		msg.setField("RespondTo", "Replies");
		msg.setField("RespondTag", tag);
		
		resp.setReplyTag(tag);
		
		this.handlers.put(tag, resp);
	}

	// "RespondTag" header
	public void clearReply(String tag) {
		this.handlers.remove(tag);
	}

	public int size() {
		return this.handlers.size();
	}

	public void forgetReplyAll() {
		for (ServiceResult resp : this.handlers.values()) {
			try {
				resp.error("Session disconnect, response is lost");
				resp.completed();
			}
			catch(Exception x) {
				// TODO log
			}
		}
		
		this.handlers.clear();
	}
}
