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
package divconq.tasks.test;

import divconq.bus.IService;
import divconq.bus.Message;
import divconq.hub.Hub;
import divconq.lang.FuncResult;
import divconq.lang.OperationResult;
import divconq.mod.ExtensionBase;
import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;
import divconq.work.Task;
import divconq.work.TaskRun;

/*
 */

public class Service extends ExtensionBase implements IService {
	@Override
	public void start(OperationResult log) {
		super.start(log);
		
		/*
		Task task = ScriptFactory.createNccTestInsertTask();
		
		Hub.instance.getWorkQueue().submit(task);
		*/

		/*
		Task task = ScriptFactory.createSlowLoopTask(65);
		
		Hub.instance.getWorkQueue().submit(task);
		*/
	}
	
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");

		if ("Tests".equals(feature)) {
			if ("LoopSleepScript".equals(op)) {
				RecordStruct rec = msg.getFieldAsRecord("Body");
				
				long loops = rec.getFieldAsInteger("LoopCount", 100);
				
				Task task = ScriptFactory.createSlowLoopTask((int) loops);
				
				if (task == null) {					
					request.error(1, "Unable to create task.");
					request.complete();
					return;
				}
				
				FuncResult<String> res = Hub.instance.getWorkQueue().submit(task);
					
				if (res.hasErrors()) {
					request.copyMessages(res);
					request.complete();
					return;
				}
				
				request.setResult(new RecordStruct(
						new FieldStruct("TaskId", task.getId()), 
						new FieldStruct("WorkId", res.getResult())
				));
				request.complete();
				return;
			}
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
}
