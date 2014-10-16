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
import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import divconq.xml.XElement;

// a list of times during the day to run
// this list knows nothing of what days to run or not run, or
// anything about the date of a ScheduleInstant when it works with one
public class ScheduleList {
	protected List<ScheduleEntry> entries = new ArrayList<ScheduleEntry>();
	
	public int size() {
		return this.entries.size();
	}
	
	// next searches only relative to the time of day, not the date
	// return null if no more runs after the param
	public ScheduleEntry next(DateTime at) {
		LocalTime lt = at.toLocalTime();
		
		for (ScheduleEntry se : this.entries)
			if (se.getTime().isAfter(lt))
				return se;
		
		return null;
	}
	
	public boolean anyRequired() {
		for (ScheduleEntry se : this.entries)
			if (se.isRunIfMissed())
				return true;
		
		return false;
	}
	
	public void init(XElement config) {
		if (config != null) {
			for (XElement sel : config.selectAll("Schedule")) {
				ScheduleEntry e = new ScheduleEntry();
				e.init(sel);
				this.entries.add(e);
			}
		}
		
		// default to run at midnight
		if (this.entries.size() == 0) {
			ScheduleEntry e = new ScheduleEntry();
			e.init(null);
			this.entries.add(e);			
		}
		
		Collections.sort(this.entries);
	}
}
