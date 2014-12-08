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

import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;

/*
 * Service, Attachments, Extension are all reserved headers - Feature and Op semi-reserved.  All other headers are fine as long as the don't start with _.
 */

public class Message extends RecordStruct {
	public Message() { }
	
	public Message(String service) {
		this.setField("Service", service);
	}
	
	public Message(String service, String feature, String op) {
		this.setField("Service", service);
		this.setField("Feature", feature);
		this.setField("Op", op);
	}
	
	public Message(String service, String feature, String op, Object body) {
		this.setField("Service", service);
		this.setField("Feature", feature);
		this.setField("Op", op);
		
		if (body != null)
			this.setField("Body", body);
	}
	
	public RecordStruct bodyRecord() {
		RecordStruct ret = new RecordStruct();
		this.setField("Body", ret);		
		return ret;
	}
	
	public ListStruct bodyList() {
		ListStruct ret = new ListStruct();
		this.setField("Body", ret);		
		return ret;
	}
	
	public Message withToHub(String v) {
    	this.setField("ToHub", v);
    	return this;
	}
	
	public Message withFromHub(String v) {
    	this.setField("FromHub", v);
    	return this;
	}
	
	public Message withService(String v) {
    	this.setField("Service", v);
    	return this;
	}
	
	public Message withFeature(String v) {
    	this.setField("Feature", v);
    	return this;
	}
	
	public Message withOp(String v) {
    	this.setField("Op", v);
    	return this;
	}
	
	public Message withTag(String v) {
    	this.setField("Tag", v);
    	return this;
	}

	public boolean hasErrors() {
		return MessageUtil.hasErrors(this);
	}
	
	public long getCode() {
		return MessageUtil.getCode(this);
	}
	
	public String getMessage() {
		return MessageUtil.getMessage(this);
	}
	
	public ListStruct getMessages() {
		return this.getFieldAsList("Messages");
	}

	public boolean isVerifyRequest() {
		return ("dcAuth".equals(this.getFieldAsString("Service")) 
				&& "Authentication".equals(this.getFieldAsString("Feature"))
				&& "Verify".equals(this.getFieldAsString("Op")));
	}	
}
