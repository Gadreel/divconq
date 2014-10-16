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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.joda.time.DateTime;

import divconq.scheduler.limit.CheckInfo;
import divconq.util.TimeUtil;
import divconq.xml.XElement;

public class WeekdayHelper extends CommonHelper {
	protected List<WeekdayEntry> weekdays = new ArrayList<WeekdayEntry>();
	
	// first time this code was run since the module/server was started
	// requires special check for any missed runs
	protected boolean firstCall = true;
	
	@Override
	public void init(CommonSchedule schedule, XElement config) {
		if (config != null) {
			for (XElement el : config.selectAll("Weekdays")) {
				WeekdayEntry entry = new WeekdayEntry();
				entry.init(el);
				this.weekdays.add(entry);
			}
		}
		
		if (this.weekdays.size() == 0) {
			WeekdayEntry entry = new WeekdayEntry();
			entry.init(null);
			this.weekdays.add(entry);
		}
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
			//else if (this.last == null) {
			//	this.last = new DateTime();		// start with today (now)
			//}
			// see if we need to run because of a missed past schedule
			else {
				if (this.last == null) 
						this.last = new DateTime(1970, 1, 1, 0, 0, 0, 0);		// start with today (now)
						
				for (WeekdayEntry we : this.weekdays) {
					if (this.checkStartupNeedsRun(we.dailySchedule, we)) {		
						//System.out.println("run asap!!!!");
						this.last = this.limits.nextAllowedRun();
						return this.last;
					}
				}

				if (this.last == null)
					this.last = new DateTime();		// start with today (now)
			}
		}
		
		// we should never get here with a really out of date "last", today is fine
		CheckInfo ci = new CheckInfo();
		ci.setWhen(this.last);

		while (true) {
			for (WeekdayEntry we : this.weekdays) {
				if (we.checkDate(ci)) {
					ScheduleEntry se = we.dailySchedule.next(ci.getWhen());
					
					while (se != null) {
						// add the scheduled entry to the time
						// (first time with a given date, this is the time past midnight)
						DateTime tlast = TimeUtil.withTime(ci.getWhen(), se.getTime());
						
						// we cannot schedule again, the schedule is expired
						if (this.limits.isEnded(tlast)) {
							this.last = null;
							return null;
						}
						
						// can we run at the suggested time?
						if (this.limits.checkForRun(tlast)) {
							this.last = tlast;
							return this.last;
						}
						
						// if not, should we run asap after the suggested time
						if (se.isRunIfMissed()) {
							this.last = this.limits.nextAllowedRunAfter(tlast);
							return this.last;
						}
						
						// go on and check the next time
						se = we.dailySchedule.next(tlast);
					}
					
					break;
				}
			}
			
			ci.incrementDay();
		}
	}
	
	class WeekdayEntry implements IDateChecker {
		ScheduleList dailySchedule = new ScheduleList();		
		// (0 = monday, 6 = sunday)
		BitSet dayOfWeek = new BitSet(7);
		
		public void init(XElement config) {
			if (config != null) {
				if ("True".equals(config.getAttribute("All")))
					this.dayOfWeek.set(0, 6);
	
				if ("True".equals(config.getAttribute("Monday")))
					this.dayOfWeek.set(0);
				
				if ("True".equals(config.getAttribute("Tuesday")))
					this.dayOfWeek.set(1);
				
				if ("True".equals(config.getAttribute("Wednesday")))
					this.dayOfWeek.set(2);
				
				if ("True".equals(config.getAttribute("Thursday")))
					this.dayOfWeek.set(3);
				
				if ("True".equals(config.getAttribute("Friday")))
					this.dayOfWeek.set(4);
				
				if ("True".equals(config.getAttribute("Saturday")))
					this.dayOfWeek.set(5);
				
				if ("True".equals(config.getAttribute("Sunday")))
					this.dayOfWeek.set(6);
			}
			
			// if none set then default to all
			if (this.dayOfWeek.cardinality() == 0)
				this.dayOfWeek.set(0, 6);

			this.dailySchedule.init(config);
		}

		@Override
		public boolean checkDate(CheckInfo ci) {
			if (this.dayOfWeek.get(ci.getDayOfWeek() - 1)) 
				return true;
			
			return false;
		}
	}
}