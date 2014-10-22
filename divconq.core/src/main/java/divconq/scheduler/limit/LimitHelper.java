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
package divconq.scheduler.limit;


import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;

import divconq.hub.Hub;
import divconq.util.StringUtil;
import divconq.util.TimeUtil;
import divconq.xml.XElement;

// artificially limit expiration at 10 years, if none present
// server should be restarted at least once every 10 years :)
public class LimitHelper {
	protected DayWindow dailyWindow = new DayWindow();
	protected DateTime validFrom = null;
	protected DateTime validTo = null;
	protected DateTimeZone zone = null;
	
	// use only for next/check/blocked/open - do not inherit init/start/end
	protected LimitHelper parent = null; 
	
	protected List<MonthWindow> monthly = new ArrayList<MonthWindow>();
	
	protected List<WeekdayWindow> weekly = new ArrayList<WeekdayWindow>();
	
	public DayWindow getDailyWindow() {
		return this.dailyWindow;
	}
	
	/*
	 *			<Limits 
	 *				LinkBatch="None,Small,Medium,Large"   - include limits defined at server level (batch processing)
	 *				DefaultWindow="T/F"			- by default you have 24 hours enabled
					ValidFrom="iso-date-time"   - schedule only before/after these time 
					ValidTo="iso-date-time"
					TimeZone="name"				- zone to apply to the limits
	 *			>  			
	 *  	
	 *  			// one or more windows during which it is ok to run the scheduled work
	 *  			// defaults to beginning of day to end of day, no matter 
	 *  			<IncludeWindow From="00:00" To="24:00" />
	 *  
	 *  			<ExcludeWindow From="04:15" To="04:17" />
	 *  
	 *				<Weekdays Monday="T/F" Tuesday="n" ... All="T/F" >
	 *					// if exclude is not present, then assume entire day
	 *					<ExcludeWindow From="" To="" />
	 *				</Weekdays>
	 *
	 *  			<Months January="T/F" ... >
	 *  				<First Monday="T/F" Tuesday="n" ... All="T/F" >
	 *						<ExcludeWindow From="" To="" />
	 *  				</First>
	 *  				<Second Monday="T/F" Tuesday="n" ... All="T/F" >
	 *						<ExcludeWindow From="" To="" />
	 *  				</Second>
	 *  				... etc, or ...
	 *  				<Monthday List="N,N,N,Last"> 
	 *						<ExcludeWindow From="" To="" />
	 *  				</Monthday> 
	 *  			</Months>
	 *			</Limits>
	 * 
	 * @param config
	 */
	public void init(XElement config) {
		if (config != null) {
			String zone = config.getAttribute("TimeZone");
			
			if (StringUtil.isNotEmpty(zone))
				this.zone = DateTimeZone.forID(zone);
			
			String from = config.getAttribute("ValidFrom");
			
			if (!StringUtil.isEmpty(from)) {
				this.validFrom = TimeUtil.parseDateTime(from);
				
				// TODO not sure about this - parsing of ISO string should not be circumvented?
				if (this.zone != null)
					this.validFrom = new DateTime(this.validFrom, this.zone);
			}
			
			String to = config.getAttribute("ValidTo");
			
			if (!StringUtil.isEmpty(to)) {
				this.validTo = TimeUtil.parseDateTime(to);
				
				// TODO not sure about this - parsing of ISO string should not be circumvented?
				if (this.zone != null)
					this.validTo = new DateTime(this.validTo, this.zone);
			}
			
			// default to 10 years from now
			if (this.validTo == null)
				this.validTo = new DateTime().plusYears(10);
			
			for (XElement el : config.selectAll("Months")) {
				MonthWindow ww = new MonthWindow();
				ww.init(this, el);
				this.monthly.add(ww);
			}
			
			for (XElement el : config.selectAll("Weekdays")) {
				WeekdayWindow ww = new WeekdayWindow();
				ww.init(this, el);
				this.weekly.add(ww);
			}
			
			this.dailyWindow.init(config);
			
			String batch = config.getAttribute("LinkBatch");
			
			if (StringUtil.isNotEmpty(batch)) 
				this.parent = Hub.instance.getScheduler().getBatch(batch);
		}
		else {
			this.dailyWindow.init(null);
		}
		
		if (this.parent != null) {
			// if parent (batch) then we only use our daily overrides, 
			// not our month or weekly  
			this.dailyWindow.setParent(this.parent.getDailyWindow());
		}
	}

	// return true if instant can run within a window
	// return false if instant is in the past
	public boolean checkForRun(DateTime v) {
		if (this.zone != null)
			v = new DateTime(v, this.zone);
		
		// if this time was ended at mid or before midnight then entire day is blocked
		if (this.isEnded(v))
			return false;
		
		// if this time was not started by the end of the then entire day is blocked
		if (!this.isStarted(v))
			return false;
		
		// runs must also be now (recent) or future
		if (v.plusMinutes(5).isBeforeNow())
			return false;
		
		if (this.parent != null) 
			return this.parent.checkForRun(v);
		
		CheckInfo ci = new CheckInfo();
		ci.setWhen(v);
		
		// if there are any months, those take precedence over other
		if (this.monthly.size() > 0) {
			for (MonthWindow ww : this.monthly)
				if (ww.appliesTo(ci)) 
					return (ww.check(ci) == CheckLimitResult.Pass);
			
			return false;
		}		
		// if there are any weeks, those take precedence over daily
		else if (this.weekly.size() > 0) {
			for (WeekdayWindow ww : this.weekly)
				if (ww.appliesTo(ci)) 
					return (ww.check(ci) == CheckLimitResult.Pass);
			
			return false;
		}
		
		return (this.dailyWindow.check(v) == CheckLimitResult.Pass);
	}

	// return the start of the next available window for running
	// from "now"
	public DateTime nextAllowedRun() {
		return this.nextAllowedRunAfter(new DateTime().minusMinutes(1));
	}

	// return the start of the next available window for running (must always be after or equal to now)
	public DateTime nextAllowedRunAfter(DateTime lin) {
		if (this.zone != null)
			lin = new DateTime(lin, this.zone);
		
		// cannot run before now - 2 minutes
		if (lin.plusMinutes(5).isBeforeNow())
			lin = new DateTime().minusMinutes(1);  // start back one minute so we can start on time
		
		// cannot run again
		if (this.isEnded(lin))
			return null;
		
		// must start at least at "from"
		if (!this.isStarted(lin))
			lin = this.validFrom;
		
		if (this.parent != null) 
			return this.parent.nextAllowedRunAfter(lin);
		
		CheckInfo ci = new CheckInfo();
		ci.setWhen(lin);
		
		LocalTime nt = null;
		
		// move forward 1 day at a time till we find a date that has an opening
		while (true) {
			// if there are any months, those take precedence over other
			if (this.monthly.size() > 0) {
				for (MonthWindow ww : this.monthly)
					if (ww.appliesTo(ci)) {
						nt = ww.nextTimeOn(ci);
						break;
					}
			}
			// if there are any weeks, those take precedence over daily
			else if (this.weekly.size() > 0) {
				for (WeekdayWindow ww : this.weekly) 
					if (ww.appliesTo(ci)) {
						nt = ww.nextTimeOn(ci);
						break;
					}
			}
			else
				nt = this.dailyWindow.nextTimeOn(ci.getWhen());
			
			if (nt != null)
				break;
			
			ci.incrementDay();
			
			// there is no next allowed
			if (this.isEnded(ci.getWhen()))
				return null;
		}
		
		lin = TimeUtil.withTime(ci.getWhen(), nt);
		
		// there is no next allowed
		if (this.isEnded(lin))
			return null;
		
		return lin;
	}

	public boolean isDateBlocked(DateTime tlast) {
		if (this.zone != null)
			tlast = new DateTime(tlast, this.zone);
		
		// if this time was ended at mid or before midnight then entire day is blocked
		if (this.isEnded(tlast.withTime(0, 0, 0, 0)))
			return true;
		
		// if this time was not started by the end of the then entire day is blocked
		if (!this.isStarted(tlast.withTime(23, 59, 59, 0)))
			return true;
		
		if (this.parent != null) 
			return this.parent.isDateBlocked(tlast);
		
		CheckInfo ci = new CheckInfo();
		ci.setWhen(tlast);

		// if there are any months, those take precedence over other
		if (this.monthly.size() > 0) {
			for (MonthWindow ww : this.monthly)
				if (ww.appliesTo(ci))
					return false;
			
			return true;
		}
		// if there are any weeks, those take precedence over daily		
		else if (this.weekly.size() > 0) {
			// only need to find one window to return false
			for (WeekdayWindow ww : this.weekly)
				if (ww.appliesTo(ci))
					return false;
			
			return true;
		}
		
		return this.dailyWindow.excludeAll();
	}

	// return true if "now" is after valid start date 
	public boolean isStarted() {
		if (this.validFrom != null)
			return !this.validFrom.isAfterNow();		// now is equal or greater than from
		
		return true;
	}
	
	// return true if param is after valid start date 
	public boolean isStarted(DateTime scheduleDate) {
		if (this.validFrom != null)
			return !this.validFrom.isAfter(scheduleDate);		// now is equal or greater than from
		
		return true;
	}

	// return true if "now" is after valid end date 
	public boolean isEnded() {
		if (this.validTo != null)
			return !this.validTo.isAfterNow();			// must be before now (not equal)
		
		return false;
	}

	// return true if param is after valid end date
	public boolean isEnded(DateTime scheduleDate) {
		if (this.validTo != null)
			return !this.validTo.isAfter(scheduleDate);
		
		return false;
	}

	// return the first valid date for this schedule
	public DateTime getFirstDate() {
		return this.validFrom;
	}

	// return the last valid date for this schedule
	public DateTime getLastDate() {
		return this.validTo;
	}
}
