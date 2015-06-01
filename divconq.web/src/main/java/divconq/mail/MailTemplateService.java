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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import divconq.bus.IService;
import divconq.bus.Message;
import divconq.filestore.CommonPath;
import divconq.lang.op.FuncCallback;
import divconq.lang.op.OperationContext;
import divconq.mod.ExtensionBase;
import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.web.IInnerContext;
import divconq.web.IOutputAdapter;
import divconq.web.WebContext;
import divconq.web.WebDomain;
import divconq.web.WebModule;
import divconq.web.dcui.ViewOutputAdapter;
import divconq.work.TaskRun;
import divconq.xml.XElement;

// Mail Template Service must be run within the Web module
public class MailTemplateService extends ExtensionBase implements IService {
	@Override
	public void start() {
		super.start();
		
		XElement settings = this.getLoader().getSettings();
		
		if (settings != null) {
			/*
			servicesettings.setField("SmtpPassword", settings.getAttribute("SmtpPassword"));
			*/
		}
		
		//System.out.println("mod: " + this.getLoader().getModule().getClass().getName());
	}
	
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
		
		if ("Message".equals(feature)) {
			if ("Build".equals(op)) {
				try {
					RecordStruct rec = msg.getFieldAsRecord("Body");
					
					CommonPath path = new CommonPath(rec.getFieldAsString("Path"));					
					WebModule mod = (WebModule) this.getLoader().getModule();
					WebDomain dom = mod.getWebSiteManager().getDomain(OperationContext.get().getDomain().getId());
					
					IInnerContext ic = new EmailInnerContext(path, dom, new FuncCallback<EmailInnerContext>() {
						@Override
						public void callback() {
							EmailInnerContext eic = this.getResult();
							
							RecordStruct resp = new RecordStruct()
								.withField("Subject", eic.getSubject())
								.withField("Body", eic.getHtmlResponse().getBody().toString())
								.withField("TextBody", eic.getTextResponse().getBody().toString());

							request.setResult(resp);
							request.complete();
						}
					});
					
					WebContext ctx = new WebContext(ic, mod.getWebSiteManager().getDefaultExtension());
					
					RecordStruct dparams = rec.getFieldAsRecord("Params");
					
					if (dparams != null) {
						Map<String, List<String>> pmap = ic.getRequest().getParameters();
						
						for (FieldStruct fld : dparams.getFields()) {
							List<String> lst = new ArrayList<>();
							lst.add(Struct.objectToString(fld.getValue()));
							pmap.put(fld.getName(), lst);
						}
					}
					
					IOutputAdapter output = new ViewOutputAdapter(ctx.getDomain(), path, 
							Paths.get("./public/dcw/" + dom.getAlias()  + "/email" + path), false);

					if (OperationContext.get().hasErrors() || (output == null)) {
						request.errorTr(150001);
						request.complete();
						return;
					}
					
					ctx.setAdapter(output);
					
					output.execute(ctx);
				} 
				catch (Exception x) {
					request.error("Unable to build email: " + x);
					//x.printStackTrace();
					request.complete();
				}
				
				
				/*
				Task task = MailTaskFactory.createSendEmailTask(msg.getFieldAsRecord("Body"));
				
				FuncResult<RecordStruct> ares = this.submit(task);
				
				if (!ares.hasErrors()) 
					request.setResult(ares.getResult());
				*/
				
				return;
			}
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
}
