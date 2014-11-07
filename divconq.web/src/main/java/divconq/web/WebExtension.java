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
package divconq.web;

import java.util.concurrent.ConcurrentHashMap;

import divconq.bus.IService;
import divconq.bus.Message;
import divconq.bus.MessageUtil;
import divconq.hub.DomainInfo;
import divconq.hub.Hub;
import divconq.interchange.CommonPath;
import divconq.io.FileStoreEvent;
import divconq.lang.op.FuncCallback;
import divconq.lang.op.OperationResult;
import divconq.mod.Bundle;
import divconq.mod.ExtensionBase;
import divconq.session.Session;
import divconq.work.TaskRun;

public class WebExtension extends ExtensionBase implements IService, IWebExtension {	
	protected ConcurrentHashMap<String, IWebDomain> dsitemap = new ConcurrentHashMap<String, IWebDomain>();

	@Override
	public String getAppName() {
		return this.loader.getName();
	}
	
	@Override
	public IWebDomain getDomain(String id) {
		DomainInfo di = Hub.instance.getDomainInfo(id);
		
		if (di != null) {
			IWebDomain domain = this.dsitemap.get(di.getId());
			
			if (domain != null)
				return domain; 
			
			for (DomainInfo d : Hub.instance.getDomains()) {
				if (d.getId().equals(id)) {
					domain = new WebDomain();
					domain.init(this, id);
					this.dsitemap.put(id, domain);
					
					return domain;
				}
			}
		}
		
		return null;
	}
	
	@Override
	public OperationResult handle(Session sess, HttpContext hctx) {
		OperationResult res = new OperationResult();
		
		//String range = req.getValue("Content-Range");
		
		//if (StringUtil.isNotEmpty(range))
		//	System.out.println("h: " + range);
		
		Request req = hctx.getRequest();
		
		if (req.pathEquals("/"))
			req.setPath(new CommonPath("/" + this.getAppName() + "/index"));
		
		/* phase out
		String fn = req.pathEquals("/") ? "local" : req.getPath().getName(0);
		
		if (!"local".equals(fn) && !this.getLoader().getName().equals(fn))
			req.setPath(new CommonPath("/local" + req.getPath()));
			*/

		WebContext ctx = new WebContext(hctx, this);
		
		IWebDomain d = ctx.getDomain();
		
		if (d == null) 
			res.errorTr(150003);
		else {
			
			//if (StringUtil.isNotEmpty(range))
			//	System.out.println("h2: " + range);
			
			d.execute(ctx);
		}
		
		return res;
	}

	@Override
	public void start() {
		super.start();
				
		// detect changes to local file store
		if (Hub.instance.getLocalFileStore() != null) {
			Hub.instance.getLocalFileStore().register(this.loader.getName(), new FuncCallback<FileStoreEvent>() {
				@Override
				public void callback() {
					CommonPath p = this.getResult().getPath();
					
					//System.out.println(p);
					
					// must be inside a domain or we don't care
					if (p.getNameCount() < 2) {
						String domain = p.getName(0).substring(6, 27);
						
						IWebDomain d = WebExtension.this.getDomain(domain);
						
						// must be inside a domain or we don't care
						if (d != null) 
							d.siteNotify();
						
						return;
					}
					
					String domain = p.getName(0).substring(1);
					
					IWebDomain d = WebExtension.this.getDomain(domain);
					
					// must be inside a domain or we don't care
					if (d == null) 
						return;
			
					d.fileNotify(this.getResult());
				}
			});
		}
	}
	
	// will get called for each time the web server goes on line
	@Override
	public void online() {
		this.dsitemap.clear();
	}
	
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
		
		// TODO log?
		//System.out.println("web ex: " + feature + "-" + op);
		
		request.setResult(MessageUtil.errorTr(441, this.serviceName(), feature, op));
		request.complete();
	}
	
	@Override
	public Bundle getBundle() {
		return this.getLoader();
	}
}
