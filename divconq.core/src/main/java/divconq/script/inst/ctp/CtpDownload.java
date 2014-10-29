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
package divconq.script.inst.ctp;

import java.nio.file.Path;
import java.nio.file.Paths;

import divconq.api.ApiSession;
import divconq.api.tasks.DownloadFile;
import divconq.hub.Hub;
import divconq.interchange.CommonPath;
import divconq.lang.OperationCallback;
import divconq.script.ExecuteState;
import divconq.script.Instruction;
import divconq.script.StackEntry;
import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;
import divconq.work.Task;

public class CtpDownload extends Instruction {
	@Override
	public void run(final StackEntry stack) {
        String service = stack.stringFromSource("Service");
        
        if (StringUtil.isEmpty(service))
        	service = "dcFileServer";
        
        String fname = stack.stringFromSource("Dest");
        
        if (StringUtil.isEmpty(fname)) {
			stack.setState(ExecuteState.Done);
        	stack.log().error("Missing Dest");
        	stack.resume();
        	return;
        }
        
    	Path dest = null;
    	
    	try {
    		dest = Paths.get(fname);
    	}
    	catch (Exception x) {
			stack.setState(ExecuteState.Done);
        	stack.log().error("Dest error: " + x);
        	stack.resume();
        	return;
    	}
        
        String dname = stack.stringFromSource("Source");
        
        if (StringUtil.isEmpty(dname)) {
			stack.setState(ExecuteState.Done);
        	stack.log().error("Missing Source");
        	stack.resume();
        	return;
        }
    	
    	CommonPath src = null;
    	
    	try {
    		src = new CommonPath(dname);
    	}
    	catch (Exception x) {
			stack.setState(ExecuteState.Done);
        	stack.log().error("Source error: " + x);
        	stack.resume();
        	return;
    	}
        
        Struct ss = stack.refFromSource("Session");
        
        if ((ss == null) || !(ss instanceof ApiSession)) {
			stack.setState(ExecuteState.Done);
        	stack.log().errorTr(531);
        	stack.resume();
        	return;
        }
        
		ApiSession sess = (ApiSession) ss;
        
		Task t = Task.subtask(stack.getActivity().getTaskRun(), "Downloading", new OperationCallback() {
			@Override
			public void callback() {
				stack.setState(ExecuteState.Done);
				stack.resume();
	        	return;
			}
		});
		
		t.withParams(new RecordStruct(
				new FieldStruct("LocalPath", dest),
				new FieldStruct("RemotePath", src),
				new FieldStruct("ServiceName", service)
				//new FieldStruct("TransferParams", storeParams),
				//new FieldStruct("ForceOverwrite", !allowResume)
		));
		
		DownloadFile work = new DownloadFile();
		work.setSession(sess);
		
		t.withWork(work);
		
		Hub.instance.getWorkPool().submit(t);
	}
	
	@Override
	public void cancel(StackEntry stack) {
		// do nothing, this isn't cancellable
	}
}
