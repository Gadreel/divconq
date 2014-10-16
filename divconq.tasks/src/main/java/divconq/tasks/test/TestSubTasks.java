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

import java.util.concurrent.atomic.AtomicInteger;

import divconq.hub.Hub;
import divconq.struct.RecordStruct;
import divconq.work.IWork;
import divconq.work.Task;
import divconq.work.TaskRun;
import divconq.work.WrappedTaskObserver;

public class TestSubTasks implements IWork {
	// do 8 greets before ending
	protected AtomicInteger countdown = new AtomicInteger(8); 
	
	@Override
	public void run(final TaskRun trun) {
		int cnt = countdown.decrementAndGet();

		// if we have maxed out our tries
		if (cnt < 0) {
			trun.exit(0, "Test sub tasks Successfully Completed");
			trun.complete();
			return;
		}
		
		// don't run if we are idled (we might run 3 of 5 rounds with this)
		if (Hub.instance.isIdled()) {
			trun.exit(0, "Test sub tasks Successfully Completed");
			trun.complete();
			return;
		}
		
		RecordStruct params = trun.getTask().getParams();		
		String greet = params.getFieldAsString("Greet");
		
		Task subtask = ScriptFactory.createSlowGreetTask(greet);
		
		// wrapper so we collect the logs from sub tasks
		Hub.instance.getWorkPool().submit(subtask, new WrappedTaskObserver(trun) {
			public void completed(TaskRun or) {
				Hub.instance.getWorkPool().submit(trun);
			}
		});
	}
}
