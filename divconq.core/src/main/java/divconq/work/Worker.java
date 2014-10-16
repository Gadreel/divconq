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

import java.util.concurrent.locks.ReentrantLock;

import divconq.hub.Hub;
import divconq.log.Logger;

public class Worker implements Runnable {
	protected Thread thread = null;
	
	// when not null we are busy
	protected TaskRun run = null;
	
	protected boolean stopping = false;
	protected ReentrantLock lock = new ReentrantLock();
	protected int slot = 0;
	protected int jobs = 0;
	
	public void start(int slot) {
		this.slot = slot;
		this.thread = new Thread(this, "WorkPool_" + this.slot);		
		this.thread.setDaemon(true);
		
		// TODO consider this option
		//this.thread.setUncaughtExceptionHandler(eh);
		
		this.thread.start();
	}
	
	@Override
	public void run() {
		Logger.trace("Work pool thread started: " + this.slot);		
		Hub.instance.getWorkPool().incThreadsCreated();
		
		while (!this.stopping) {
			try {
				Logger.trace("Work pool thread taking: " + this.slot);
				
				TaskRun r = Hub.instance.getWorkPool().take();
				
				r.touch();		// make sure the task does not timeout immediately (especially when debugging)
				
				this.lock.lockInterruptibly();
				
				this.run = r;
				
				this.lock.unlock();
				
				if (!this.stopping) {
					Logger.trace("Work pool thread running: " + this.slot);
					
					r.run();
				}
				
				if (Thread.currentThread().isInterrupted())
					break;
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
			
			this.run = null;
		}
		
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
		// if not alive then toss this worker out
		if (!this.thread.isAlive()) 
			this.stopNice();
		
		if ((this.run != null) && (this.run.isHung())) {
			Logger.warn("Work pool thread hung: " + this.slot);
			
			// TODO remove
			System.out.println("Overdue: " + this.run.isOverdue());
			System.out.println("Overdue Time: " + this.run.getTask().getDeadlineMS());
			System.out.println("Inactive: " + this.run.isInactive());
			System.out.println("Inactive Time: " + this.run.getTask().getTimeoutMS());
			
			this.run.kill();
			
			this.dumpDebug();
			
			Hub.instance.getWorkPool().incThreadsHung();
			this.stop();			
		}
	}
	
	public void stop() {
		this.stopping = true;
		
		try {
			if (this.thread.isAlive())
				this.thread.interrupt();
		}
		catch (Exception x) {				
		}
		
		// replace my thread
		Hub.instance.getWorkPool().initSlot(this.slot);
	}
	
	// TODO define how this is different from stop so we know when to call it
	public void stopNice() {
		this.stopping = true;
		
		try {
			this.lock.lockInterruptibly();
			
			// only if not busy
			try {
				if ((this.run == null) && this.thread.isAlive()) 
					this.thread.interrupt();
					
				Hub.instance.getWorkPool().initSlot(this.slot);   
			}
			catch (Exception x) {				
			}
			finally {
				this.lock.unlock();
			}
		} 
		catch (InterruptedException x) {
		}
	}
}