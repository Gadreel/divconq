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

import divconq.bus.IService;
import divconq.bus.Message;
import divconq.bus.MessageUtil;
import divconq.lang.op.OperationResult;
import divconq.mod.Bundle;
import divconq.mod.ExtensionBase;
import divconq.session.Session;
import divconq.work.TaskRun;

public class WebExtension extends ExtensionBase implements IService, IWebExtension {	
	@Override
	public String getAppName() {
		return this.loader.getName();
	}
	
	@Override
	public OperationResult handle(Session sess, HttpContext hctx) {
		OperationResult res = new OperationResult();

		WebContext ctx = new WebContext(hctx, this);
		
		IWebDomain d = ctx.getDomain();
		
		if (d == null) 
			res.errorTr(150003);
		else 
			d.execute(ctx);
		
		return res;
	}

	@Override
	public void start() {
		super.start();
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
