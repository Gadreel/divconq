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
import divconq.mod.ExtensionBase;
import divconq.work.TaskRun;

public class WebExtension extends ExtensionBase implements IService, IWebExtension {	
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
}
