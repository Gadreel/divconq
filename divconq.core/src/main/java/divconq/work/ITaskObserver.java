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

import divconq.lang.IOperationObserver;

public interface ITaskObserver extends IOperationObserver {
	// this code must not be async or
	// if it has to be then it must start another task to complete that work
	
	// also note that prep might be called multiple times - don't write code that can handle only 1 prep call
	void prep(TaskRun or);
	void start(TaskRun or);
	void completed(TaskRun or);
	void stop(TaskRun or);

}
