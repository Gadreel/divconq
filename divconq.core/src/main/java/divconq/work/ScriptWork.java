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
import divconq.lang.FuncResult;
import divconq.lang.OperationCallback;
import divconq.lang.OperationResult;
import divconq.script.Activity;
import divconq.script.ActivityManager;
import divconq.script.Script;
import divconq.struct.RecordStruct;
import divconq.util.IOUtil;
import divconq.util.StringUtil;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

public class ScriptWork implements ISmartWork {
	static public boolean addScript(Task info, Path source) {
		return ScriptWork.addScript(info, IOUtil.readEntireFile(source).getResult());
	}
	
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
		
		return true;
	}
	
	// members
	protected Activity act = null;
	
	@Override
	public void run(final TaskRun scriptrun) {
		scriptrun.infoTr(151, scriptrun.getTask().getId());
		
		RecordStruct params = scriptrun.getTask().getParams();
		
		if (params == null) {
			scriptrun.errorTr(148, scriptrun.getTask().getId(), "Missing task parameters");
			scriptrun.complete();
			return;
		}
		
		String source = params.getFieldAsString("_Script");
		
		if (StringUtil.isEmpty(source)) {
			scriptrun.errorTr(148, scriptrun.getTask().getId(), "Missing task script parameter");
			scriptrun.complete();
			return;
		}
		
		Script srpt = null;
		
		try {
			ActivityManager man = Hub.instance.getActivityManager();
			
			FuncResult<XElement> xres = XmlReader.parse(source, true); 
			
			if (xres.hasErrors()) {
				Hub.instance.getWorkQueue().sendAlert(148, scriptrun.getTask().getId(), xres.getMessage());					
				
				scriptrun.errorTr(148, scriptrun.getTask().getId(), xres.getMessage());
				scriptrun.complete();
				return;
			}
			
			XElement script = xres.getResult(); 
	
			srpt = new Script(man);
			OperationResult compilelog = srpt.compile(script);
			
			if (compilelog.hasErrors()) {
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
		
		this.act = new Activity(srpt, params);
		
		this.act.setLog(scriptrun);
		
		this.act.run(new OperationCallback() {				
			@Override
			public void callback() {
				scriptrun.complete();
			}
		}, scriptrun.getTask());		
	}

	@Override
	public void cancel(TaskRun run) {
		System.out.println("script work cancelled");
		
		if (this.act != null)
			this.act.cancel();
	}

	@Override
	public void completed(TaskRun run) {
		
	}	
}
