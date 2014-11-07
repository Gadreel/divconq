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
package divconq.lang.op;

import java.util.concurrent.locks.ReentrantLock;

import divconq.hub.Hub;
import divconq.lang.TimeoutPlan;
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
	
	public OperationCallback() {
		super();
	}
	
	public OperationCallback(TimeoutPlan plan) {
		this();
		
		Task timeouttask = new Task()
			.withSubContext()
			.withWork(new IWork() {
			@Override
			public void run(TaskRun task) {
				OperationCallback.this.abandon();
				task.complete();
			}
		});
		
		this.timeout = Hub.instance.getScheduler().runIn(timeouttask, plan.getSeconds());
	}
	
	public OperationCallback(OperationContext ctx) {
		super(ctx);
	}
	
	public OperationCallback(OperationContext ctx, TimeoutPlan plan) {
		this(ctx);
		
		Task timeouttask = new Task()
			.withContext(ctx)
			.withWork(new IWork() {
			@Override
			public void run(TaskRun task) {
				OperationCallback.this.abandon();
				task.complete();
			}
		});
		
		this.timeout = Hub.instance.getScheduler().runIn(timeouttask, plan.getSeconds());
	}
	
	public void resetCalledFlag() {
		this.called = false;
	}
	
	// override if need to do something on timeout/giveup on operation
	// return true if timeout occurred, false if already completed
	public boolean abandon() {
		// courtesy only, no need to lock if we do know called is true, real called check below
		if (this.called)
			return false;
		
		this.oplock.lock();
		
		try {
			if (this.called)
				return false;
			
			this.errorTr(218, this.opcontext.freezeToSafeRecord());
		}
		finally {
			this.oplock.unlock();
		}
		
		this.complete();
		
		return true;
	}
	
	public void complete() {
		// courtesy only, no need to look if we do know called is true, real called check below
		if (this.called)
			return;
		
		this.oplock.lock();
		
		try {
			// check for race condition
			if (this.called)
				return;
			
			this.called = true;
		}
		finally {
			this.oplock.unlock();
		}
		
		// be sure we restore the context
		OperationContext ctx = OperationContext.get();
		
		try {
			OperationContext.set(this.opcontext);
			
			if (this.timeout != null)
				this.timeout.cancel();
			
			OperationContext.set(this.opcontext);
			
			this.markEnd();
			
			this.callback();
			
			// TODO review, this may not be useful
			//this.opcontext.fireEvent(OperationEvents.COMPLETED, null);
		}
		finally {
			OperationContext.set(ctx);
		}
	}

	abstract public void callback();
}
