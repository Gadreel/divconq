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
import divconq.lang.op.OperationResult;


public class CountDownCallback {
	protected AtomicInteger count = null;
	protected OperationCallback callback = null;
	protected ReentrantLock cdlock = new ReentrantLock();		// TODO try StampedLock
	
	public CountDownCallback(int count, OperationCallback callback) {
		this.count = new AtomicInteger(count);
		this.callback = callback;
	}
	
	public int countDown() {
		this.cdlock.lock();
		
		try {
			int res = this.count.decrementAndGet();
			
			if (res < 0)
				res = 0;
			
			if (res == 0)
				this.callback.complete();
			
			return res;
		}
		finally {
			this.cdlock.unlock();
		}
	}
	
	public int countDown(OperationResult res) {
		this.cdlock.lock();
		
		try {
			// we should use the SubContext approach to capture messages
			//this.callback.copyMessages(res);
			
			return this.countDown();
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

	/* TODO remove if possible
	public void setContext(OperationContext taskContext) {
		this.callback.setContext(taskContext);
	}
	*/

	public int value() {
		return this.count.intValue();
	}
}
