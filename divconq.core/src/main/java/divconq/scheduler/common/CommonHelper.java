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

import divconq.scheduler.limit.CheckInfo;
import divconq.scheduler.limit.LimitHelper;
import divconq.util.TimeUtil;
import divconq.xml.XElement;

abstract public class CommonHelper implements IScheduleHelper {
	protected XElement config = null;
	protected LimitHelper limits = null;
	protected DateTime last = null;
	protected CommonSchedule schedule = null;

	@Override
	public void init(CommonSchedule schedule, XElement config) {
		this.schedule = schedule;
		this.config = config;
	}

	@Override
	public void setLimits(LimitHelper limits) {
		this.limits = limits;
	}

	@Override
	public void setLast(DateTime last) {
		this.last = last;
	}
	
	/*
	// return true if passes the limits check
	public boolean checkLimits(Instant v) {
		if (this.limits != null)
			return this.limits.check(v);
		
		return true;
	}
	*/
	
	// used by daily, weekday and monthday helpers
	protected boolean checkStartupNeedsRun(ScheduleList list, IDateChecker checker) {
		// do not alter last.  either we will use it in the scheduler normally
		// or, if we return true, we will set last to run asap
		CheckInfo ci = new CheckInfo();
		ci.setWhen(this.last);
		
		// starting with last date, run through and check all dates until now
		// to see if any are open, if so it is valid to run for past		
		while (true) {
			if (checker.checkDate(ci) && !this.limits.isDateBlocked(ci.getWhen())) {
				DateTime tlast = ci.getWhen();		
				ScheduleEntry se = list.next(tlast);
				
				// now check today to see if we should have run earlier today
				// note that if last started out as a past date that we would now
				// be pointing at midnight today.  if last started out as today
				// then we would still be at the last run today value (e.g. last run 2 hours ago)
				while (se != null) {
					// add the scheduled entry to the time
					// (first time with a given date, this is the time past midnight)
					tlast = TimeUtil.withTime(tlast, se.getTime());
					
					// if this schedule entry comes after now, then schedule normally
					if (!tlast.isBeforeNow())
						return false;
						
					// we cannot schedule again, the schedule is expired
					// this will have to be figured out by the normal scheduler
					if (this.limits.isEnded(tlast))
						return false;
					
					// did we miss a required run today? if so tell the normal scheduler
					// to run asap
					if (se.isRunIfMissed()) {
						this.schedule.setHint("_ScheduleTimeHint", se.getTime().toString());
						return true;
					}
					
					// go on and check the next time today
					se = list.next(tlast);
				}
			}
			
			ci.incrementDay();
		}		
	}
}
