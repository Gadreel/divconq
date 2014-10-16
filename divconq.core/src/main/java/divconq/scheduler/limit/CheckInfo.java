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

import org.joda.time.DateTime;

import divconq.util.TimeUtil;

public class CheckInfo {
	private DateTime when = null;
	private int dayOfWeek = 0;		// 0 = monday
	private int monthPlacement = 1;   // 1 = first, 2 = second, 3 = third, etc
	private boolean isLastPlacement = false;    // last of this dow in this month	
	private boolean isLastDay = false;    // last of this month	
	private int dayOfMonth = 1;
	private int monthOfYear = 1;
	
	public void incrementDay() {
		this.setWhen(TimeUtil.nextDayAtMidnight(this.when));
	}
	
	public void setWhen(DateTime when) {
		this.when = when;
		
		this.dayOfMonth = when.getDayOfMonth();
		this.dayOfWeek = when.getDayOfWeek();
		this.monthOfYear = when.getMonthOfYear();
		
		this.monthPlacement = ((this.dayOfMonth - 1) / 7) + 1;
		this.isLastPlacement = ((when.dayOfMonth().withMaximumValue().getDayOfMonth() - this.dayOfMonth) < 7);
		this.setLastDay((when.dayOfMonth().withMaximumValue().getDayOfMonth() == this.dayOfMonth));
	}
	
	public DateTime getWhen() {
		return this.when;
	}
	
	public void setDayOfWeek(int dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}
	
	public int getDayOfWeek() {
		return this.dayOfWeek;
	}
	
	public void setMonthPlacement(int monthPlacement) {
		this.monthPlacement = monthPlacement;
	}
	
	public int getMonthPlacement() {
		return this.monthPlacement;
	}
	
	public void setLastPlacement(boolean isLastPlacement) {
		this.isLastPlacement = isLastPlacement;
	}
	
	public boolean isLastPlacement() {
		return this.isLastPlacement;
	}
	
	public void setDayOfMonth(int dayOfMonth) {
		this.dayOfMonth = dayOfMonth;
	}
	
	public int getDayOfMonth() {
		return this.dayOfMonth;
	}
	
	public void setMonthOfYear(int monthOfYear) {
		this.monthOfYear = monthOfYear;
	}
	
	public int getMonthOfYear() {
		return this.monthOfYear;
	}

	public CheckInfo deepClone() {
		CheckInfo ci = new CheckInfo();
		ci.when = this.when;
		ci.dayOfMonth = this.dayOfMonth;
		ci.dayOfWeek = this.dayOfWeek;
		ci.isLastPlacement = this.isLastPlacement;
		ci.monthOfYear = this.monthOfYear;
		ci.monthPlacement = this.monthPlacement;
		return ci;
	}

	public void setLastDay(boolean isLastDay) {
		this.isLastDay = isLastDay;
	}

	public boolean isLastDay() {
		return isLastDay;
	}
}
