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
package divconq.scheduler;

import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

import org.joda.time.DateTimeUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.joda.time.ReadableInstant;

import divconq.hub.Hub;
import divconq.hub.ISystemWork;
import divconq.hub.SysReporter;
import divconq.lang.FuncResult;
import divconq.lang.OperationContext;
import divconq.lang.OperationResult;
import divconq.log.Logger;
import divconq.scheduler.common.CommonSchedule;
import divconq.scheduler.limit.LimitHelper;
import divconq.struct.ListStruct;
import divconq.util.StringUtil;
import divconq.work.Task;
import divconq.work.WorkPool;
import divconq.xml.XElement;

/**
 * Handles scheduling application tasks.
 * 
 * @author Andy White
 *
 */
// TODO add tracing settings
public class Scheduler {
	// the first node in the list of scheduled nodes - the head of the list is moved
	// forward as the items on the list get scheduled.  List is single linked list.
	protected SchedulerNode first = null;

	// how many are currently in the linked list?
	protected long nodeCnt = 0;
	
	// lock during adding and removing of scheduled work.  all add and remove operations
	// are thread safe
	protected ReentrantLock lock = new ReentrantLock();
	
	protected ScheduledFuture<?> clock = null;
	
	protected HashMap<String,LimitHelper> batches = new HashMap<String,LimitHelper>();
	
	protected ISchedulerDriver driver = null;
	
	public LimitHelper getBatch(String name) {
		return this.batches.get(name);
	}
	
	public void init(OperationResult or, XElement config) {
		// TODO

		if (config != null) {
			for (XElement el : config.selectAll("Batch")) {
				XElement bel = el.find("Limits");
				String name = el.getAttribute("Name");
				
				if (StringUtil.isNotEmpty(name) && (bel != null)) {
					LimitHelper h = new LimitHelper();
					h.init(bel);
					this.batches.put(name, h);
				}				
			}
			
			// setup the provider of the work queue
			String classname = config.getAttribute("InterfaceClass");
			
			if (StringUtil.isNotEmpty(classname)) {
				Object impl =  Hub.instance.getInstance(classname);		
				
				if ((impl == null) || !(impl instanceof ISchedulerDriver)) {
					or.errorTr(227, classname);
					return;
				}
				
				this.driver = (ISchedulerDriver)impl;
				this.driver.init(or, config);
			}
		}
	}
	
	public void start(OperationResult or) {
		Hub.instance.getClock().addFastSystemWorker(new ISystemWork() {			
			@Override
			public void run(SysReporter reporter) {
				Scheduler.this.execute();
			}
			
			@Override
			public int period() {
				return 1;
			}
		});
		
		Hub.instance.getClock().addSlowSystemWorker(new ISystemWork() {			
			@Override
			public void run(SysReporter reporter) {
				reporter.setStatus("before schedule update");
				// TODO check for updates to the schedule
				reporter.setStatus("after schedule update");
			}
			
			@Override
			public int period() {
				return 5;
			}
		});
		
		if (this.driver != null) {
			FuncResult<ListStruct> loadres = this.driver.loadSchedule();
			
			or.copyMessages(loadres);
			
			if (loadres.isNotEmptyResult()) {				
				loadres.getResult().recordStream().forEach(rec -> {
					XElement schedule = rec.getFieldAsXml("Schedule");
					
					ISchedule sched = "CommonSchedule".equals(schedule.getName()) ? new CommonSchedule() : new SimpleSchedule();
					
					sched.init(schedule);
					
					sched.setTask(new Task()
						.withId(divconq.session.Session.nextTaskId("ScheduleLoader"))
						.withTitle("Scheduled Task Loader: " + rec.getFieldAsString("Title"))
						.withRootContext()
						.withWork(trun -> {
								FuncResult<ScheduleEntry> loadres2 = Scheduler.this.driver.loadEntry(rec.getFieldAsString("Id"));

								trun.copyMessages(loadres2);
								
								if (loadres2.isNotEmptyResult()) {
									ScheduleEntry entry = loadres2.getResult();
									
									entry.setSchedule(sched);
									
									entry.submit(trun);
								}
								
								// we are done, no need to wait 
								trun.complete();
						})
					);
					
					Scheduler.this.addNode(sched);
				});
			}
		}
	}
	
	public void stop(OperationResult or) {
		// TODO
		
		if (this.clock != null)
			this.clock.cancel(false);
	}

	public long size() {
		return this.nodeCnt;
	}

	// the scheduler runs on its own thread, this is the code that starts and runs the scheduler
	private void execute() {
		OperationContext.useHubContext();
		
		long loadcnt = this.nodeCnt;
		
		this.lock.lock();

		try {
			SchedulerNode curr = this.first;

			long now = DateTimeUtils.currentTimeMillis();
			WorkPool p = Hub.instance.getWorkPool();
			
			//System.out.println(new DateTime() +  " - scheduler - " + new DateTime(now) + " > " + new DateTime(curr.when));

			while ((curr != null) && (curr.when <= now)) {
				//System.out.println("Scheduled node: " + curr.scheduler.isCanceled());
				
				if (!curr.scheduler.isCanceled())
					p.submit(curr.task, curr.scheduler);
				
				curr = curr.next;
				this.first = curr;
				this.nodeCnt--;
			}
			
			loadcnt = this.nodeCnt;
		}
		catch(Exception x) {
			// TODO trace/log
		}		
		finally {
			this.lock.unlock();
		}
		
		// it is possible due to race conditions to get a mis-ordered value in the counter
		// a) it doesn't matter 99.99999999% of the time, b) we cannot afford to do this in the lock
		Hub.instance.getCountManager().allocateSetNumberCounter("dcSchedulerLoad", loadcnt);
	}

	// add a work unit to run (almost) immediately - much the same as directly adding to the thread pool
	// any work unit submitted to the scheduler (or to any thread pool) will become owned by the
	// scheduler (or thread pool).
    public ISchedule runNow(Task work) {
    	return this.addNode(new SimpleSchedule(work, DateTimeUtils.currentTimeMillis(), 0));
    }

	// run the work unit once in Sec seconds from now
	public ISchedule runIn(Task work, int secs) {
		return this.addNode(new SimpleSchedule(work, DateTimeUtils.currentTimeMillis() + (1000 * secs), 0));		
	}

	// run the work unit once at the specified time.  If less than now then submits immediately
	// if in the distant future, it may not be run if the process is terminated.  adding working
	// to the schedule is no guarantee work will be run.
	public ISchedule runAt(Task work, ReadableInstant time) {
		return this.addNode(new SimpleSchedule(work, time.getMillis(), 0));				
	}
	
	public ISchedule runAt(Task work, LocalDate date, Period period) {
		LocalDateTime ldt = date.toLocalDateTime(new LocalTime(0, 0).plus(period));
		return this.runAt(work, ldt.toDateTime());
	}

	// run the work unit repeatedly, every Secs seconds - note scheduler will not own work, you have to keep track of it
	public ISchedule runEvery(Task work, int secs) {
		return this.addNode(new SimpleSchedule(work, DateTimeUtils.currentTimeMillis() + (1000 * secs), secs));		
	}
	
	public ISchedule addNode(ISchedule schedule) {
		long when = schedule.when();
		
		if (when < 0)
			return null;
		
		if ((schedule.task() == null) || (schedule.task().getContext() == null)) {
			Logger.warn("Schedule missing task or context: " + schedule.task());
			return null;
		}
		
		long loadcnt = this.nodeCnt;
		
		this.lock.lock();

        try {
        	SchedulerNode snode = new SchedulerNode();
        	snode.task = schedule.task();
        	snode.when = when;
        	snode.scheduler = schedule;

			SchedulerNode curr = this.first;
            SchedulerNode last = null;

			this.nodeCnt++;

			// loop through the scheduling linked list and find the right place to insert
			// the new TScheduleNode
            while (curr != null) {
				if (snode.when < curr.when) {
                    snode.next = curr;

					if (last == null) 
						this.first = snode;
					else 
						last.next = snode;

                    return schedule;
                }

                last = curr;
				curr = curr.next;
            }

            // none found then add to end
			if (last == null) 
				this.first = snode;
			else 
				last.next = snode;
			
			loadcnt = this.nodeCnt;
        }
        catch(Exception x) {
        	// TODO

            return null;
        }
        finally {
        	this.lock.unlock();
        }
		
		// it is possible due to race conditions to get a mis-ordered value in the counter
		// a) it doesn't matter 99.99999999% of the time, b) we cannot afford to do this in the lock
		Hub.instance.getCountManager().allocateSetNumberCounter("dcSchedulerLoad", loadcnt);

        return schedule;
	}

	public class SchedulerNode {
		protected SchedulerNode next = null; 
		protected long when = 0;	
		protected Task task = null;
		protected ISchedule scheduler = null;
	}
	
	public void dump() {
		SchedulerNode curr = this.first;

        while (curr != null) {
    		Logger.info("     + " + curr.task.getTitle());
        	
			curr = curr.next;
        }
	}
}
