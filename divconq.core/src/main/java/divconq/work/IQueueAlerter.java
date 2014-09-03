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
package divconq.work;

import divconq.lang.OperationResult;
import divconq.xml.XElement;

public interface IQueueAlerter {
	void init(OperationResult or, XElement config);	
	void sendAlert(long code, Object... params);
}
