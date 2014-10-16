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

import divconq.hub.Hub;
import divconq.lang.OperationResult;
import divconq.struct.RecordStruct;
import divconq.work.Task;
import divconq.work.TaskRun;
import divconq.xml.XElement;

public class SimpleSchedule implements ISchedule {
	protected Task task = null;		
	protected boolean repeat = false;
	protected long nextrunat = 0;
	protected int every = 0;
	protected RecordStruct hints = new RecordStruct();
	protected boolean canceled = false;
	
	@Override
	public void setTask(Task v) {
		this.task = v;
	}
	
	public SimpleSchedule() {
		
	}
	
	public SimpleSchedule(Task run, long at, int repeatevery) {
		this.task = run;
		this.nextrunat = at;
		
		if (repeatevery > 0) {
			this.repeat = true;
			this.every = repeatevery;
		}
	}
	
	public void init(XElement config) {
		// TODO load a SimpleSchedule element
	}
	
	@Override
	public boolean reschedule() {
		if (!this.repeat || this.canceled)
			return false;
		
		//this.task.reset();	// TODO might be a better place for this?
		this.nextrunat += (this.every * 1000);
		return true;
	}

	@Override
	public long when() {
		return this.nextrunat;
	}

	@Override
	public Task task() {
		return this.task;
	}

	@Override
	public RecordStruct getHints() {
		return this.hints;
	}

	@Override
	public void cancel() {
		this.canceled = true;
		
		// TODO someday optimize by removing from scheduler node list
	}
	
	@Override
	public boolean isCanceled() {
		return this.canceled;
	}

	// these are all for IOperationObserver
	
	@Override
	public void completed(TaskRun or) {
		// remember - we can look at the task for further info when rescheduling
		// run.getResult();
		
		if (this.reschedule()) 
			Hub.instance.getScheduler().addNode(this);
	}

	@Override
	public void log(OperationResult run, RecordStruct entry) {
	}

	@Override
	public void boundary(OperationResult run, String... tags) {
	}

	@Override
	public void step(OperationResult run, int num, int of, String name) {
	}

	@Override
	public void progress(OperationResult run, String msg) {
	}

	@Override
	public void amount(OperationResult run, int v) {
	}

	@Override
	public void prep(TaskRun or) {
	}

	@Override
	public void start(TaskRun or) {
	}

	@Override
	public void stop(TaskRun or) {
	}
}
