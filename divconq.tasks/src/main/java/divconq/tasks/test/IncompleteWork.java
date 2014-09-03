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

import divconq.work.IWork;
import divconq.work.TaskRun;

public class IncompleteWork implements IWork {

	@Override
	public void run(TaskRun run) {
		run.info("I have started, but have made no provision to complete...watch me timeout!");
	}
	
}
