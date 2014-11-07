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

import divconq.bus.IService;
import divconq.bus.Message;
import divconq.hub.Hub;
import divconq.lang.op.FuncResult;
import divconq.mod.ExtensionBase;
import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;
import divconq.work.Task;
import divconq.work.TaskRun;
import divconq.xml.XElement;

public class MailService extends ExtensionBase implements IService, IMailProcessor {
	protected String procmode = "Queue";
	
	@Override
	public FuncResult<RecordStruct> submit(Task mail) {
		FuncResult<RecordStruct> or = new FuncResult<>();
		
		// run the message off the queue
		if ("Queue".equals(this.procmode)) {
			FuncResult<String> ares = Hub.instance.getWorkQueue().submit(mail);
			
			if (!or.hasErrors())
				or.setResult(new RecordStruct(new FieldStruct("WorkId", ares.getResult()), new FieldStruct("TaskId", mail.getId())));
		}
		// run the message in local pool, don't wait
		else { 
			Hub.instance.getWorkPool().submit(mail);
			
			if (!or.hasErrors())
				or.setResult(new RecordStruct(new FieldStruct("TaskId", mail.getId())));
		}
		
		return or;
	}

	@Override
	public void embilishTask(Task email) {
		email.withWork(SendWork.class);
	}
	
	@Override
	public void start() {
		super.start();

		RecordStruct servicesettings = new RecordStruct();
		
		XElement settings = this.getLoader().getSettings();
		
		if (settings != null) {
			servicesettings.setField("SmtpHost", settings.getAttribute("SmtpHost"));
			servicesettings.setField("SmtpPort", settings.getAttribute("SmtpPort"));
			servicesettings.setField("SmtpUsername", settings.getAttribute("SmtpUsername"));
			servicesettings.setField("SmtpAuth", settings.getAttribute("SmtpAuth"));
			servicesettings.setField("SmtpDebug", settings.getAttribute("SmtpDebug"));
			servicesettings.setField("SmtpPassword", settings.getAttribute("SmtpPassword"));
			
			this.procmode = settings.getAttribute("ProcessMode", this.procmode);
			
			MailTaskFactory.init(settings);
		}
		
		MailTaskFactory.setSettings(servicesettings);
		MailTaskFactory.setProcessor(this);
	}
	
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
		
		if ("Message".equals(feature)) {
			if ("Send".equals(op)) {
				Task task = MailTaskFactory.createSendEmailTask(msg.getFieldAsRecord("Body"));
				
				FuncResult<RecordStruct> ares = this.submit(task);
				
				if (!ares.hasErrors()) 
					request.setResult(ares.getResult());
				
				request.complete();
				return;
			}
		}
		else if ("Status".equals(feature)) {
			if ("Check".equals(op)) {
				// TODO
				request.setResult(
						new RecordStruct(
						)
				);
				
				request.complete();
				return;
			}
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
}
