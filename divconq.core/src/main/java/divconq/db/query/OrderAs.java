/* ************************************************************************
#
#  DivConq
#
#  http://divconq.com/
#
#  Copyright:
#    Copyright 2012 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package divconq.db.query;

public enum OrderAs {
	Ascending(0),
	Descending(1);

	protected int code = 0;
	
	private OrderAs(int code) {
		this.code = code;
	}
	
	public String getCode() {
		if (this.code == 1)
			return "Desc";
		
		return "";		
	}
}
