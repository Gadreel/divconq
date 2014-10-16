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
package divconq.lang;

public class CoreLocalTime {
	private int hour = 0;
	private int minute = 0;
	private int second = 0;
	private int millSec = 0;
	
	public CoreLocalTime(int h, int m, int s, int i) {
		this.hour = h;
		this.minute = m;
		this.second = s;
		this.millSec = i;
	}

	public int getMinuteStamp() {
		return (this.hour * 60) + this.minute;
	}

	public long getStamp() {
		return (this.hour * 36000000) + (this.minute * 60000) + (this.minute * 1000) + this.millSec;
	}
	
	public void setMillSec(int millSec) {
		this.millSec = millSec;
	}
	
	public int getMillSec() {
		return this.millSec;
	}
	
	public void setSecond(int second) {
		this.second = second;
	}
	
	public int getSecond() {
		return this.second;
	}
	
	public void setMinute(int minute) {
		this.minute = minute;
	}
	
	public int getMinute() {
		return this.minute;
	}
	
	public void setHour(int hour) {
		this.hour = hour;
	}
	
	public int getHour() {
		return this.hour;
	}

	public boolean isBefore(CoreLocalTime other) {
		if (other == null)
			return false;
		
		return (this.getStamp() < other.getStamp());
	}
}
