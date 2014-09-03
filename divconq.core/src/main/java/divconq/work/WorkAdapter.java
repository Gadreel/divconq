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
package divconq.work;


public class WorkAdapter implements IWork {
	protected Runnable work = null;
	
	public WorkAdapter(Runnable run) {
		this.work = run;
	}
	
	@Override
	public void run(TaskRun task) {
		this.work.run();
		
		// this adapter doesn't work with async operations in runnable
		task.complete();
	}	
}
