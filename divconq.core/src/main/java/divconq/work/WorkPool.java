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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import divconq.hub.Hub;
import divconq.hub.ISystemWork;
import divconq.hub.SysReporter;
import divconq.lang.OperationResult;
import divconq.log.Logger;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.xml.XAttribute;
import divconq.xml.XElement;

public class WorkPool implements ExecutorService {
	protected LinkedBlockingQueue<TaskRun> queue = new LinkedBlockingQueue<>();
	protected ConcurrentHashMap<Integer, Worker> slots = new ConcurrentHashMap<>();
	protected ConcurrentHashMap<String, WorkBucket> buckets = new ConcurrentHashMap<>();
	
	protected int size = 16;
	
	// when set, the pool will work only on N number of tasks until one of those tasks completes
	// where upon a new task from the general queue "queue" can be accepted
	// in other words, when fullsize == inprogress.size() we are full and do no additional processing
	// when this is null, we pull tasks off the general queue ANY time a thread has spare cycles
	
	protected AtomicLong totalThreadsCreated = new AtomicLong();
	protected AtomicLong totalThreadsHung = new AtomicLong();		// based on timeout
	
	protected boolean shutdown = false;		// TODO replace with state - starting, running, stopping, stopped
	protected long scheduleFreq = 150;
	protected boolean poolTrace = false;
	
	public void init(OperationResult or, XElement config) {
		// place the default bucket in - it might be overridden in config
		WorkBucket defbucket = new WorkBucket();
		defbucket.init(or, new XElement("Bucket", new XAttribute("Name", "Default")), this.size);
		
		if (or.hasErrors())
			return;
		
		this.buckets.put(defbucket.getName(), defbucket);

		if (config != null) {
			this.size = Integer.parseInt(config.getAttribute("Threads", "16"));
			this.scheduleFreq = Integer.parseInt(config.getAttribute("TimeoutChecker", "150"));
			this.poolTrace = "True".equals(config.getAttribute("Trace", "False"));
						
 			for (XElement bucketel : config.selectAll("Bucket")) {
 				WorkBucket bucket = new WorkBucket();
 				bucket.init(or, bucketel, this.size);
 				
 				if (or.hasErrors())
 					return;
 				
 				this.buckets.put(bucket.getName(), bucket);
 			}
		}
		
		Hub.instance.getCountManager().allocateSetNumberCounter("dcWorkPool_Buckets", this.buckets.size());
		
		Hub.instance.getCountManager().allocateSetNumberCounter("dcWorkPool_Threads", this.size);
	}

	public void addBucket(WorkBucket bucket) {
		this.buckets.put(bucket.getName(), bucket);
	}

	public void removeBucket(String name) {
		this.buckets.remove(name);
	}
	
	public void init(int threads) {
		this.size = threads;
		
		Hub.instance.getCountManager().allocateSetNumberCounter("dcWorkPool_Threads", this.size);
	}
	
	public int threadCount() {
		return this.size; 
	}
	
	public long threadsCreated() {
		return this.totalThreadsCreated.get();
	}
	
	public void incThreadsCreated() {
		long n = this.totalThreadsCreated.incrementAndGet();
		
		Hub.instance.getCountManager().allocateSetNumberCounter("dcWorkPool_ThreadsCreated", n);
	}
	
	public long threadsHung() {
		return this.totalThreadsHung.get();
	}
	
	public void incThreadsHung() {
		long n = this.totalThreadsHung.incrementAndGet();

		Hub.instance.getCountManager().allocateSetNumberCounter("dcWorkPool_ThreadsHung", n);
	}
	
	public Collection<WorkBucket> getBuckets() {
		return this.buckets.values();
	}
	
	// to work as an Executor
	@Override
	public void execute(Runnable command) {
		if (command instanceof TaskRun)
			this.submit((TaskRun)command);		// useful for resume
		else {
			Task builder = new Task()
				.withWork(command);
			
			this.submit(builder, null);
		}
	}
	
	public TaskRun submit(IWork work) {
		Task task = new Task()
			.withWork(work);
	
		return this.submit(task);
	}
	
	public TaskRun submit(Task task) {
		TaskRun run = new TaskRun(task);
		
		this.submit(run);
		
		return run;
	}
	
	public TaskRun submit(IWork work, ITaskObserver observer) {
		Task task = new Task()
			.withWork(work);
	
		return this.submit(task, observer);
	}
	
	public TaskRun submit(Task task, ITaskObserver observer) {
		TaskRun run = new TaskRun(task);
		
		if (observer != null)
			run.addObserver(observer);
		
		this.submit(run);
		
		return run;
	}
	
	// this might accept "resubmits" or "new" - either way we should run "complete" if it fails
	public void submit(TaskRun run) {
		if (run == null)
			return;
		
		// don't run if shut down
		if (this.shutdown) {
			run.errorTr(197, run);
			run.complete();
			return;
		}
		
		// make sure context and logging, etc are ready
		run.prep();
		
		// this will also catch if run was resubmitted but killed
		if (run.hasErrors()) {
			run.errorTr(216, run);
			run.complete();
			return;
		}
		
		// after prep, prep will setup context 
		if (run.getContext() == null) {
			run.errorTr(198, run);
			run.complete();
			return;
		}
		
		// find the work bucket
		WorkBucket bucket = this.getBucketOrDefault(run);
		
		// see if the bucket advises a submit, if not the bucket will hold onto the run
		// in a wait queue.  if true then we put right on the active work queue
		if (bucket.canSubmit(run)) 
			this.queue.add(run);
	}
	
	public TaskRun take() throws InterruptedException {
		TaskRun run = this.queue.take();
		
		// find the work bucket
		WorkBucket bucket = this.getBucketOrDefault(run);
		
		// let the bucket know this run is in progress
		bucket.took(run);
		
		return run;
	}

	public void complete(TaskRun run) {
		// find the work bucket
		WorkBucket bucket = this.getBucketOrDefault(run);
		
		// tell the bucket to complete run
		TaskRun newrun = bucket.complete(run);
		
		// see if the bucket advises a submit
		if (newrun != null) {
			Logger.traceTr(199, newrun);
			this.queue.add(newrun);
		}
	}
	
	public WorkBucket getBucketOrDefault(String name) {
		WorkBucket bucket = this.buckets.get(name);
		
		if (bucket != null)
			return bucket;
		
		return this.buckets.get("Default");
	}
	
	public WorkBucket getBucketOrDefault(TaskRun run) {
		WorkBucket bucket = this.buckets.get(run.getTask().getBucket());
		
		if (bucket != null)
			return bucket;
		
		return this.buckets.get("Default");
	}
	
	public void start(OperationResult or) {
		for (int i = 0; i < this.size; i++) 
			this.initSlot(i);
		
		// the task defines a timeout, not the pool.  tasks with no timeout set
		// will simply not timeout and the pool will be burdened - so set timeouts
		// on tasks if there is any possibility that they might
		Hub.instance.getClock().addSlowSystemWorker(new ISystemWork() {
			@Override
			public void run(SysReporter reporter) {
				reporter.setStatus("Reviewing hung buckets");
				
				// even when stopping we still want to clear hung tasks
				for (Worker w : WorkPool.this.slots.values()) 
					w.checkIfHung();
				
				for (WorkBucket b : WorkPool.this.buckets.values()) 
					b.checkIfHung();
				
				reporter.setStatus("After reviewing hung buckets");
			}
			
			@Override
			public int period() {
				return 5;  // TODO remove/advise -- this.scheduleFreq);   
			}
		});    
	}
	
	protected void initSlot(int slot) {
		if (!this.shutdown) {
			Worker work = new Worker();
			this.slots.put(slot, work);
			work.start(slot);
		}
		else
			this.slots.remove(slot);
		
		Logger.trace("Thread Pool slot " + slot + " changed, now have " + this.slots.size() + " threads");
	}
	
	public void stopNice(OperationResult or) {
		or.trace(0, "Work Pool Stopping Nice");
		
		this.shutdown = true;
		
		for (Worker w : this.slots.values()) 
			w.stopNice();
	}
	
	public void stop(OperationResult or) {
		or.trace(0, "Work Pool Stopping");
		
		this.shutdown = true;
		
		or.trace(0, "Work Pool Stopping Nice");
		
		for (Worker w : this.slots.values()) 
			w.stopNice();
		
		or.trace(0, "Work Pool Waiting");
				
		// wait a minute for things to finish up.   -- TODO config
		for (int i = 0; i < 60; i++) {
			if (this.slots.size() == 0)
				break;
			
			try {
				Thread.sleep(1000);
			}
			catch (Exception x) {				
			}
		}
		
		or.trace(0, "Work Pool Size: " + this.slots.size());
		
		or.trace(0, "Work Pool Interrupt Remaining Workers");
		
		for (Worker w : this.slots.values()) 
			w.stop();
		
		or.trace(0, "Work Pool Stopped");
	}

	public int queued() {
		return this.queue.size();		
	}
	
	public RecordStruct toStatusReport() {
		RecordStruct rec = new RecordStruct();
		
		rec.setField("Queued", this.queued());
		rec.setField("Threads", this.threadCount());
		rec.setField("ThreadsCreated", this.threadsCreated());
		rec.setField("ThreadsHung", this.threadsHung());
		
		ListStruct buckets = new ListStruct();
		
		for (WorkBucket bucket : this.buckets.values()) 
			buckets.addItem(bucket.toStatusReport());

		rec.setField("Buckets", buckets);
		
		return rec;
	}
	
	// for a task by identity alone
	public RecordStruct status(String taskid) {
		/* TODO */
		
		return null;
	}
	
	// for a task by identity plus workid (slightly more secure)
	public RecordStruct status(String taskid, String workid) {
		for (WorkBucket bucket : this.buckets.values()) {
			TaskRun run = bucket.findTask(taskid);
			
			if (run != null) 
				return run.status();
		}

		return null;
	}

	// TODO start - someday make this a full working executor service
	@Override
	public boolean awaitTermination(long arg0, TimeUnit arg1)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> arg0)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> List<Future<T>> invokeAll(
			Collection<? extends Callable<T>> arg0, long arg1, TimeUnit arg2)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> arg0)
			throws InterruptedException, ExecutionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> arg0, long arg1,
			TimeUnit arg2) throws InterruptedException, ExecutionException,
			TimeoutException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isShutdown() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isTerminated() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Runnable> shutdownNow() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> Future<T> submit(Callable<T> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<?> submit(Runnable arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> Future<T> submit(Runnable arg0, T arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public int inprogress() {
		int cnt = 0;
		
		for (WorkBucket bucket : this.buckets.values()) 
			cnt += bucket.inprogress();
		
		return cnt;
	}
	
	// TODO end - someday make this a full working executor service
}
