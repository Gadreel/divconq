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
import divconq.scheduler.limit.MonthdayKind;
import divconq.util.StringUtil;
import divconq.util.TimeUtil;
import divconq.xml.XElement;

public class MonthHelper extends CommonHelper {
	protected List<MonthEntry> months = new ArrayList<MonthEntry>();
	
	// first time this code was run since the module/server was started
	// requires special check for any missed runs
	protected boolean firstCall = true;
	
	@Override
	public void init(CommonSchedule schedule, XElement config) {
		if (config != null) {
			for (XElement el : config.selectAll("Months")) {
				MonthEntry entry = new MonthEntry();
				entry.init(el);
				this.months.add(entry);
			}
		}
		
		if (this.months.size() == 0) {
			MonthEntry entry = new MonthEntry();
			entry.init(null);
			this.months.add(entry);
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
					this.last = new DateTime(1970, 1, 1, 0, 0, 0, 0);		// start with deep past
					
				for (MonthEntry we : this.months) {
					if (we.checkStartupNeedsRun()) {		
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
			for (MonthEntry we : this.months) {
				MonthdayEntry me = we.getApplicable(ci);
				
				if (me != null) {
					ScheduleEntry se = me.dailySchedule.next(ci.getWhen());
					
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
						se = me.dailySchedule.next(tlast);
					}
					
					break;
				}
			}
			
			ci.incrementDay();
		}
	}

	class MonthEntry implements IDateChecker {
		protected List<MonthdayEntry> monthly = new ArrayList<MonthdayEntry>();
		// (0 = jan, 11 = dec)
		protected BitSet monthOfYear = new BitSet(12);
		
		public void init(XElement config) {
			if (config != null) {
				if ("True".equals(config.getAttribute("All")))
					this.monthOfYear.set(0, 11);
				
				if ("True".equals(config.getAttribute("January")))
					this.monthOfYear.set(0);
				
				if ("True".equals(config.getAttribute("February")))
					this.monthOfYear.set(1);
				
				if ("True".equals(config.getAttribute("March")))
					this.monthOfYear.set(2);
				
				if ("True".equals(config.getAttribute("April")))
					this.monthOfYear.set(3);
				
				if ("True".equals(config.getAttribute("May")))
					this.monthOfYear.set(4);
				
				if ("True".equals(config.getAttribute("June")))
					this.monthOfYear.set(5);
				
				if ("True".equals(config.getAttribute("July")))
					this.monthOfYear.set(6);
				
				if ("True".equals(config.getAttribute("August")))
					this.monthOfYear.set(7);
				
				if ("True".equals(config.getAttribute("September")))
					this.monthOfYear.set(8);
				
				if ("True".equals(config.getAttribute("October")))
					this.monthOfYear.set(9);
				
				if ("True".equals(config.getAttribute("November")))
					this.monthOfYear.set(10);
				
				if ("True".equals(config.getAttribute("December")))
					this.monthOfYear.set(11);
				
				String[] slots = { "Monthday", "First", "Second", "Third", "Fourth", "Last" };
				
				for (String slot : slots)			
					for (XElement el : config.selectAll(slot)) {
						MonthdayEntry ww = new MonthdayEntry();
						ww.init(el);
						this.monthly.add(ww);
					}
			}
			
			// if none set, then default to all
			if (monthOfYear.cardinality() == 0)
				this.monthOfYear.set(0, 11);
			
			if (this.monthly.size() == 0) { 
				MonthdayEntry ww = new MonthdayEntry();
				ww.init(null);
				this.monthly.add(ww);
			}
		}		

		// the meaning of this method is off a little from expected
		// normally this should only check monthOfYear and ignore the monthdays
		// but for the checkStartupNeedsRun this needs to check the monthdays
		// lets see if any problem come up
		@Override
		public boolean checkDate(CheckInfo ci) {
			if (this.monthOfYear.get(ci.getMonthOfYear() - 1)) 
				for (MonthdayEntry w : this.monthly) 
					if (w.checkDate(ci))
						return true;
			
			return false;
		}

		public MonthdayEntry getApplicable(CheckInfo ci) {
			if (this.monthOfYear.get(ci.getMonthOfYear() - 1)) 
				for (MonthdayEntry w : this.monthly) 
					if (w.checkDate(ci))
						return w;
			
			return null;
		}
		
		public boolean checkStartupNeedsRun() {
			for (MonthdayEntry w : this.monthly) 
				if (MonthHelper.this.checkStartupNeedsRun(w.dailySchedule, this)) 
					return true;
			
			return false;
		}
	}
	
	class MonthdayEntry implements IDateChecker {
		ScheduleList dailySchedule = new ScheduleList();		
		// (0 = monday, 6 = sunday)
		BitSet dayOfWeek = new BitSet(7);
		
		protected MonthdayKind kind = MonthdayKind.Set;
		
		// if type SET then look here for the listed days 
		protected BitSet dayOfMonth = new BitSet(31);
		// does the set include "Last"
		protected boolean isLastDayOfMonth = false;		
		
		public void init(XElement config) {
			if (config != null) {
				String kind = config.getName();
				
				if ("First".equals(kind))
					this.kind = MonthdayKind.First;
				else if ("Second".equals(kind))
					this.kind = MonthdayKind.Second;
				else if ("Third".equals(kind))
					this.kind = MonthdayKind.Third;
				else if ("Fourth".equals(kind))
					this.kind = MonthdayKind.Fourth;
				else if ("Last".equals(kind))
					this.kind = MonthdayKind.Last;
				
				if (this.kind != MonthdayKind.Set) {
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
					
					// if none set then default to all
					if (this.dayOfWeek.cardinality() == 0)
						this.dayOfWeek.set(0, 6);
				}
				else {
					String list = config.getAttribute("List");
					
					if (StringUtil.isEmpty(list) || "All".equals(list)) {
						this.isLastDayOfMonth = true;
						this.dayOfMonth.set(0, 30);
					}
					else {
						String[] parts = list.split(",");
						
						for (String p : parts) {
							if (p.equals("Last")) {
								this.isLastDayOfMonth = true;
								break;
							}
							
							int idx = (int)StringUtil.parseInt(p, 0) - 1;
							
							if (idx > -1)
								this.dayOfMonth.set(idx);
						}
					}
				}
			}
			
			if ((this.kind == MonthdayKind.Set) && (this.dayOfMonth.cardinality() == 0)) {
				this.isLastDayOfMonth = true;
				this.dayOfMonth.set(0, 30);
			}
			
			this.dailySchedule.init(config);
		}

		@Override
		public boolean checkDate(CheckInfo si) {
			if (this.kind == MonthdayKind.Set) {
				if (si.isLastDay() && this.isLastDayOfMonth)
					return true;
				else if (this.dayOfMonth.get(si.getDayOfMonth() - 1))
					return true;
			}
			else if (this.dayOfWeek.get(si.getDayOfWeek() - 1)) { 
				if ((this.kind == MonthdayKind.First) && (si.getMonthPlacement() == 1))  
					return true;
				else if ((this.kind == MonthdayKind.Second) && (si.getMonthPlacement() == 2))  
					return true;
				else if ((this.kind == MonthdayKind.Third) && (si.getMonthPlacement() == 3))  
					return true;
				else if ((this.kind == MonthdayKind.Fourth) && (si.getMonthPlacement() == 4))  
					return true;
				else if ((this.kind == MonthdayKind.Last) && (si.isLastPlacement())) 
					return true;
			}
			
			return false;
		}
	}
}