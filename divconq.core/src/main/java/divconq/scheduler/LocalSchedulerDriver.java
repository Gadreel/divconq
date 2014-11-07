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

import java.util.HashMap;

import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationResult;
import divconq.scheduler.ISchedulerDriver;
import divconq.scheduler.ScheduleEntry;
import divconq.scheduler.ScheduleEntry.ScheduleArea;
import divconq.session.Session;
import divconq.struct.CompositeParser;
import divconq.struct.CompositeStruct;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class LocalSchedulerDriver implements ISchedulerDriver {
	protected ListStruct schedule = new ListStruct();
	protected HashMap<String, ScheduleEntry> entries = new HashMap<>();
	
	@Override
	public void init(OperationResult or, XElement config) {
		if (config != null) {
			for (XElement task : config.selectAll("Task")) {
				String id = task.getAttribute("Id");
				
				if (StringUtil.isEmpty(id))
					id = Session.nextTaskId();
				
				String title = task.getAttribute("Title");
				
				XElement sched = task.find("CommonSchedule");
				
				if (sched == null)
					sched = task.find("SimpleSchedule");
				
				this.schedule.addItem(
						new RecordStruct( 
								new FieldStruct("Id", id), 
								new FieldStruct("Title", title), 
								new FieldStruct("Schedule", sched)
						)
				);	
				
				ScheduleEntry entry = new ScheduleEntry();
				
				entry.setScheduleId(id);
				entry.setArea(ScheduleArea.Local);
				entry.setTitle(title);
				entry.setProvider("$" + task.getAttribute("Script"));
				
				String params = task.selectFirstText("Params");
				
				if (StringUtil.isNotEmpty(params)) {
					FuncResult<CompositeStruct> pres = CompositeParser.parseJson(params);
					
					if (pres.isNotEmptyResult())
						entry.setParams((RecordStruct) pres.getResult());
				}
				
				this.entries.put(id, entry);
			}
		}
	}

	@Override
	public void start(OperationResult or) {
		or.infoTr(225);
	}

	@Override
	public void stop(OperationResult or) {
		or.infoTr(226);
	}

	@Override
	public FuncResult<ListStruct> loadSchedule() {
		FuncResult<ListStruct> res = new FuncResult<>();
		res.setResult(this.schedule);
		return res;
	}
	
	@Override
	public FuncResult<ScheduleEntry> loadEntry(String id) {
		FuncResult<ScheduleEntry> res = new FuncResult<ScheduleEntry>();

		ScheduleEntry entry = this.entries.get(id);
		
		if (entry == null) 
			res.errorTr(166, id);
		else
			res.setResult(entry);
		
		return res;
	}
}
