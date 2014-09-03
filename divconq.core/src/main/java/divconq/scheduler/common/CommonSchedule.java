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
package divconq.scheduler.common;

import org.joda.time.DateTime;

import divconq.hub.Hub;
import divconq.lang.OperationResult;
import divconq.scheduler.ISchedule;
import divconq.scheduler.limit.LimitHelper;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;
import divconq.util.TimeUtil;
import divconq.work.Task;
import divconq.work.TaskRun;
import divconq.xml.XElement;

// TODO respect Hub idled
public class CommonSchedule implements ISchedule {
	static public final int METHOD_NONE = 0;
	static public final int METHOD_STANDARD = 1;
	static public final int METHOD_SCRIPT = 2;
	static public final int METHOD_CLASS = 3;
	
	// what method is used to calculate the run times
	protected int method = CommonSchedule.METHOD_NONE;

	protected IScheduleHelper helper = null;
	
	// limits handler
	protected LimitHelper limits = new LimitHelper();
	
	// when was this last run, leave null if not important
	protected DateTime last = null;
	
	// the code to run on schedule
	protected Task task = null;
	
	protected IInlineScript iscript = null;
	
	protected Object userData = null;
	protected RecordStruct hints = new RecordStruct();
	
	protected boolean canceled = false;
	
	public void setLastRun(DateTime v) {
		this.last = v;
	}
	
	public DateTime getLastRun() {
		return this.last;
	}
	
	public void setInlineScript(IInlineScript v) {
		this.iscript = v;
	}
	
	public IInlineScript getInlineScript() {
		return this.iscript;
	}
	
	public void setTask(Task v) {
		this.task = v;
	}

	@Override
	public Task task() {
		return this.task;
	}
	
	public void setUserData(Object v) {
		this.userData = v;
	}
	
	public Object getUserData() {
		return this.userData;
	}

	@Override
	public RecordStruct getHints() {
		return this.hints;
	}
	
	public void setHint(String name, String value) {
		this.hints.setField(name, value);
	}
	
	/*
	 *  	<CommonSchedule 
	 *  		Method="None,Standard,Script,Class"
	 *  		View="Period,Daily,Weekly,Monthly,Script,Custom"    - for the UI to determine which pane to show 
	 *  		ClassName="n"		- use the bundle provided, if any, to load the class
	 *  							- must implement IScheduleHelper
	 *  	>
	 *			<Limits ... />		- see LimitHelper
	 *  
	 *  		// use ISO periods, e.g. PT2H30M10S
	 *  		// used for intra-daily mostly, but can be any
	 *  		<Period Value="n" />   
	 *  
	 *  		// if method = Daily, these are the times to run, ignore frequency
	 *  		<Daily>
	 *  			<Schedule At="" RunIfMissed="True/False" />
	 *  			<Schedule At="" RunIfMissed="True/False" />
	 *  			<Schedule At="" RunIfMissed="True/False" />
	 *  		</Daily>
	 *  
	 *  		// if method = Weekly, these are the days to run.  may be more than one WeekDays, use first match
	 *  		<Weekly>
	 *  			<Weekdays Monday="T/F" Tuesday="n" ... All="T/F" >
	 *  				<Schedule At="" RunIfMissed="True/False" />
	 *  			</Weekdays>
	 *  		</Weekly>
	 *  
	 *  		// if method = "Monthly" (may be more than Months, etc)
	 *  		// excludes for monthly don't make sense, but are there 
	 *  		<Monthly>
	 *  			<Months January="T/F" ... All="T/F" >
	 *  				<First Monday="T/F" Tuesday="n" ... All="T/F" >
	 *  					<Schedule At="" RunIfMissed="True/False" />
	 *  				</First>
	 *  				<Second Monday="T/F" Tuesday="n" ... All="T/F" >
	 *  					<Schedule At="" RunIfMissed="True/False" />
	 *  				</Second>
	 *  				... etc, or ...
	 *  				<Monthday List="N,N,N,Last">
	 *  					<Schedule At="" RunIfMissed="True/False" />
	 *  				</Monthday> 
	 *  			</Months>
	 *  		</Monthly>
	 *  
	 *  		// _last is available, but = null if first run
	 *  		// _now is available 
	 *  		// if method != Script then there is an obj _suggested
	 *  		// to call "suggest.next()" that will provide next based
	 *  		// on the method settings (so script is more a filter)
	 *  		// when method is script then no hints are provided  
	 *  		<Script> run when scheduling next </Script>
	 *  	</CommonSchedule>
	 */
	
	public void init(XElement config) {
		// TODO load config, if classes are involved then use custom loader if available
		
		if (config != null) {
			this.limits.init(config.find("Limits"));
			
			// what method is used to calculate the run times
			String meth = config.getAttribute("Method");
			
			if (StringUtil.isEmpty(meth))
				meth = "Standard";
			
			XElement helpel = null;
			
			if ("Standard".equals(meth)) {
				this.method = CommonSchedule.METHOD_STANDARD;
				
				helpel = config.find("Period");
				
				if (helpel != null) {
					this.helper = new PeriodHelper();
				}
				else {					
					helpel = config.find("Daily");
					
					if (helpel != null) {
						this.helper = new DailyHelper();
					}
					else {
						helpel = config.find("Weekly");
						
						if (helpel != null) {
							this.helper = new WeekdayHelper();
						}
						else {
							helpel = config.find("Monthly");
							
							if (helpel != null) {
								this.helper = new MonthHelper();
							}
							else {
								// TODO log
								System.out.println("schedule does not appear to have a helper");
							}
						}
					}					
				}
			}
			else if ("Script".equals(meth)) {
				this.method = CommonSchedule.METHOD_SCRIPT;
				
				XElement sel = config.find("Script");
				
				if (sel != null) {
					/*  TODO
					String code = sel.getText();
					
					if (!StringUtil.isBlank(code)) {
						this.script = new Script();
						
						try {
							this.script.setScript(code);
						} 
						catch (Exception e) {
							// TODO log
						}
					}
					
					// TODO
					 * 
					 */
				}
			}
			else if ("Class".equals(meth)) {
				this.method = CommonSchedule.METHOD_CLASS;
				
				String className = config.getAttribute("ClassName");
				
				try {
					// TODO
					//this.helper = (IScheduleHelper) ((this.customLoader != null) 
					//	? this.customLoader.getInstance(className) 
					//	: Class.forName(className).newInstance());
					
					this.helper = (IScheduleHelper) Hub.instance.getInstance(className);
					
					helpel = config;
				} 
				catch (Exception e) {
					// TODO log
					System.out.println("unable to load schedule helper class: " + className);
				}	
			}
			
			if (this.helper != null) {
				this.helper.setLimits(this.limits);
				this.helper.setLast(this.last);
				this.helper.init(this, helpel);
			}
		}

		// setup the first run
		this.reschedule();
	}
	
	public void init(IScheduleHelper helper, XElement limits, XElement hconfig) {
		// TODO load config, if classes are involved then use custom loader if available
		
		this.limits.init(limits);
		
		this.method = CommonSchedule.METHOD_CLASS;
		
		this.helper = helper;
		
		this.helper.setLimits(this.limits);
		this.helper.setLast(this.last);
		this.helper.init(this, hconfig);

		// setup the first run
		this.reschedule();
	}

	// same as reschedule, except we must move forward at least one day
	public boolean rescheduleOnNextDate() {
		this.last = TimeUtil.nextDayAtMidnight(this.last);		
		return this.reschedule();
	}

	@Override
	public boolean reschedule() {
		if (this.helper != null) {
			// TODO if script then use that to help with finding next
			this.last = this.helper.next();
			
			//System.out.println("rescheduled: " + ((this.last == null) ? "null" : this.last.toString()));
			
			return (this.last != null);
		}
		/* TODO
		else if (this.script != null) {
			this.script.addVariable("_now", new DateTime());
			this.script.addVariable("_last", this.last);
			this.script.addVariable("_sched", this);
			this.script.addVariable("_data", this.userData);
			
			try {
				Object o = this.script.call("schedule");	// TODO consider passing params rather than set vars above	
				
				if (o instanceof DateTime)
					this.last = (DateTime)o;
				else
					System.out.println("bad return value: " + o);		// TODO log
			}
			catch (Exception x) {
				System.out.println("bad return value: " + x);		// TODO log
			}
			
			System.out.println("rescheduled script: " + ((this.last == null) ? "null" : this.last.toString()));
			
			return (this.last != null);
		}
		*/
		else if (this.iscript != null) {
			try {
				this.last = this.iscript.schedule(this);	// TODO consider passing params rather than set vars above	
			}
			catch (Exception x) {
				System.out.println("bad return value: " + x);		// TODO log
			}
			
			System.out.println("rescheduled script: " + ((this.last == null) ? "null" : this.last.toString()));
			
			return (this.last != null);
		}
		
		return false;
	}

	@Override
	public long when() {
		if ((this.task != null) && (this.last != null))
			return this.last.getMillis();
		
		return -1;
	}

	@Override
	public void cancel() {
		this.canceled = true;
		
		// TODO someday optimize by removing from scheduler node list
	}
	
	@Override
	public boolean isCanceled() {
		return this.canceled;
	}

	@Override
	public void completed(TaskRun or) {
		// remember - we can look at the task for further info when rescheduling
		// run.getResult();
		
		if (this.reschedule()) {
			//this.task.reset();
			Hub.instance.getScheduler().addNode(this);
		}
	}

	@Override
	public void log(OperationResult run, RecordStruct entry) {
	}

	@Override
	public void boundary(OperationResult run, String... tags) {
	}

	@Override
	public void step(OperationResult run, int num, int of, String name) {
	}

	@Override
	public void progress(OperationResult run, String msg) {
	}

	@Override
	public void amount(OperationResult run, int v) {
	}

	@Override
	public void prep(TaskRun or) {
	}

	@Override
	public void start(TaskRun or) {
	}

	@Override
	public void stop(TaskRun or) {
	}
}
