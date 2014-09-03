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

public class SleepyWork implements ISynchronousWork {

	@Override
	public void run(TaskRun run) {
		run.info("I have started, but will just hang for a while...watch me timeout!");
		
		// tie up thread until we are kicked off
		try {
			Thread.sleep(120000);
		} 
		catch (InterruptedException x) {
		}
	}

}
