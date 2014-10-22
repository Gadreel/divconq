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
import java.util.BitSet;
import java.util.List;

import org.joda.time.LocalTime;

import divconq.xml.XElement;

public class MonthWindow {
	// window to use for this 
	protected List<MonthdayWindow> monthly = new ArrayList<MonthdayWindow>();
	
	// (0 = jan, 11 = dec)
	protected BitSet monthOfYear = new BitSet(12);
	
	public void init(LimitHelper helper, XElement config) {
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
			
			// if none set, then default to all
			if (monthOfYear.cardinality() == 0)
				this.monthOfYear.set(0, 11);
			
			for (XElement el : config.selectAll("Monthday")) {
				MonthdayWindow ww = new MonthdayWindow();
				ww.init(helper, el);
				this.monthly.add(ww);
			}
			
			for (XElement el : config.selectAll("First")) {
				MonthdayWindow ww = new MonthdayWindow();
				ww.init(helper, el);
				this.monthly.add(ww);
			}
			
			for (XElement el : config.selectAll("Second")) {
				MonthdayWindow ww = new MonthdayWindow();
				ww.init(helper, el);
				this.monthly.add(ww);
			}
			
			for (XElement el : config.selectAll("Third")) {
				MonthdayWindow ww = new MonthdayWindow();
				ww.init(helper, el);
				this.monthly.add(ww);
			}
			
			for (XElement el : config.selectAll("Fourth")) {
				MonthdayWindow ww = new MonthdayWindow();
				ww.init(helper, el);
				this.monthly.add(ww);
			}
			
			for (XElement el : config.selectAll("Last")) {
				MonthdayWindow ww = new MonthdayWindow();
				ww.init(helper, el);
				this.monthly.add(ww);
			}
		}
	}
	
	/*
	 * search to see if there are any minutes open for this date after or at the current minute
	 *  
	 * @param si
	 * @return
	 */
	public LocalTime nextTimeOn(CheckInfo si) {
		// pick the first window that applies to our info
		MonthdayWindow mw = this.getApplicable(si);
		
		if (mw != null)
			return mw.nextTimeOn(si);
		
		return null;
	}

	public CheckLimitResult check(CheckInfo si) {
		// pick the first window that applies to our info
		MonthdayWindow mw = this.getApplicable(si);
		
		if (mw != null)
			return mw.check(si);
		
		return CheckLimitResult.NA;
	}

	public boolean isDateBlocked(CheckInfo ci) {
		return !this.appliesTo(ci);
	}

	public MonthdayWindow getApplicable(CheckInfo ci) {
		if (this.monthOfYear.get(ci.getMonthOfYear() - 1)) 
			for (MonthdayWindow w : this.monthly) 
				if (w.appliesTo(ci))
					return w;
		
		return null;
	}
	
	public boolean appliesTo(CheckInfo ci) {
		if (this.monthOfYear.get(ci.getMonthOfYear() - 1)) 
			return true;
		
		return false;
	}
}