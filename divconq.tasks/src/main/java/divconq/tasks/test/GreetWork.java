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
package divconq.tasks.test;

import divconq.struct.RecordStruct;
import divconq.util.StringUtil;
import divconq.work.ISynchronousWork;
import divconq.work.TaskRun;

public class GreetWork implements ISynchronousWork {

	@Override
	public void run(TaskRun run) {
		/*
		try {
			Thread.sleep(2000);
		} 
		catch (InterruptedException x) {
		}
		*/
		
		RecordStruct params = run.getTask().getParams();
		
		if (params == null) {
			run.error("Unable to Greet, missing params structure.");
			return;
		}
		
		String name = params.getFieldAsString("Greet");
		
		if (StringUtil.isEmpty(name)) {
			run.error("Unable to Greet, missing Greet param.");
			return;
		}
		
		run.info("Greetings " + name);
	}

}
