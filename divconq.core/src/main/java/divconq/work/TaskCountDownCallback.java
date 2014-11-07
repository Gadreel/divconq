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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import divconq.hub.Hub;
import divconq.lang.CountDownCallback;
import divconq.lang.op.IOperationObserver;
import divconq.lang.op.OperationCallback;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationObserver;

public class TaskCountDownCallback extends CountDownCallback {
	protected int delayComplete = 2;
	protected List<Task> tasks = new ArrayList<>();
	protected HashMap<String, TaskRun> runs = new HashMap<>();
	
	protected IOperationObserver taskCallback = new OperationObserver() {
		@Override
		public void completed(OperationContext or) {
			TaskCountDownCallback.this.countDown();
		}				
	};
	
	public void setDelayComplete(int v) {
		this.delayComplete = v;
	}
	
	public IOperationObserver getTaskCallback() {
		return this.taskCallback;
	}
	
	public TaskCountDownCallback() {
		super(0, null);
	}
	
	public TaskCountDownCallback(int tasks, OperationCallback callback) {
		super(tasks, callback);
	}
	
	public void setCallback(OperationCallback v) {
		this.callback = v;
	}
	
	public List<Task> getTasks() {
		return this.tasks;
	}
	
	public Map<String, TaskRun> getRuns() {
		return this.runs;
	}
	
	public TaskRun getRun(String id) {
		return this.runs.get(id);
	}

	@Override
	public int countDown() {
		this.cdlock.lock();
		
		try {
			int res = this.count.decrementAndGet();
			
			if (res < 0)
				res = 0;
			
			// make this a delayed action
			if (res == 0) {
				// be sure we are running as the task that requested this callback
				OperationContext.set(this.callback.getContext());
				
				Task reporttask = new Task()
					.withSubContext()
					.withWork(new ISynchronousWork() {
						@Override
						public void run(TaskRun run) {
							TaskCountDownCallback.this.callback.complete();
						}
					});
				
				Hub.instance.getScheduler().runIn(reporttask, this.delayComplete);		
			}
			
			return res;
		}
		finally {
			this.cdlock.unlock();
		}
	}

	public void add(Task task) {
		this.increment();
		
		this.tasks.add(task);
	}
	
	public void submit() {
		for (Task t : this.tasks) {
			TaskRun run = Hub.instance.getWorkPool().submit(t, this.getTaskCallback());
			
			this.runs.put(t.getId(), run);
		}
	}
}
