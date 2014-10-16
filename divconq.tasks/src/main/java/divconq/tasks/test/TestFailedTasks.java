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

import java.util.concurrent.atomic.AtomicReference;

import divconq.hub.Hub;
import divconq.lang.OperationCallback;
import divconq.lang.OperationContext;
import divconq.scheduler.ISchedule;
import divconq.work.ISynchronousWork;
import divconq.work.IWork;
import divconq.work.Task;
import divconq.work.TaskCountDownCallback;
import divconq.work.TaskRun;
import divconq.work.WorkBucket;
import divconq.work.WorkPool;

public class TestFailedTasks implements IWork {

	@Override
	public void run(final TaskRun testrunner) {
		testrunner.info("TestFailedTasks main attempting to start in Bucket: " + testrunner.getTask().getBucket());
		
		WorkPool wp = Hub.instance.getWorkPool();
		
		final WorkBucket bucket = new WorkBucket();
		bucket.setName("FailedTasks");
		bucket.setMaxSize(1);		// run only 1 at a time
		//bucket.setTrace(true);
		
		wp.addBucket(bucket);
		
		final AtomicReference<ISchedule> reportschedule = new AtomicReference<>();
		
		// when all the tasks are done, here is what to do
		final TaskCountDownCallback cntdwn = new TaskCountDownCallback();
		
		cntdwn.setCallback(new OperationCallback() {			
			@Override
			public void callback() {
				// stop reporting
				testrunner.info("TestFailedTasks main attempting to cancel reporter in Bucket: " + testrunner.getTask().getBucket());
				reportschedule.get().cancel();

				// cleanup the test bucket we added
				testrunner.info("TestFailedTasks main attempting to cleanup in Bucket: " + testrunner.getTask().getBucket());
				Hub.instance.getWorkPool().removeBucket(bucket.getName());
				
				// analyze the jobs
				TaskRun except = cntdwn.getRun("EXCEPTION");
				
				// check in case we commented it out
				if (except != null) {
					if (!except.hasCode(155))
						testrunner.error("EXCEPTION did not end as expected");
					else
						testrunner.info("EXCEPTION passed");
					
					// check other conditions too
				}
				else
					testrunner.info("EXCEPTION skipped");
				
				TaskRun sleepy = cntdwn.getRun("SLEEPY");
				
				// check in case we commented it out
				if (sleepy != null) {
					if (!sleepy.hasCode(222))
						testrunner.error("SLEEPY did not end as expected");
					else
						testrunner.info("SLEEPY passed");
					
					// check other conditions too
				}
				else
					testrunner.info("SLEEPY skipped");
				
				TaskRun tooslow = cntdwn.getRun("TOOSLOW");
				
				// check in case we commented it out
				if (tooslow != null) {
					if (!tooslow.hasCode(222))
						testrunner.error("TOOSLOW did not end as expected");
					else
						testrunner.info("TOOSLOW passed");
					
					// check other conditions too
				}
				else
					testrunner.info("TOOSLOW skipped");
				
				TaskRun incomp = cntdwn.getRun("INCOMPLETE");
				
				// check in case we commented it out
				if (incomp != null) {
					if (!incomp.hasCode(223))
						testrunner.error("INCOMPLETE did not end as expected");
					else
						testrunner.info("INCOMPLETE passed");
					
					// check other conditions too
				}
				else
					testrunner.info("INCOMPLETE skipped");
				
				// the task is done
				if (testrunner.hasErrors())
					testrunner.info("TestFailedTasks completed, but had issues: " + testrunner.getMessage());
				else
					testrunner.info("TestFailedTasks completed without issues!");
				
				testrunner.complete();
			}
		});
		
		// create a task that shows the TEST bucket status every second 
		Task reporttask = new Task()
			.withWork(new ISynchronousWork() {
				@Override
				public void run(TaskRun run) {
					// by logging on testrunner we are keeping it active - without this it could timeout
					testrunner.info("REPORT - In Progress: " + bucket.inprogress() + " - Backlog: " + bucket.backlog());
				}
			});
		
		reportschedule.set(Hub.instance.getScheduler().runEvery(reporttask, 1));

		// launch the Sleepy Work test - shows inactive timeout in action
		{
			Task task = new Task()
				.withId("SLEEPY")
				.withTitle("Sleepy Work Test")
				.withDeadline(1)		// must complete in 1 minute 
				.withContext(
						OperationContext.get()
							.toBuilder()
							.withNewOpId()							// same user, logging, etc as test task, just use new op ids (it is fine to have same op id, this is just an example of how to use another op id)
							.toOperationContext()
				)
				.withBucket(bucket.getName())
				.withWork(SleepyWork.class);
	
			cntdwn.add(task);
		}

		// launch the Too Slow Work test - shows finish timeout 
		{
			Task task = new Task()
				.withId("TOOSLOW")
				.withTitle("Too Slow Work Test")
				.withDeadline(1)		// must complete in 1 minute 
				.withContext(
						OperationContext.get()
							.toBuilder()
							.withNewOpId()							// same user, logging, etc as test task, just use new op ids (it is fine to have same op id, this is just an example of how to use another op id)
							.toOperationContext()
				)
				.withBucket(bucket.getName())
				.withWork(VerySlowWork.class);
	
			cntdwn.add(task);
		}

		// launch the Exception Work test
		{
			Task task = new Task()
				.withId("EXCEPTION")
				.withTitle("Exception Work Test")
				.withDeadline(1)		// give 1 minute to complete
				.withContext(
						OperationContext.get()
							.toBuilder()
							.withNewOpId()							// same user, logging, etc as test task, just use new op ids (it is fine to have same op id, this is just an example of how to use another op id)
							.toOperationContext()
				)
				.withBucket(bucket.getName())
				.withWork(ExceptionWork.class);

			cntdwn.add(task);
		}

		// launch the Incomplete Work test
		{
			Task task = new Task()
				.withId("INCOMPLETE")
				.withTitle("Incomplete Work Test")
				.withTimeout(1)		// give 1 minute to complete
				.withDeadline(10)		// we will timeout, not be overdue
				.withContext(
						OperationContext.get()
							.toBuilder()
							.withNewOpId()							// same user, logging, etc as test task, just use new op ids (it is fine to have same op id, this is just an example of how to use another op id)
							.toOperationContext()
				)
				.withBucket(bucket.getName())
				.withWork(IncompleteWork.class);
	
			cntdwn.add(task);
		}
		
		cntdwn.submit();
	}	
}
