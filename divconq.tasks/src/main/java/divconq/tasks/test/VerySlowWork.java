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

// i need 10 minutes to complete
public class VerySlowWork implements ISynchronousWork {
	@Override
	public void run(TaskRun run) {
		run.info("I have started, and am doing work, but don't finish on time...watch me 'finish' timeout!");
		
		// tie up thread until we are kicked off
		try {
			for (int i = 0; i < 600; i++) {
				run.info("doing some work");
				
				Thread.sleep(1000);
			}
		} 
		catch (InterruptedException x) {
		}
	}
}
