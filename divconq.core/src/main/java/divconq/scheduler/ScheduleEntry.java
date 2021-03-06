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

import java.nio.file.Paths;

import divconq.hub.Hub;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationResult;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;
import divconq.work.ScriptWork;
import divconq.work.Task;

public class ScheduleEntry {
	static public enum ScheduleArea {
		Squad,
		Team,
		Local
	}
	
	protected RecordStruct params = null;
	protected Task task = null;
	protected String provider = null;
	protected ISchedule schedule = null;
	protected ScheduleArea area = null;
	protected String scheduleId = null;
	protected String title = null;
	
	public ISchedule getSchedule() {
		return this.schedule;
	}
	
	public void setSchedule(ISchedule schedule) {
		this.schedule = schedule;
	}
	
	public String getScheduleId() {
		return this.scheduleId;
	}
	
	public void setScheduleId(String scheduleId) {
		this.scheduleId = scheduleId;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public Task getTask() {
		return this.task;
	}
	
	public void setTask(Task task) {
		this.task = task;
	}
	
	public String getProvider() {
		return this.provider;
	}
	
	public void setProvider(String provider) {
		this.provider = provider;
	}
	
	public ScheduleArea getArea() {
		return this.area;
	}
	
	public void setArea(ScheduleArea area) {
		this.area = area;
	}
	
	public RecordStruct getParams() {
		return this.params;
	}
	
	public void setParams(RecordStruct params) {
		this.params = params;
	}

	public void prepareTask(OperationResult or) {
		if (StringUtil.isNotEmpty(this.provider)) {
			if (this.provider.startsWith("@")) {
				ITaskProvider prov = (ITaskProvider) Hub.instance.getInstance(this.provider.substring(1));
				
				if (prov != null) {
					FuncResult<Task> res = prov.getTask(this);
					
					this.task = res.getResult();
				}
				else
					or.error("Could not load task provider for schedule");
			}		
			else if (this.provider.startsWith("$")) {
				Task t = new Task()
					.withTitle(this.title)
					.withParams(this.params)			
					.withRootContext();
				
				if (ScriptWork.addScript(t, Paths.get(this.provider.substring(1)))) 
					this.task = t;
				else
					or.error("Error compiling script");
			}		
		}
		
		if (this.task == null)
			or.error("No task defined for schedule");
	}
	
	public void submit(OperationResult or) {
		// first prepare the task, which relies on
		this.prepareTask(or);
		
		if (or.hasErrors())
			return;
		
		this.task.prep();
		
		RecordStruct params = this.task.getParams();
		
		if (params == null) {
			params = new RecordStruct();
			this.task.withParams(params);
		}
		
		params.setField("_ScheduleId", this.scheduleId);
		params.setField("_ScheduleHints", this.schedule.getHints());
		
		// TODO figure out how to get correct task run info to the scheduler so it can reschedule correctly
		// add a task observer that, when run is done, provides the scheduler with the run for analysis and continuation scheduling
		
		if (this.area == ScheduleArea.Local) {
			Hub.instance.getWorkPool().submit(this.task);
			return;
		}
		
		if (this.area == ScheduleArea.Squad) {
			Hub.instance.getWorkQueue().reserveUniqueAndSubmit(this.task);
			
			// don't error further even if we could not reserve
			return;
		}
		
		or.error("Unable to submit scheduled task");
	}
}
