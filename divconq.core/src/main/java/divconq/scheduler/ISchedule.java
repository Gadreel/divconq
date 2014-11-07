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
package divconq.scheduler;

import divconq.lang.op.IOperationObserver;
import divconq.struct.RecordStruct;
import divconq.work.Task;
import divconq.xml.XElement;

public interface ISchedule extends IOperationObserver {
	void init(XElement config);
	Task task();
	void setTask(Task v);
	boolean reschedule();
	long when();
	RecordStruct getHints();
	void cancel();
	boolean isCanceled();
}
