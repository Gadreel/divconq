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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import divconq.lang.op.OperationCallback;
import divconq.work.TaskRun;

public class CountDownCallback {
	protected AtomicInteger count = null;
	protected OperationCallback callback = null;
	protected TaskRun task = null;
	protected ReentrantLock cdlock = new ReentrantLock();		// TODO try StampedLock ?
	
	public CountDownCallback(int count, OperationCallback callback) {
		this.count = new AtomicInteger(count);
		this.callback = callback;
	}
	
	public CountDownCallback(int count, TaskRun task) {
		this.count = new AtomicInteger(count);
		this.task = task;
	}
	
	public int countDown() {
		this.cdlock.lock();
		
		try {
			int res = this.count.decrementAndGet();
			
			if (res < 0)
				res = 0;
			
			if (res == 0) {
				if (this.callback != null)
					this.callback.complete();
				
				if (this.task != null)
					this.task.complete();
			}
			
			return res;
		}
		finally {
			this.cdlock.unlock();
		}
	}
	
	public int increment() {
		return this.count.incrementAndGet();
	}
	
	public int increment(int amt) {
		return this.count.addAndGet(amt);
	}

	public int value() {
		return this.count.intValue();
	}
}
