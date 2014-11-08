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

import java.nio.file.Paths;

import divconq.lang.op.OperationLogger;
import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;
import divconq.work.ScriptWork;
import divconq.work.Task;

public class ScriptFactory {
	static public Task createGreetTask(String greet) {
		Task task = new Task()
			.withTitle("Greetings for: " + greet)
			.withSubContext()
			.withParams(new RecordStruct(
					new FieldStruct("Greet", greet)
				)
			);
		
		ScriptWork.addScript(task, Paths.get("./packages/dcTest/scripts/Greet.dcs.xml"));
		
		return task;
	}
	
	static public Task createSlowGreetTask(String greet) {
		Task task = new Task()
			.withTitle("Greetings for: " + greet)
			.withDefaultLogger()
			.withSubContext()
			.withParams(new RecordStruct(
					new FieldStruct("Greet", greet)
				)
			);
		
		ScriptWork.addScript(task, Paths.get("./packages/dcTest/scripts/SlowGreet.dcs.xml"));
		
		return task;
	}
	
	static public Task createSingTask(String dedicated) {
		// id is good for local or queue use 
		String tid = Task.nextTaskId("12BottlesSong");

		OperationLogger logger = new OperationLogger();
		
		Task task = new Task()
			.withId(tid)
			.withTitle("Singing 12 Bottles of Beer for: " + dedicated)
			.withSubContext()
			.withObserver(logger)
			.withUsesTempFolder(true)
			.withTimeout(30)
			.withParams(new RecordStruct(
					new FieldStruct("DedicatedTo", dedicated)
				)
			);
		
		ScriptWork.addScript(task, Paths.get("./packages/dcTest/scripts/12Bottles.dcs.xml"));
		
		return task;
	}
	
	// deadline must be > 10 to succeed
	static public Task createVerySlowTask(int deadline) {
		Task task = new Task()
			.withTitle("Too Slow Work Test")
			.withRootContext()
			.withDeadline(deadline)		 
			.withDefaultLogger();
		
		ScriptWork.addScript(task, Paths.get("./packages/dcTest/scripts/VerySlowWork.dcs.xml"));
		
		return task;
	}
	
	// deadline must be > 10 to succeed
	static public Task createSlowLoopTask(int loops) {
		Task task = new Task()
			.withTitle("Slow Loop Work Test")
			.withRootContext()
			.withDefaultLogger()
			.withParams(new RecordStruct(new FieldStruct("LoopCount", loops)));
		
		ScriptWork.addScript(task, Paths.get("./packages/dcTest/scripts/SlowLoopWork.dcs.xml"));
		
		return task;
	}

	public static Task createNccTestInsertTask() {
		Task task = new Task()
			.withTitle("NCC Test Insert")
			.withRootContext()
			.withDefaultLogger();
		
		ScriptWork.addScript(task, Paths.get("./packages/dcTest/scripts/NCCTestInsert.dcs.xml"));
		
		return task;
	}
}
