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
package divconq.hub;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.ReadableInstant;

import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.log.Logger;
import divconq.util.BasicSettingsObfuscator;
import divconq.util.ISettingsObfuscator;
import divconq.util.StringUtil;
import divconq.util.TimeUtil;
import divconq.xml.XElement;

/**
 * Clock has three uses:
 * 
 * 1) Control the date time settings - DivConq's DateTime are based on Joda's library.
 *    With Joda it is possible to set the effective timezone and the current date time
 *    itself.  Through configuration the Clock can be made to enforce a fixed time,
 *    a sped up time (clock moving faster than 1 second per system second), or a regular
 *    time.  Also the time zone can be set.
 * 2) There are some methods for scheduling work on another thread. However,  
 *    typically one should use {@link divconq.scheduler.Scheduler} for task scheduling.
 * 3) DivConq supports obfuscated (encrypted) settings in the config files.  This class
 *    provides access to the feature for encrypting and decrypting config settings.
 *    
 * @author Andy
 *
 */
public class Clock {
	class ClockThreadFactory implements ThreadFactory {
		protected String name = null;
		protected boolean daemon = true;
		
		public ClockThreadFactory(String name, boolean daemon) {
			this.name = name;
			this.daemon = daemon;
		}
		
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, this.name);
			
			t.setDaemon(this.daemon);
			
			// TODO consider this option
			//t.setUncaughtExceptionHandler(eh);
			
			return t;
		}
	}
	
    // periodic internal intervals, not for use with scheduling real tasks like dcTasks 
	// does not preserve user context, uses system clock not divconq clock

	// for the fast clock which run only tasks that are very quick to execute, < 10 ms total per entire run
	protected ScheduledExecutorService fastscheduler = Executors.newSingleThreadScheduledExecutor(
			new ClockThreadFactory("FastScheduler", false)   		//  we need something to keep us alive in Service mode - fast is not a daemon
	);
	
	protected ScheduledFuture<?> fastclock = null;
	protected List<ISystemWork> fastsyswork = new CopyOnWriteArrayList<>(); 
	protected int fastsysworkcycle = 1;
	protected SysReporter fastreporter = new SysReporter();

	// for the clock itself
	
	// how many seconds to travel per system second - only enabled if using CoreDate
	protected long speed = 0;
	protected XElement config = null;	
	protected ISettingsObfuscator obfus = null;	
	protected DateTimeZone serverTimeZone = null;
	
	// for the slow clock - which is still fairly fast - execute system tasks that take less < 100ms total per entire run.
	// in a bad case, if it took a little longer not a problem...but generally runs fastish
	protected ScheduledExecutorService slowscheduler = Executors.newSingleThreadScheduledExecutor(
			new ClockThreadFactory("SlowScheduler", true)
	);
	
	protected ScheduledFuture<?> slowclock = null;	
	protected List<ISystemWork> slowsyswork = new CopyOnWriteArrayList<>(); 
	protected int slowsysworkcycle = 1;	
	protected SysReporter slowreporter = new SysReporter();

	public SysReporter getFastSysReporter() {
		return this.fastreporter;
	}

	public SysReporter getSlowSysReporter() {
		return this.slowreporter;
	}
	
	
	public DateTimeZone getServerTimeZone() {
		return this.serverTimeZone;
	}
	
	public Clock() {
		this.serverTimeZone = DateTimeZone.getDefault();
	}
	
    /**
     * Called from Hub.start this method configures the clock features.
     * 
     * @param or logger for the initialize
     * @param config xml holding the configuration
     */
	public void init(OperationResult or, XElement config) {
		this.config = config;
		
		if (config != null) {
			String timeZone = config.getAttribute("TimeZone", "UTC");
			
			if (StringUtil.isNotEmpty(timeZone))
				DateTimeZone.setDefault(DateTimeZone.forID(timeZone));
			
			String coreDate = config.getAttribute("CoreDate");
			
			if (StringUtil.isNotEmpty(coreDate)) {
				this.setAppClock(TimeUtil.parseDateTime(coreDate));
			}
			
			this.speed = StringUtil.parseInt(config.getAttribute("Speed"), 0);
			
			String obclass = config.getAttribute("TimerClass");
			
			if (StringUtil.isNotEmpty(obclass)) {
				try {
					Class<?> obc = this.getClass().getClassLoader().loadClass(obclass);
					this.obfus = (ISettingsObfuscator) obc.newInstance();
				}
				catch (Exception x) {
					Logger.error("Unable to load custom Settings Obfuscator class: " + obclass, "Code", "207");
				}
			}
		}

		if (this.obfus == null)
			this.obfus = new BasicSettingsObfuscator();
		
		this.obfus.init(config);
	}

	/**
	 * If the framework time is configured to use an accelerated clock, this method gets 
	 * that accelerated clock going.
	 * 
	 * @param or logger for the start up
	 */
	public void start(OperationResult or) {		
		// we check our app schedule ever 1 (or so) system seconds
		this.slowclock = this.slowscheduler.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				OperationContext.useHubContext();
				
				int cycle = Clock.this.slowsysworkcycle;
				
				Clock.this.slowreporter.setStatus("Slow Sys starting cycle: " + cycle);
				
				for (ISystemWork work : Clock.this.slowsyswork) {
					try {
						int period = work.period();
						
						if (period > 300)
							period = 300;		// max allowed						
						
						if (cycle % period  == 0) {
							Clock.this.slowreporter.setStatus("before sys work: " + work.getClass());
							work.run(Clock.this.slowreporter);
							Clock.this.slowreporter.setStatus("after sys work: " + work.getClass());
						}
					}
					catch(Exception x) {
						System.out.println("sys scheduler error: " + x);
					}
				}
				
				Clock.this.slowreporter.setStatus("Slow Sys finished cycle: " + cycle);
				
				cycle++;
				
				if (cycle > 300)
					cycle = 1;
				
				Clock.this.slowsysworkcycle = cycle;
			}
		}, 1, 1, TimeUnit.SECONDS);
		
		// we check our app schedule ever 1 (or so) system seconds
		this.fastclock = this.fastscheduler.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				OperationContext.useHubContext();
				
				int cycle = Clock.this.fastsysworkcycle;
				
				Clock.this.fastreporter.setStatus("Fast Sys starting cycle: " + cycle);
				
				for (ISystemWork work : Clock.this.fastsyswork) {
					try {
						int period = work.period();
						
						if (period > 300)
							period = 300;		// max allowed						
						
						if (cycle % period  == 0) {
							Clock.this.fastreporter.setStatus("before sys work: " + work.getClass());
							work.run(Clock.this.fastreporter);
							Clock.this.fastreporter.setStatus("after sys work: " + work.getClass());
						}
					}
					catch(Exception x) {
						System.out.println("sys scheduler error: " + x);
					}
				}
				
				Clock.this.fastreporter.setStatus("Fast Sys finished cycle: " + cycle);
				
				cycle++;
				
				if (cycle > 300)
					cycle = 1;
				
				Clock.this.fastsysworkcycle = cycle;
			}
		}, 1, 1, TimeUnit.SECONDS);
		
		if (this.speed != 0) 
			this.addFastSystemWorker(new ISystemWork() {
				@Override
				public int period() {
					return 1;
				}

				@Override
				public void run(SysReporter reporter) {
					reporter.setStatus("Before set date time");
					DateTimeUtils.setCurrentMillisFixed(DateTimeUtils.currentTimeMillis() + (Clock.this.speed * 1000));
					reporter.setStatus("After set date time");
				}
			});
	} 

	/**
	 * Terminate tasks being run on the Clock's scheduler.  Called by
	 * Hub.stop during the final steps of shutdown.
	 * 
	 * @param or logger for the shutdown 
	 */
	public void stop(OperationResult or) {
		if (this.slowclock != null)
			this.slowclock.cancel(false);
		
		if (this.fastclock != null)
			this.fastclock.cancel(false);
		
		this.slowscheduler.shutdown();
		this.fastscheduler.shutdown();
		
		try {
			this.slowscheduler.awaitTermination(60, TimeUnit.SECONDS);
			this.fastscheduler.awaitTermination(60, TimeUnit.SECONDS);
		} 
		catch (InterruptedException e) {
			// TODO 
		}
		
		this.slowscheduler.shutdownNow();
		this.fastscheduler.shutdownNow();
	}
	
	// your period must be 300 or less or it will never occur
	// you must be very careful with this, problems in your sysworker will lock down the whole hub process 
	public void addSlowSystemWorker(ISystemWork v) {
		if (v == null)
			return;
		
		this.slowsyswork.add(v);
	}
	
	/**
	 * Typically use {@link divconq.scheduler.Scheduler} for task scheduling.
	 * However, when a task should run on the system clock rather than the 
	 * app clock, these methods may be used.  Use sparingly as these are run on
	 * a single threaded pool.  These methods also do not preserve the TaskContext.
	 * 
	 * @param v new work for the fast thread
	 */	
	// your period must be 300 or less or it will never occur
	// you must be very careful with this, problems in your sysworker will lock down the whole hub process 
	public void addFastSystemWorker(ISystemWork v) {
		if (v == null)
			return;
		
		this.fastsyswork.add(v);
	}

	/**
	 * set framework's time to use the system time
	 */
	public void resetAppClock() {
		DateTimeUtils.setCurrentMillisSystem();
	}
	
	/**
	 * set framework's time to use a fixed time
	 * 
	 * @param time fixed time to use
	 */
	public void setAppClock(ReadableInstant time) {
		DateTimeUtils.setCurrentMillisFixed(time.getMillis());
	}
	
	/**
	 * Typically use {@link divconq.scheduler.Scheduler} for task scheduling.
	 * However, when a task should run on the system clock rather than the 
	 * app clock, these methods may be used.  Use sparingly as these are run on
	 * a single threaded pool.  These methods also do not preserve the TaskContext.
	 * 
	 * @param command the code for the task to run
	 * @param delaySecs how many seconds until the task is run
	 * @return the token for canceling the task 
	 */	
	public ScheduledFuture<?> scheduleOnceInternal(Runnable command, long delaySecs) {
		return this.slowscheduler.schedule(command, delaySecs, TimeUnit.SECONDS);
	}
	
	/**
	 * Typically use {@link divconq.scheduler.Scheduler} for task scheduling.
	 * However, when a task should run on the system clock rather than the 
	 * app clock, these methods may be used.  Use sparingly as these are run on
	 * a single threaded pool.  These methods also do not preserve the TaskContext.
	 * 
	 * @param command the code for the task to run
	 * @param delay how many "units" until the task is run
	 * @param unit time unit used with delay
	 * @return the token for canceling the task 
	 */	
	public ScheduledFuture<?> scheduleOnceInternal(Runnable command, long delay, TimeUnit unit) {
		return this.slowscheduler.schedule(command, delay, unit);
	}
	
	/**
	 * Typically use {@link divconq.scheduler.Scheduler} for task scheduling.
	 * However, when a task should run on the system clock rather than the 
	 * app clock, these methods may be used.  Use sparingly as these are run on
	 * a single threaded pool.  These methods also do not preserve the TaskContext.
	 * 
	 * @param command the code for the task to run
	 * @param periodSecs how many seconds until the task is run first time, and then how long between runs
	 * @return the token for canceling the task 
	 */	
	public ScheduledFuture<?> schedulePeriodicInternal(Runnable command, long periodSecs) {
		return this.slowscheduler.scheduleAtFixedRate(command, periodSecs, periodSecs, TimeUnit.SECONDS);
	}
	
	/**
	 * Typically use {@link divconq.scheduler.Scheduler} for task scheduling.
	 * However, when a task should run on the system clock rather than the 
	 * app clock, these methods may be used.  Use sparingly as these are run on
	 * a single threaded pool.  These methods also do not preserve the TaskContext.
	 * 
	 * @param command the code for the task to run
	 * @param initialDelay how many "units" until the task is first run
	 * @param period how many "units" between task runs, after first run
	 * @param unit time unit used with delay and period
	 * @return the token for canceling the task 
	 */	
	public ScheduledFuture<?> schedulePeriodicInternal(Runnable command, long initialDelay, long period, TimeUnit unit) {
		return this.slowscheduler.scheduleAtFixedRate(command, initialDelay, period, unit);
	}
	
	/**
	 * Config file settings containing sensitive info may be obscured (encypted)
	 * to make it hard for a hacker to get anything useful from just a copy of the
	 * config file.  How the settings are obscured is based on the Clock's settings.
	 * 
	 * For more details on how this works @see divconq.util.ISettingsObfuscator
	 *  
	 * @return the obfuscator used with this settings file 
	 */
	public ISettingsObfuscator getObfuscator() {
		return this.obfus;
	}
}
