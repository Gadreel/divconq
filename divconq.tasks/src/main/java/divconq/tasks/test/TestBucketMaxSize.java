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
import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;
import divconq.work.ISynchronousWork;
import divconq.work.IWork;
import divconq.work.Task;
import divconq.work.TaskCountDownCallback;
import divconq.work.TaskObserver;
import divconq.work.TaskRun;
import divconq.work.WorkBucket;
import divconq.work.WorkPool;

public class TestBucketMaxSize implements IWork {

	@Override
	public void run(final TaskRun testrunner) {
		testrunner.info("TestBucketMaxSize main attempting to start in Bucket: " + testrunner.getTask().getBucket());
		
		RecordStruct params = testrunner.getTask().getParams();
		
		int launch = 21;
		
		if (params != null) {
			launch = (int) params.getFieldAsInteger("LaunchCount", launch);
		}
		
		WorkPool wp = Hub.instance.getWorkPool();
		
		final WorkBucket bucket = new WorkBucket();
		bucket.setName("MaxSizeTest");
		bucket.setMaxSize(4);		// run only 4 at a time
		//bucket.setTrace(true);
		
		wp.addBucket(bucket);
		
		final AtomicReference<ISchedule> reportschedule = new AtomicReference<>();
		
		// when all the tasks are done, here is what to do
		final TaskCountDownCallback cntdwn = new TaskCountDownCallback(launch, new OperationCallback() {			
			@Override
			public void callback() {
				// note this task id should be the same as main
				testrunner.info("TestBucketMaxSize main attempting to cancel reporter in Bucket: " + testrunner.getTask().getBucket());
				
				// stop reporting
				reportschedule.get().cancel();
				
				// note this task id should be the same as main
				testrunner.info("TestBucketMaxSize main attempting to cleanup in Bucket: " + testrunner.getTask().getBucket());
				
				// the bucket is for testing only, remove it
				Hub.instance.getWorkPool().removeBucket(bucket.getName());
				
				// note this task id should be the same as main
				testrunner.info("TestBucketMaxSize completed!");
				
				// mark this TEST task as complete
				testrunner.complete();
			}
		});
		
		// launch the (sub) tasks
		for (int i = 0; i < launch; i++) {
			Task task = new Task()
				.withTitle("Greeting Carl " + i)
				.withParams(new RecordStruct(new FieldStruct("Greet", "Carl")))
				.withTimeout(5)		// allow for the fact it is "slow" and "dumb" (won't talk to log to stay alive) and give 5 seconds to complete
				.withContext(
						OperationContext.get()
							.toBuilder()
							.withNewOpId()							// same user, logging, etc as test task, just use new op ids (it is fine to have same op id, this is just an example of how to use another op id)
							.toOperationContext()
				)
				.withBucket(bucket.getName())
				.withWork(SlowGreetWork.class);
	
			Hub.instance.getWorkPool().submit(task, new TaskObserver() {				
				@Override
				public void completed(TaskRun or) {
					int left = cntdwn.countDown();
					
					// the message, when printed, should be in the context of the greeter task, not in our task
					if (left == 0)
						or.info("I was the last greeter!");					
				}
			});
		}
		
		// create a task that shows the TEST bucket status every second 
		Task reporttask = new Task()
			.withContext(
					OperationContext.get()
						.toBuilder()
						.withNewOpId()							// same user, logging, etc as test task, just use new task ids
						.toOperationContext()
			)
			.withWork(new ISynchronousWork() {
				@Override
				public void run(TaskRun run) {
					run.info("TestBucketMaxSize reporter attempting to run in Bucket: " + run.getTask().getBucket());
					
					// by logging on testrunner we are keeping it active - without this it would likely timeout
					testrunner.info("REPORT - In Progress: " + bucket.inprogress() + " - Backlog: " + bucket.backlog());
				}
			});
		
		reportschedule.set(Hub.instance.getScheduler().runEvery(reporttask, 1));
	}	
}
