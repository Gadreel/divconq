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

import org.joda.time.LocalTime;

import divconq.util.TimeUtil;
import divconq.xml.XElement;

/*
 *	<Schedule At="" RunIfMissed="True/False" />
 */
public class ScheduleEntry implements Comparable<ScheduleEntry> {
	protected LocalTime time = null;
	protected boolean runIfMissed = false;
	
	public LocalTime getTime() {
		return this.time;
	}
	
	public boolean isRunIfMissed() {
		return this.runIfMissed;
	}
	
	public void init(XElement config) {
		if (config != null) { 
			if ("True".equals(config.getAttribute("RunIfMissed")))
				this.runIfMissed = true;
		
			this.time = TimeUtil.parseLocalTime(config.getAttribute("At"));

			// scheduling at midnight does not work, but 1 ms after is fine 
			if (this.time.getMillisOfDay() == 0) 
				this.time = this.time.plusMillis(1);
		}
		
		if (this.time == null)
			this.time = new LocalTime();
	}

	@Override
	public int compareTo(ScheduleEntry entry) {
		return this.time.compareTo(entry.getTime());
	}
}
