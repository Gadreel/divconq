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
package divconq.lang.op;


// if you are listening to work, your efforts in "completed" will be in the work context, not yours - be sure to respect that
public interface IOperationObserver {
	void fireEvent(OperationEvent event, OperationContext target, Object detail);
}
