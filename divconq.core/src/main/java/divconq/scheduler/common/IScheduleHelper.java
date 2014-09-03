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

import divconq.scheduler.limit.LimitHelper;
import divconq.xml.XElement;

public interface IScheduleHelper {
	void init(CommonSchedule schedule, XElement config);
	void setLimits(LimitHelper limits);   // limits for the helper to check
	void setLast(DateTime last);		// set only once
	DateTime next();					// call-able multiple times, each time gets the next
}
