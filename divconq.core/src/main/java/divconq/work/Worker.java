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

import java.util.concurrent.locks.StampedLock;

import divconq.hub.Hub;
import divconq.lang.op.OperationContext;
import divconq.log.Logger;

public class Worker implements Runnable {
	protected Thread thread = null;
	protected boolean stopping = false;
	protected int slot = 0;
	
	protected TaskRun run = null;			// we are busy when not null 
	protected int taskThrottle = -1;
	protected boolean resumeTask = false;
	protected StampedLock lock = new StampedLock();
	
	public void start(int slot) {
		this.slot = slot;
		this.thread = new Thread(this, "WorkPool_" + this.slot);		
		this.thread.setDaemon(true);
		
		// TODO consider this option
		//this.thread.setUncaughtExceptionHandler(eh);
		
		this.thread.start();
	}
	
	// if tr is the current running task then allow it to resume without going all the way to the back of the line
	// note that if we get here then we are the task (only the task is allowed to resume itself) so no need to 
	// lock the member variables
	//
	// return false if we cannot resume
	public boolean resume(TaskRun tr) {
		long stamp = this.lock.readLock();
		
		try {
			if ((this.run != tr) || (this.taskThrottle == 0))
				return false;
			
			long wstamp = this.lock.tryConvertToWriteLock(stamp);
			
			if (wstamp == 0L) {
				this.lock.unlockRead(stamp);
				wstamp = this.lock.writeLock();  
			}
			
			stamp = wstamp;

			// check again for good measure, in case we didn't get write convert
			if ((this.run != tr) || (this.taskThrottle == 0))
				return false;
			
			this.resumeTask = true;		
			return true;
		}
		finally {
			this.lock.unlock(stamp);
		}
	}
	
	@Override
	public void run() {
		OperationContext.useHubContext();
		
		Logger.trace("Work pool thread started: " + this.slot);		
		
		Hub.instance.getWorkPool().incThreadsCreated();
		
		while (!this.stopping) {
			try {
				Logger.trace("Work pool thread taking: " + this.slot);
				
				TaskRun r = Hub.instance.getWorkPool().take();
				
				r.touch();		// make sure the task does not timeout immediately (especially when debugging)
				
				int tthrottle = r.getTask().getThrottle();		// always run at least once even if we get 0 
				
				long wstamp = this.lock.writeLock();
				
				try {
					this.run = r;
					this.taskThrottle = tthrottle;
					this.resumeTask = false;		// run once
				}
				finally {
					this.lock.unlock(wstamp);
				}
				
				// it is important to record the slot, if we do not then during resume
				// we may find an older worker still pointing to this run and think it can resume us
				r.slot = this.slot;
				
				// taskThrottle == -1 means unlimited local resumes
				// taskThrottle == 0 or 1 means don't allow local resumes
				// taskThrottle > 1 means allow N - 1 number of resumes before putting back into the work pool
				
				while (!this.stopping) {
					long stamp1 = this.lock.writeLock();
					
					try {
						// -1 has special meaning, do not go under 0
						if (this.taskThrottle > 0)
							this.taskThrottle--;
						
						this.resumeTask = false;
					}
					finally {
						this.lock.unlock(stamp1);
					}
					
					//Logger.trace("Work pool thread running: " + this.slot);
						
					r.run();
					
					OperationContext.useHubContext();
					
					long stamp2 = this.lock.readLock();
					
					try {
						// if we have no more resumes then break
						boolean needbreak = (Thread.currentThread().isInterrupted() || (this.taskThrottle == 0) || !this.resumeTask);
						
						if (needbreak) {
							long wstamp2 = this.lock.tryConvertToWriteLock(stamp2);
							
							if (wstamp2 == 0L) {
								this.lock.unlockRead(stamp2);
								wstamp2 = this.lock.writeLock();  
							}
							
							stamp2 = wstamp2;
							
							needbreak = (Thread.currentThread().isInterrupted() || (this.taskThrottle == 0) || !this.resumeTask);
							
							if (needbreak) {
								this.taskThrottle = 0;
								this.resumeTask = false;
								this.run = null;
								
								break;
							}
						}
					}
					finally {
						this.lock.unlock(stamp2);
					}
				}   
			}
			catch (InterruptedException x) {
				break;
			}
			catch (Exception x) {
				Logger.warn("Work pool caught error: " + x);
			}
			catch (ThreadDeath x) {
				Logger.warn("Work pool thread died: " + x);
				throw x;
			}
			
			long wstamp = this.lock.writeLock();
			
			try {
				this.taskThrottle = 0;
				this.resumeTask = false;
				this.run = null;
			}
			finally {
				this.lock.unlock(wstamp);
			}
		}
		
		// for interrupted flow
		this.run = null;
		
		Logger.trace("Work pool thread stopped: " + this.slot);
	}

	protected void dumpDebug() {
		try {
			Logger.warn("     Thread Id: " + this.thread.getId());
			Logger.warn("   Thread Name: " + this.thread.getName());
			Logger.warn("  Thread State: " + this.thread.getState());
			Logger.warn("  Thread Stack:");
			
			StackTraceElement[] stack = this.thread.getStackTrace();
			
			for (StackTraceElement e : stack) 
				Logger.warn("              - " + e.toString());
		}
		catch (Exception x) {			
		}
	}
	
	public void checkIfHung() {
		Thread th = this.thread;
		
		// if not alive then toss this worker out
		// replace/remove my thread
		if ((th == null) || !th.isAlive())
			Hub.instance.getWorkPool().initSlot(this.slot);
		
		TaskRun tr = this.run;
		
		if ((tr != null) && (tr.isHung())) {
			Logger.warn("Work pool thread hung: " + this.slot);
			
			// TODO remove
			System.out.println("Overdue: " + tr.isOverdue());
			System.out.println("Overdue Time: " + tr.getTask().getDeadlineMS());
			System.out.println("Inactive: " + tr.isInactive());
			System.out.println("Inactive Time: " + tr.getTask().getTimeoutMS());
			
			tr.kill();
			
			this.dumpDebug();
			
			Hub.instance.getWorkPool().incThreadsHung();
			this.stop();			
		}
	}
	
	public void stop() {
		this.stopping = true;
		
		Thread th = this.thread;
		
		if ((th == null) || !th.isAlive())
			return;
		
		try {
			th.interrupt();
		}
		catch (Exception x) {				
		}
		
		// replace/remove my thread
		Hub.instance.getWorkPool().initSlot(this.slot);
	}
	
	// 
	public void stopNice() {
		this.stopping = true;
		
		Thread th = this.thread;
		
		if ((th == null) || !th.isAlive())
			return;
		
		// only stop if not busy, if run is null then no work is being done
		if (this.run != null)
			return;
		
		try {
			th.interrupt();
		}
		catch (Exception x) {				
		}
		
		// replace/remove my thread
		Hub.instance.getWorkPool().initSlot(this.slot);   
	}
}