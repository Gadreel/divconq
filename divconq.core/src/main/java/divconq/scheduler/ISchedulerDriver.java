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

import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationResult;
import divconq.struct.ListStruct;
import divconq.xml.XElement;

public interface ISchedulerDriver {
	void init(OperationResult or, XElement config);	
	void start(OperationResult or);	
	void stop(OperationResult or);	
	
	FuncResult<ListStruct> loadSchedule();
	FuncResult<ScheduleEntry> loadEntry(String id);
}
