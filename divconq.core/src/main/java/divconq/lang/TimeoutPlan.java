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

public enum TimeoutPlan {
	Regular,
	Long,
	ExtraLong;
	
	public int getSeconds() {
		// 2 minutes is regular    TODO config
		
		if (this == TimeoutPlan.Long) 
			return 300;
		
		if (this == TimeoutPlan.ExtraLong)
			return 1200;
		
		return 120;
	}
}
