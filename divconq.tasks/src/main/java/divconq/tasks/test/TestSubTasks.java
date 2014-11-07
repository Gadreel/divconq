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
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationObserver;
import divconq.struct.RecordStruct;
import divconq.work.ISmartWork;
import divconq.work.Task;
import divconq.work.TaskRun;

public class TestSubTasks implements ISmartWork {
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
			trun.exit(0, "Test sub tasks Successfully Exited but did not Complete");
			trun.complete();
			return;
		}
		
		RecordStruct params = trun.getTask().getParams();		
		String greet = params.getFieldAsString("Greet");
		
		Task subtask = ScriptFactory.createSlowGreetTask(greet);
		
		/*
		Task subtask = new Task()
			.withTitle("Greetings for: " + greet)
			.withDefaultLogger()
			.withSubContext()
			.withParams(new RecordStruct(
					new FieldStruct("Greet", greet)
				)
			)
			.withWork(ExceptionWork.class);
		*/
		
		// wrapper so we collect the logs from sub tasks
		Hub.instance.getWorkPool().submit(subtask, new OperationObserver() {
			public void completed(OperationContext or) {
				Hub.instance.getWorkPool().submit(trun);
			}
		});
	}

	@Override
	public void cancel(TaskRun run) {
	}

	@Override
	public void completed(TaskRun trun) {
		System.out.println("TestSubTasks Completed!");
		System.out.println();
		System.out.println("trun has errors: " + trun.hasErrors());
		System.out.println("trun finish code: " + trun.getCode());
		System.out.println("trun # msgs: " + trun.getMessages().getSize());

		System.out.println();
		System.out.println("Log for total:");
		OperationsWork.dumpLog(trun);
	}
}
