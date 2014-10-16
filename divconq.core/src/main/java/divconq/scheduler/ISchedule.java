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

import divconq.struct.RecordStruct;
import divconq.work.ITaskObserver;
import divconq.work.Task;
import divconq.xml.XElement;

public interface ISchedule extends ITaskObserver {
	void init(XElement config);
	Task task();
	void setTask(Task v);
	boolean reschedule();
	long when();
	RecordStruct getHints();
	void cancel();
	boolean isCanceled();
}
