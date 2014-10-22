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

import java.util.BitSet;

import org.joda.time.LocalTime;

import divconq.xml.XElement;

public class WeekdayWindow {
	// window to use for this 
	protected DayWindow dailyWindow = new DayWindow();
	
	// if other types, besides SET, then list here (0 = monday, 6 = sunday)
	protected BitSet dayOfWeek = new BitSet(7);
	
	public void init(LimitHelper helper, XElement config) {
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
			
			// if none set then default to all
			if (this.dayOfWeek.cardinality() == 0)
				this.dayOfWeek.set(0, 6);
			
			this.dailyWindow.init(config);
			
			// True - use default of 24 hours open if there are no windows given
			// False - do not include 24 hour open if there are no windows given
			// Parent - use only the parent (if present) if there are no windows given
			//		serves as a hint to the parser to include parent
			String defaultWindow = config.getAttribute("DefaultWindow"); 
			
			if (("Parent".equals(defaultWindow)) && (helper != null))
				this.dailyWindow.setParent(helper.getDailyWindow());
		}
	}
	
	public boolean excludeAll() {
		// do we have any open window during the day?
		if (this.dailyWindow.excludeAll())
			return true;
		
		if (this.dayOfWeek.cardinality() == 0)
			return true;
		
		return false;
	}

	/*
	 * search to see if there are any minutes open for this date after or at the current minute
	 *  
	 * @param si
	 * @return
	 */
	public LocalTime nextTimeOn(CheckInfo si) {
		if (this.excludeAll())
			return null;
	
		if (this.dayOfWeek.get(si.getDayOfWeek() - 1)) 
			return this.dailyWindow.nextTimeOn(si.getWhen());
		
		return null;
	}

	public CheckLimitResult check(CheckInfo si) {
		if (this.excludeAll())
			return CheckLimitResult.Fail;
		
		if (this.dayOfWeek.get(si.getDayOfWeek() - 1)) 
				return this.dailyWindow.check(si.getWhen());
		
		return CheckLimitResult.NA;
	}

	public boolean isDateBlocked(CheckInfo ci) {
		return !this.appliesTo(ci);
	}
	
	public boolean appliesTo(CheckInfo ci) {
		if (this.excludeAll())
			return false;
	
		if (this.dayOfWeek.get(ci.getDayOfWeek() - 1)) 
			return true;
		
		return false;
	}
}