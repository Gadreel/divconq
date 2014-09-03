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

public interface ICallbackObserver extends IOperationObserver {
	// this code must not be async or
	// if it has to be then it must start another task to complete that work
	void completed(OperationCallback callback);

}
