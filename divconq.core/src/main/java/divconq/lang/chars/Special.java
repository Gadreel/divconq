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
package divconq.lang.chars;

public enum Special {
	Tab(9),
	LineFeed(10),
	VTab(11),
	FormFeed(12),
	CarriageReturn(13),
	
	StartRec(30),
	EndRec(31),
	StartList(28),
	EndList(29),
	Field(26),
	Scalar(25),
	End(23);
	
	protected int code = 0;
	
	private Special(int code) {
		this.code = code;
	}
	
	public int getCode() {
		return this.code;		
	}
}
