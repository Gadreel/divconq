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

import divconq.util.StringUtil;
import divconq.xml.XElement;

public class MonthdayWindow {
	protected MonthdayKind kind = MonthdayKind.Set;
	
	// window to use for this 
	protected DayWindow dailyWindow = new DayWindow();
	
	// if type SET then look here for the listed days 
	protected BitSet dayOfMonth = new BitSet(31);
	// does the set include "Last"
	protected boolean isLastDayOfMonth = false;
	
	// if other types, besides SET, then list here (0 = monday, 6 = sunday)
	protected BitSet dayOfWeek = new BitSet(7);
	
	public void init(LimitHelper helper, XElement config) {
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
		
		// if so, are there any days open
		if (this.kind == MonthdayKind.Set) { 
			if ((this.dayOfMonth.cardinality() == 0) && !this.isLastDayOfMonth)
				return true;
		}
		else {
			if (this.dayOfWeek.cardinality() == 0)
				return true;
		}
		
		return false;
	}

	/*
	 * search to see if there are any minutes open for this date after or at the current minute
	 *  
	 * @param si
	 * @return
	 */
	public LocalTime nextTimeOn(CheckInfo si) {
		if (this.appliesTo(si))
			return this.dailyWindow.nextTimeOn(si.getWhen());				
		
		return null;
	}

	public CheckLimitResult check(CheckInfo si) {
		if (this.excludeAll())
			return CheckLimitResult.Fail;
		
		if (this.appliesTo(si))
			return this.dailyWindow.check(si.getWhen());
		
		return CheckLimitResult.NA;
	}
	
	public boolean isDateBlocked(CheckInfo si) {
		return !this.appliesTo(si);
	}
	
	public boolean appliesTo(CheckInfo si) {
		if (this.excludeAll())
			return false;
	
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