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

import divconq.lang.FuncResult;
import divconq.lang.OperationResult;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.xml.XElement;

public interface IQueueDriver {
	void init(OperationResult or, XElement config);	
	void start(OperationResult or);	
	void stop(OperationResult or);	
	
	// this identity will only ever run once, so identity must be completely unique
	FuncResult<String> reserveUniqueWork(String taskidentity);
	// this identity indicates that common task needs to be run, will not reserve if currently in queue
	FuncResult<String> reserveCurrentWork(String taskidentity);
	FuncResult<String> submit(Task info);
	
	FuncResult<ListStruct> findPotentialClaims(String pool, int howmanymax);
	FuncResult<RecordStruct> makeClaim(RecordStruct info);
	OperationResult updateClaim(Task info);
	
	FuncResult<Task> loadWork(RecordStruct info);
	FuncResult<String> startWork(String workid);
	OperationResult endWork(TaskRun task);
	OperationResult trackWork(TaskRun task, boolean ended);
	
	ListStruct list();
	RecordStruct status(String taskid, String workid);
}
