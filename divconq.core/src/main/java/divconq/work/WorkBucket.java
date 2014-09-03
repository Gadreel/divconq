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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import divconq.lang.OperationResult;
import divconq.log.Logger;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class WorkBucket {
	protected String name = "Default";
	protected LinkedBlockingQueue<TaskRun> backlogqueue = new LinkedBlockingQueue<TaskRun>();
	protected HashMap<String, TaskRun> inprogress = new HashMap<>();
	protected boolean automaticQueueLoader = false;
	
	protected ReentrantLock bucketlock = new ReentrantLock();
	
	protected Long maxsize = null;
	protected Long loadsize = null;
	protected boolean trace = false;
	
	public void init(OperationResult or, XElement config, int defaultloadsize) {
		if (config != null) {
			this.maxsize = StringUtil.parseInt(config.getAttribute("MaxSize"));
			this.loadsize = StringUtil.parseInt(config.getAttribute("LoadSize"));
			
			this.name = config.getAttribute("Name");
			
			this.automaticQueueLoader = "True".equals(config.getAttribute("AutomaticQueueLoader", "True"));
		}
		
		if (StringUtil.isEmpty(this.name)) 
			or.exit(1, "Missing work bucket name");
		
		if ((this.maxsize != null) && (this.maxsize < 1))
			this.maxsize = null;
		
		if ((this.maxsize == null) && (this.loadsize == null))
			this.loadsize = (long) defaultloadsize;
	}

	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public int getMaxSize() {
		return (this.maxsize == null) ? 0 : this.maxsize.intValue();
	}
	
	public void setMaxSize(int v) {
		this.maxsize = (long) v;
	}
	
	public boolean getAutomaticQueueLoader() {
		return this.automaticQueueLoader;
	}
	
	public void setAutomaticQueueLoader(boolean v) {
		this.automaticQueueLoader = v;
	}

	public int backlog() {
		return this.backlogqueue.size();		
	}

	public int inprogress() {
		return this.inprogress.size();		
	}
	
	public Collection<TaskRun> tasksInProgress() {
		return this.inprogress.values(); 
	}

	// if Full Size is in use we have a hard limit, otherwise a suggested limit of 150% thread count
	public int availCount() {
		if (this.maxsize != null)
			return this.maxsize.intValue() - this.inprogress.size();		
		
		return this.loadsize.intValue() - this.inprogress.size();
	}
	
	public boolean isFull() {
		if (this.maxsize == null) 
			return false;
		
		return (this.inprogress.size() >= this.maxsize.intValue());
	}

	public void setTrace(boolean v) {
		this.trace = v;
	}
	
	public RecordStruct toStatusReport() {
		RecordStruct rec = new RecordStruct();
		
		rec.setField("Name", this.name);
		rec.setField("InProgress", this.inprogress());
		rec.setField("Backlogged", this.backlog());
		rec.setField("MaxSize", this.getMaxSize());
		
		return rec;
	}

	// return true if pool can submit directly, otherwise return false and backlog it
	public boolean canSubmit(TaskRun run) {
		this.bucketlock.lock();

		boolean isLost = true;
		
		try {
			// if this task is continuing (via resubmit) then it goes right on the queue
			if (this.inprogress.containsKey(run.getTask().getId())) {
				run.traceTr(199, run);
				return true;
			}

			// if our queue can take unlimited work then task goes right on the queue
			// this is fast, but could result in a large number of partially run tasks 
			// if any tasks are all async
			if (this.maxsize == null) {
				this.inprogress.put(run.getTask().getId(), run);		// adds only if not already in set - first time a task is taken that task is considered in progress until the task completes
				run.traceTr(199, run);
				return true;
			}
			
			// otherwise see if we have available space in the bucket, backlog if not 
			int prog = this.inprogress.size();
			int avail = this.maxsize.intValue() - prog;
			
			if (avail > 0) {
				this.inprogress.put(run.getTask().getId(), run);		// adds only if not already in set - first time a task is taken that task is considered in progress until the task completes
				run.traceTr(199, run);
				
				isLost = false;
				
				prog++;
				avail--;
				
				if (this.trace) {
					System.out.println("------------------------------------------------");
					System.out.println("  Task Running: " + run.getTask().getId());
					System.out.println("------------------------------------------------");
					System.out.println("         Max: " + this.maxsize.intValue());
					System.out.println("     In Prog: " + prog);			// use var so it doesn't change between above and here
					System.out.println("       Avail: " + avail);
					System.out.println("  Backlogged: " + this.backlogqueue.size());
				}
				
				return true;
			}

			run.traceTr(211, run);
			this.backlogqueue.add(run);
			
			isLost = false;
			
			if (this.trace) {
				System.out.println("------------------------------------------------");
				System.out.println("  Task Backlogged: " + run.getTask().getId());
				System.out.println("------------------------------------------------");
				System.out.println("         Max: " + this.maxsize.intValue());
				System.out.println("     In Prog: " + prog);			// use var so it doesn't change between above and here
				System.out.println("       Avail: " + avail);
				System.out.println("  Backlogged: " + this.backlogqueue.size());
			}
		}
		catch(Exception x) {
			run.traceTr(212, run, x);		// TODO error?
			
			// if not in any queue then make sure we cleanup
			if (isLost)
				run.complete();
		}
		finally {
			this.bucketlock.unlock();
		}
		
		return false;
	}

	public void took(TaskRun run) {
		if (this.trace) {
			System.out.println("------------------------------------------------");
			System.out.println("  Task Taken: " + run.getTask().getId());
			System.out.println("------------------------------------------------");
			System.out.println("     In Prog: " + this.inprogress.size());
			System.out.println("  Backlogged: " + this.backlogqueue.size());
		}
	}

	// complete a run.  return another run to queue if we are back logged
	public TaskRun complete(TaskRun run) {
		// if using backlog then see if there is room for a new task
		this.bucketlock.lock();

		try {
			String completedid = run.getTask().getId();
			
			if (this.trace) {
				System.out.println(this.name + " Removing: " + completedid);
				
				for (TaskRun inrun : this.inprogress.values()) {
					String lid = inrun.getTask().getId();
					System.out.println("In list: " + lid + " - looks equal: " + lid.equals(completedid));
				}
			}
			
			if (this.trace) 
				System.out.println(this.name + " prog: " + this.inprogress.size());
			
			TaskRun rt = this.inprogress.get(completedid);
			
			if (this.trace) 
				System.out.println(this.name + " Got: " + rt);
			
			rt = this.inprogress.remove(completedid);
			
			if (this.trace) {
				System.out.println(this.name + " Removed: " + rt);
				
				for (TaskRun inrun : this.inprogress.values()) {
					String lid = inrun.getTask().getId();
					System.out.println("In list: " + lid + " - looks equal: " + lid.equals(completedid));
				}
			}
			
			// if no max size then no back log
			if (this.maxsize == null) 
				return null;
		
			int prog = this.inprogress.size();
			int avail = this.maxsize.intValue() - prog;		
			
			if (this.trace) {
				System.out.println("------------------------------------------------");
				System.out.println("  Task Completed: " + run.getTask().getId());
				System.out.println("------------------------------------------------");
				System.out.println("         Max: " + this.maxsize.intValue());
				System.out.println("     In Prog: " + prog);			// use var so it doesn't change between above and here
				System.out.println("       Avail: " + avail);
				System.out.println("  Backlogged: " + this.backlogqueue.size());
			}
			
			if ((avail > 0) && !this.backlogqueue.isEmpty()) {
				TaskRun r = this.backlogqueue.take();
				this.inprogress.put(r.getTask().getId(), r);
				return r;
			}
		}
		catch (InterruptedException x) {
			// shouldn't happen during normal run
		}
		finally {
			this.bucketlock.unlock();
		}
		
		return null;
	}
	
	public void checkIfHung() {
		List<TaskRun> killlist = new ArrayList<>();
		
		this.bucketlock.lock();
		
		try {
			for (TaskRun run : this.inprogress.values()) {
				if (run.isHung()) 
					killlist.add(run);
				else
					run.reviewClaim();
			}
		}
		finally {
			this.bucketlock.unlock();
		}
		
		for (TaskRun run : killlist) {
			Logger.warn("Bucket " + this.name + " found hung: " + run.getTask().getId());
			run.kill();
		}
	}
	
	@Override
	public String toString() {
		return this.name;
	}

	public TaskRun findTask(String taskid) {
		return this.inprogress.get(taskid);
	}
}
