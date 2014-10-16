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
import divconq.struct.RecordStruct;
import divconq.work.TaskRun;

/**
 * 
 * @author Andy
 *
 */
abstract public class WrappedTaskObserver extends TaskObserver {
	protected TaskRun or = null;
	
	public WrappedTaskObserver(TaskRun run) {
		this.or = run;
	}

	@Override
	public void log(OperationResult or, RecordStruct entry) {
		this.or.log(entry);
	}

	@Override
	public void boundary(OperationResult or, String... tags) {
		this.or.boundary(tags);
	}

	@Override
	public void step(OperationResult or, int num, int of, String name) {
		// should not influence higher up 
	}

	@Override
	public void progress(OperationResult or, String msg) {
		this.or.setProgressMessage(msg);
	}

	@Override
	public void amount(OperationResult or, int v) {
		this.or.setAmountCompleted(v);
	}

	@Override
	public void prep(TaskRun or) {
		// should not influence higher up 
	}

	@Override
	public void start(TaskRun or) {
		// should not influence higher up 
	}
	
	@Override
	public void completed(TaskRun or) {
		// should not influence higher up 
	}

	@Override
	public void stop(TaskRun or) {
		// should not influence higher up 
	}
}
