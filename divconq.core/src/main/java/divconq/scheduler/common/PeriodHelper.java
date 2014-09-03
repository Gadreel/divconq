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
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;

import divconq.util.StringUtil;
import divconq.xml.XElement;

/*
 *  // use ISO periods, e.g. PT2H30M10S
 *  // used for intra-daily mostly, but can be any
 *  <Period Value="n"  />   
 */
public class PeriodHelper extends CommonHelper {
	protected Period period = null;
	
	// first time this code was run since the module/server was started
	// requires special check for any missed runs
	protected boolean firstCall = true;
	
	@Override
	public void init(CommonSchedule schedule, XElement config) {
		super.init(schedule, config);
		
		if (config == null)
			return;
		
		String period = config.getAttribute("Value");
		
		if (!StringUtil.isEmpty(period))
			try {
				this.period = ISOPeriodFormat.standard().parsePeriod(period);
			}
			catch (Exception x) {
				// TODO log
			}
	}
	
	// return null to indicate this can never run again
	@Override
	public DateTime next() {
		if (this.firstCall) {
			this.firstCall = false;
			
			if (this.limits.isEnded()) {
				this.last = null;
				return null;
			}
			
			// if there was no past runs, do not try to find them
			// or if last run was more than one period old, then run asap
			if ((this.last == null) || (this.last.plus(this.period.toStandardDuration()).isBeforeNow())) {
				this.last = this.limits.nextAllowedRun();
				return this.last;
			}
		}
		
		if (this.last == null)
			return null;
	
		this.last = this.last.plus(this.period);
		
		// we cannot schedule again, the schedule is expired
		if (this.limits.isEnded(this.last))
			return null;
		
		// can we run at the suggested time?
		if (this.limits.checkForRun(this.last)) 
			return this.last;

		this.last = this.limits.nextAllowedRunAfter(this.last);
		return this.last;
	}
}
