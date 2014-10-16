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

import divconq.lang.OperationObserver;

public class TaskObserver extends OperationObserver implements ITaskObserver {
	@Override
	public void prep(TaskRun or) {
	}

	@Override
	public void start(TaskRun or) {
	}

	@Override
	public void completed(TaskRun or) {
	}

	@Override
	public void stop(TaskRun or) {
	}
}
