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
import divconq.struct.Struct;
import divconq.work.ITaskObserver;
import divconq.work.TaskRun;

// attributes saved in database
public class DurableTaskObserver extends RecordStruct implements ITaskObserver {
	public DurableTaskObserver() {
	}
	
	@Override
	public Struct deepCopy() {
		DurableTaskObserver cp = new DurableTaskObserver();
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	public void log(OperationResult or, RecordStruct entry) {
	}

	@Override
	public void boundary(OperationResult or, String... tags) {
	}

	@Override
	public void step(OperationResult or, int num, int of, String name) {
	}

	@Override
	public void progress(OperationResult or, String msg) {
	}

	@Override
	public void amount(OperationResult or, int v) {
	}

	@Override
	public void prep(TaskRun or) {
		// don't open login file til start		
	}

	@Override
	public void start(TaskRun run) {
	}

	@Override
	public void completed(TaskRun or) {
		// TODO
	}

	@Override
	public void stop(TaskRun or) {
	}
}