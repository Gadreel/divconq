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
package divconq.mail;

import java.util.Collection;

import divconq.lang.FuncResult;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;
import divconq.work.Task;
import divconq.xml.XElement;

public class MailTaskFactory {
	// abstract out the service so we can use SQL, noSQL or XML based service - or non-smtp services
	static protected String useBucket = null;
	static protected String debugBCC = null;
	static protected IMailProcessor processor = null; 
	static public RecordStruct settings = null;
	
	public static RecordStruct getSettings() {
		return MailTaskFactory.settings;
	}

	public static void setSettings(RecordStruct settings) {
		MailTaskFactory.settings = settings;
	}
	
	// we need a processor of some sort or we are dead in water
	public static void setProcessor(IMailProcessor processor) {
		MailTaskFactory.processor = processor;
	}
	
	static public FuncResult<RecordStruct> sendEmail(Task email) {
		if (MailTaskFactory.processor == null)
			return null;
		
		return MailTaskFactory.processor.submit(email);
	}
	
	static public Task createSendEmailTask(String from, String to, String subject, String body) {
		return MailTaskFactory.createSendEmailTask(from, to, subject, body, null);
	}
	
	static public Task createSendEmailTask(String from, String to, String subject, String body, Collection<? extends AbstractAttachment> attachments) {
		RecordStruct params = new RecordStruct(
			new FieldStruct("From", from),
			new FieldStruct("To", to),
			new FieldStruct("Subject", subject),
			new FieldStruct("Body", body)
		);
		
		if ((attachments != null) && (attachments.size() > 0)) {
			ListStruct alist = new ListStruct();
			
			for (AbstractAttachment attch : attachments)
				alist.addItem(attch.toParam());
			
			params.setField("Attachments", alist);		
		}
		
		return MailTaskFactory.createSendEmailTask(params);
	}
	
	static public Task createSendEmailTask(RecordStruct params) {
		if (MailTaskFactory.processor == null)
			return null;
		
		String tid = divconq.session.Session.nextTaskId("EMAIL");
		
		if (StringUtil.isNotEmpty(MailTaskFactory.debugBCC))
			params.setField("BccDebug", MailTaskFactory.debugBCC);
		
		Task task = new Task()
			.withId(tid)
			.withTitle("Send Email To " + params.getFieldAsString("To"))
			.withParams(params)
			.withBucket(MailTaskFactory.useBucket)
			.withDefaultLogger()
			.withMaxTries(6)
			.withTimeout(30);
		
		MailTaskFactory.processor.embilishTask(task);
		
		return task;
	}

	public static void init(XElement settings) {
		MailTaskFactory.useBucket = settings.getAttribute("Bucket", "Default");
		MailTaskFactory.debugBCC = settings.getAttribute("BccDebug");
	}
}
