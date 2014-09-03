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
package divconq.hub;

public class SysReporter {
	protected String status = null;
	protected long last = 0;
	
	public String getStatus() {
		return this.status;
	}
	
	public long getLast() {
		return this.last;
	}
	
	public void setStatus(String status) {
		this.status = status;
		this.last = System.currentTimeMillis();
	}
}
