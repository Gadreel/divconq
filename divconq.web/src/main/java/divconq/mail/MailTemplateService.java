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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import divconq.bus.IService;
import divconq.bus.Message;
import divconq.filestore.CommonPath;
import divconq.io.CacheFile;
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
import divconq.web.WebSite;
import divconq.web.ui.adapter.DcuiOutputAdapter;
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
					
					// TODO figure out which site to use
					WebSite site = dom.getRootSite();
					
					IInnerContext ic = new EmailInnerContext(path, site, new FuncCallback<EmailInnerContext>() {
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
					
					WebContext ctx = new WebContext(ic);
					
					RecordStruct dparams = rec.getFieldAsRecord("Params");
					
					if (dparams != null) {
						Map<String, List<String>> pmap = ic.getRequest().getParameters();
						
						for (FieldStruct fld : dparams.getFields()) {
							List<String> lst = new ArrayList<>();
							lst.add(Struct.objectToString(fld.getValue()));
							pmap.put(fld.getName(), lst);
						}
					}
					
					CacheFile cfile = site.findSectionFile("email", path.toString(), ctx.isPreview());
					
					if (cfile != null) {
						IOutputAdapter output = new DcuiOutputAdapter();
						output.init(site, cfile, path, ctx.isPreview());
						
						if (OperationContext.get().hasErrors()) {
							request.errorTr(150001);
							request.complete();
							return;
						}
						
						output.execute(ctx);
					}
					
					request.complete();
				} 
				catch (Exception x) {
					request.error("Unable to build email: " + x);
					//x.printStackTrace();
					request.complete();
				}
				
				return;
			}
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
}
