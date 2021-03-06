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

import divconq.bus.Message;
import divconq.bus.ServiceResult;
import divconq.filestore.CommonPath;
import divconq.hub.Hub;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.work.IWork;
import divconq.work.Task;
import divconq.work.TaskRun;
import divconq.xml.XElement;

public class MailTaskFactory {
	// abstract out the service so we can use SQL, noSQL or XML based service - or non-smtp services
	//static protected String useBucket = null;
	//static protected String debugBCC = null;
	static protected IMailProcessor processor = null; 
	//static public RecordStruct settings = null;
	
	static public XElement getSettings() {
		XElement dsettings = OperationContext.get().getDomain().getSettings();
		
		if (dsettings != null) {
			XElement msettings = dsettings.find("Email");
			
			if (msettings != null)
				return msettings;
		}
		
		return processor.getSettings();		// default
	}
		
	// we need a processor of some sort or we are dead in water
	public static void init(IMailProcessor processor) {
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
		
		String tid = Task.nextTaskId("EMAIL");
		
		XElement settings = MailTaskFactory.getSettings();
		
		// unfortunately this uses the current domain - which may not be the domain that is sending
		String useBucket = (settings != null) ? settings.getAttribute("Bucket", "Default") : "Default";
		
		Task task = new Task()
			.withId(tid)
			.withTitle("Send Email To " + params.getFieldAsString("To"))
			.withParams(params)
			.withBucket(useBucket)
			.withDefaultLogger()
			.withSubContext()
			.withMaxTries(6)
			.withTimeout(30);
		
		MailTaskFactory.processor.embilishTask(task);
		
		return task;
	}
	
	static public Task createBuildSendEmailTask(String from, String to, CommonPath template, RecordStruct params) {
		RecordStruct tparams = new RecordStruct(
			new FieldStruct("Path", template),
			new FieldStruct("Params", params)
		);
		
		RecordStruct sparams = new RecordStruct(
				new FieldStruct("From", from),
				new FieldStruct("To", to)
			);
			
		return MailTaskFactory.createBuildSendEmailTask(tparams, sparams);
	}
	
	static public Task createBuildSendEmailTask(RecordStruct tparams, RecordStruct sparams) {
		if (MailTaskFactory.processor == null)
			return null;
		
		String tid = Task.nextTaskId("EMAIL");
		
		XElement settings = MailTaskFactory.getSettings();
		
		// unfortunately this uses the current domain - which may not be the domain that is sending
		String useBucket = (settings != null) ? settings.getAttribute("Bucket", "Default") : "Default";
		
		Task ttask = new Task()
			.withId(tid)
			.withTitle("Build and Send Email To " + sparams.getFieldAsString("To"))
			.withBucket(useBucket)
			.withDefaultLogger()
			.withSubContext()
			.withMaxTries(2)
			.withTimeout(3)
			.withWork(new IWork() {
				@Override
				public void run(TaskRun trun) {
					Message msg = new Message("dcmEmailBuilder", "Message", "Build", tparams);
					
					Hub.instance.getBus().sendMessage(msg, new ServiceResult() {						
						@Override
						public void callback() {
							RecordStruct params = this.getBodyAsRec();
							
							params.copyFields(sparams);
							
							Task stask = MailTaskFactory.createSendEmailTask(params);
							
							MailTaskFactory.sendEmail(stask);
							
							trun.complete();
						}
					});
				}
			});
		
		return ttask;
	}
}
