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
package divconq.lang;

import java.util.concurrent.locks.ReentrantLock;

import divconq.hub.Hub;
import divconq.log.DebugLevel;
import divconq.scheduler.ISchedule;
import divconq.work.IWork;
import divconq.work.Task;
import divconq.work.TaskRun;

/**
 * Provides the same function support as @see OperationResult, however used with
 * callbacks when the method called is asynchronous.
 * 
 * @author Andy
 *
 */
abstract public class OperationCallback extends OperationResult {
	protected boolean called = false;
	protected ISchedule timeout = null;
	protected ReentrantLock oplock = new ReentrantLock();
	protected boolean repeatable = false;
	protected TaskRun run = null;
	
	public OperationCallback() {
		super();
	}
	
	public void setRepeatable(boolean v) {
		this.repeatable = v;
	}
	
	public OperationCallback(DebugLevel loglevel) {
		super(loglevel);
	}
	
	public OperationCallback(OperationContext ctx) {
		super(ctx);
	}
	
	public OperationCallback(TimeoutPlan plan) {
		super();
		
		Task timeouttask = new Task()
			.withWork(new IWork() {
			@Override
			public void run(TaskRun task) {
				OperationCallback.this.abandon();
				task.complete();
			}
		});
		
		this.timeout = Hub.instance.getScheduler().runIn(timeouttask, plan.getSeconds());
	}
	
	// do not timeout - but if at ServiceResult subclass we do 
	public OperationCallback(TaskRun run) {
		this.run = run;
	}
	
	public OperationCallback(TaskRun run, TimeoutPlan plan) {
		this(plan);
		this.run = run;
	}
	
	public void resetCalledFlag() {
		this.called = false;
	}
	
	// override if need to do something on timeout/giveup on operation
	// return true if timeout occurred, false if already completed
	public boolean abandon() {
		// courtesy only, no need to look if we do know called is true, real called check below
		if (this.called && !this.repeatable)
			return false;
		
		this.oplock.lock();
		
		try {
			if (this.called && !this.repeatable)
				return false;
			
			this.errorTr(218, this.opcontext);
			this.completed();
		}
		finally {
			this.oplock.unlock();
		}
		
		return true;
	}
	
	public void completed() {
		// courtesy only, no need to look if we do know called is true, real called check below
		if (this.called && !this.repeatable)
			return;
		
		this.oplock.lock();
		
		try {
			if (this.called && !this.repeatable)
				return;
			
			this.called = true;
			
			if (this.timeout != null)
				this.timeout.cancel();
			
			// if we are related to a task in the workpool, this is all we need to get the task working correctly again 
			// see TaskRun.resume - must keep this idea and that idea in sync
			
			if (this.run != null) 
				this.run.resume();			// do not use local context if have run, run's context may be fresher than ours do due to multiple callbacks after a bus call
			else if (this.opcontext != null)
				OperationContext.set(this.opcontext);
			
			this.callback();
			
			for (IOperationObserver ob : this.observers) {
				try {
					if (ob instanceof ICallbackObserver)
						((ICallbackObserver)ob).completed(this);			
				} 
				catch (Exception x) {
					this.error("Error notifying completing task: " + x);
				}
				
				// they might change context on us, return context
				if (this.run != null) 
					this.run.resume();			// do not use local context if have run, run's context may be fresher than ours do due to multiple callbacks after a bus call
				else if (this.opcontext != null)
					OperationContext.set(this.opcontext);
			}
		}
		finally {
			this.oplock.unlock();
		}
	}

	abstract public void callback();
}
