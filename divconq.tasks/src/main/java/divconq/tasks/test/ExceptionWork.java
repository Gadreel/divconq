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

import divconq.work.ISynchronousWork;
import divconq.work.TaskRun;

public class ExceptionWork implements ISynchronousWork {

	@Override
	public void run(TaskRun run) {
		run.info("I have started, but will throw an uncaught exception before exiting");
		
		throw new NullPointerException("Fake exception");
	}

}
