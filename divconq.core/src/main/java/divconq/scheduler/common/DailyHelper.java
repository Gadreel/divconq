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
import divconq.util.TimeUtil;
import divconq.xml.XElement;

/*
 *  		<Daily>
 *  			<Schedule At="" RunIfMissed="True/False" />
 *  			<Schedule At="" RunIfMissed="True/False" />
 *  			<Schedule At="" RunIfMissed="True/False" />
 *  		</Daily>
 */
public class DailyHelper extends CommonHelper implements IDateChecker {
	protected ScheduleList list = new ScheduleList();
	
	// first time this code was run since the module/server was started
	// requires special check for any missed runs
	protected boolean firstCall = true;
	
	@Override
	public void init(CommonSchedule schedule, XElement config) {
		super.init(schedule, config);
		
		if (config == null)
			return;
		
		this.list.init(config);
	}
	
	// return null to indicate this can never run again
	@Override
	public DateTime next() {
		this.schedule.setHint("_ScheduleTimeHint", "");
		
		if (this.firstCall) {
			this.firstCall = false;
			
			if (this.limits.isEnded())
				return null;
			
			// if the valid range for schedule has not started yet, then 
			// schedule way out
			if (!this.limits.isStarted()) {
				this.firstCall = false;
				this.last = new DateTime(this.limits.getFirstDate());
			}
			// if there was no past runs, do not try to find them
			// or if there are not any required
			else if (this.last == null) {
				this.last = new DateTime().minusDays(9);		// start 8 days ago to allow for Run if Missed
				//this.last = TimeUtil.nextDayAtMidnight(new DateTime().minusDays(9));		// start 8 days ago to allow for Run if Missed
			}
			
			// see if we need to run because of a missed past schedule
			if (this.list.anyRequired()) {
				if (this.checkStartupNeedsRun(this.list, this)) {
					//System.out.println("run asap!!!!");
					this.last = this.limits.nextAllowedRun();
					return this.last;
				}
			}
		}
		
		ScheduleEntry se = this.list.next(this.last);
		
		while (true) {
			// null means we reached the end, go to next day
			if (se == null) {
				this.last = TimeUtil.nextDayAtMidnight(this.last);
				se = this.list.next(this.last);
			}
			
			// add the scheduled entry to the time
			// (first time with a given date, this is the time past midnight)
			if (se != null)
				this.last = TimeUtil.withTime(this.last, se.getTime());
			
			// we cannot schedule again, the schedule is expired
			if (this.limits.isEnded(this.last))
				return null;
			
			// can we run at the suggested time?
			if (this.limits.checkForRun(this.last)) {
				this.schedule.setHint("_ScheduleTimeHint", se.getTime().toString());
				return this.last;
			}
			
			// if not, should we run asap after the suggested time
			if (se.isRunIfMissed()) {
				this.schedule.setHint("_ScheduleTimeHint", se.getTime().toString());
				this.last = this.limits.nextAllowedRunAfter(this.last);
				return this.last;
			}
			
			// go on and check the next time
			se = this.list.next(this.last);
		}
	}

	@Override
	public boolean checkDate(CheckInfo info) {
		// always allowed
		return true;
	}
}
