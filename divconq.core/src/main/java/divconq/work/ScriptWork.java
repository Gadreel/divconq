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

import java.nio.file.Path;

import divconq.hub.Hub;
import divconq.lang.op.OperationResult;
import divconq.script.Activity;
import divconq.struct.RecordStruct;
import divconq.util.IOUtil;
import divconq.util.StringUtil;

public class ScriptWork extends Activity {
	// TODO return func result - parse XML and add title, throttle, etc to Task
	// return error if to valid XML, though compile errors will have to be during run
	static public boolean addScript(Task info, Path source) {
		return ScriptWork.addScript(info, IOUtil.readEntireFile(source).getResult());
	}
	
	// TODO keep just file path normally, option for source  _ScriptPath or _Script
	static public boolean addScript(Task info, CharSequence source) {
		if (StringUtil.isEmpty(source))
			return false;
		
		RecordStruct params = info.getParams();
		
		if (params == null) {
			params = new RecordStruct();
			info.withParams(params);
		}
		
		params.setField("_Script", source);
		
		info.withWork(ScriptWork.class);
		info.withThrottleIfEmpty(10);		// increase default for scripts
		
		return true;
	}
	
	@Override
	public void run(TaskRun scriptrun) {
		// if already initialized then go right into run
		if (this.stack != null) {
			super.run(scriptrun);
			return;
		}
		
		// initialize the script and stack
		try {
			scriptrun.infoTr(151, scriptrun.getTask().getId());
			
			RecordStruct args = scriptrun.getTask().getParams();
			
			if (args == null) {
				scriptrun.errorTr(148, scriptrun.getTask().getId(), "Missing task parameters");
				scriptrun.complete();
				return;
			}
			
			String source = args.getFieldAsString("_Script");
			
			if (StringUtil.isEmpty(source)) {
				scriptrun.errorTr(148, scriptrun.getTask().getId(), "Missing task script parameter");
				scriptrun.complete();
				return;
			}
	
			OperationResult compilelog = this.compile(source);
			
			if (compilelog.hasErrors()) {
				// TODO replace with hub events
				Hub.instance.getWorkQueue().sendAlert(149, scriptrun.getTask().getId(), compilelog.getMessage());
				
				scriptrun.errorTr(149, scriptrun.getTask().getId(), compilelog.getMessage());
				scriptrun.complete();
				return;
			}
		}
		catch (Exception x) {
			// TODO replace with hub events
			Hub.instance.getWorkQueue().sendAlert(150, scriptrun.getTask().getId(), x);
			
			scriptrun.errorTr(150, scriptrun.getTask().getId(), x);
			scriptrun.complete();
			return;
		}
		
		// run for first time
		super.run(scriptrun);
	}
}
