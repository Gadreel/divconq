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

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import divconq.lang.CoreLocalTime;
import divconq.util.TimeUtil;
import divconq.xml.XElement;

public class DayWindow {
	protected DayWindow parent = null;
	protected BitSet excluded = new BitSet(1440);
	protected BitSet included = new BitSet(1440);
	protected boolean useDefaultWindow = true;
	
	public void setParent(DayWindow v) {
		this.parent = v;
	}
	
	public DayWindow getParent() {
		return this.parent;
	}
	
	public void init(XElement config) {
		if (config != null) {
			// get all the exclude windows 
			for (XElement el : config.selectAll("ExcludeWindow")) {
				CoreLocalTime from = TimeUtil.parseCoreLocalTime(el.getAttribute("From"));
				CoreLocalTime to = TimeUtil.parseCoreLocalTime(el.getAttribute("To"));
				
				this.setBits(false, from, to);
			}
			
			// then add all the include windows 
			for (XElement el : config.selectAll("IncludeWindow")) {
				CoreLocalTime from = TimeUtil.parseCoreLocalTime(el.getAttribute("From"));
				CoreLocalTime to = TimeUtil.parseCoreLocalTime(el.getAttribute("To"));
				
				this.setBits(true, from, to);
			}
			
			// True - use default of 24 hours open if there are no windows given
			// False - do not include 24 hour open if there are no windows given
			// Parent - use only the parent (if present) if there are no windows given
			//		serves as a hint to the parser to include parent
			String defaultWindow = config.getAttribute("DefaultWindow"); 
			
			if ("False".equals(defaultWindow) || "Parent".equals(defaultWindow)) 
				this.useDefaultWindow = false;
		}
		
		// if using default window and no values were found then fill all as though included
		if (this.useDefaultWindow &&  (this.included.cardinality() == 0)) {
			this.setBits(true, null, null);
		}
	}
	
	public boolean excludeAll() {
		if ((this.excluded.cardinality() == 1440) || (this.included.cardinality() == 0)) {
			if (this.parent != null)
				return this.parent.excludeAll();
			
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
	public LocalTime nextTimeOn(DateTime si) {
		// check that not completely excluded
		if (this.excludeAll())
			return null;
		
		int addMinutes = 1;
		LocalTime sil = si.toLocalTime();
		
		// start at the next minute
		int sidx = sil.getHourOfDay() * 60 + sil.getMinuteOfHour() + 1;
		
		// if any minute is open, return it
		for (int i = sidx; i < 1440; i++) { 
			if (this.check(i) == CheckLimitResult.Pass)
				return sil.plusMinutes(addMinutes);
			
			addMinutes++;
		}
		
		// nothing open today
		return null;
	}

	public CheckLimitResult check(DateTime si) {
		if (this.excludeAll())
			return CheckLimitResult.Fail;
		
		LocalTime sil = si.toLocalTime();			
		return this.check(sil.getHourOfDay() * 60 + sil.getMinuteOfHour());
	}
		
	// our setting take preference over the parents
	public CheckLimitResult check(int idx) {			
		if (this.excluded.get(idx))
			return CheckLimitResult.Fail;
		
		if (this.included.get(idx))
			return CheckLimitResult.Pass;
		
		if (this.parent != null)
			return this.parent.check(idx);			
		
		return CheckLimitResult.NA;
	}
	
	public void setBits(boolean include, CoreLocalTime from, CoreLocalTime to) {
		int fidx = (from == null) ? 0 : from.getMinuteStamp();
		
		if (fidx < 0)
			fidx = 0;
		
		if (fidx > 1439)
			return;
		
		int tidx = (to == null) ? 1440 : to.getMinuteStamp();
		
		if (tidx <= 1)
			return;
		
		if (tidx > 1440)
			tidx = 1440;
		
		if (include)
			this.included.set(fidx, tidx);
		else
			this.excluded.set(fidx, tidx);
	}
}