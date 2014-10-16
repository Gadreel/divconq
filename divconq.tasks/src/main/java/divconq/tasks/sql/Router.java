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
package divconq.tasks.sql;

import divconq.bus.IService;
import divconq.bus.Message;
import divconq.bus.MessageUtil;
import divconq.tasks.services.Main;
import divconq.work.TaskRun;

public class Router implements IService {
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
		
		request.setResult(MessageUtil.errorTr(441, this.serviceName(), feature, op));
		request.complete();
	}

	@Override
	public String serviceName() {
		return  Main.instance.serviceName();
	}
}
