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
package divconq.script.inst.file;

import java.nio.file.Path;
import java.nio.file.Paths;

import divconq.interchange.FileSystemDriver;
import divconq.lang.op.OperationContext;
import divconq.script.StackEntry;
import divconq.script.inst.With;
import divconq.util.StringUtil;

public class LocalFileStore extends With {
	@Override
	public void prepTarget(StackEntry stack) {
        String name = stack.stringFromSource("Name");
        
        if (StringUtil.isEmpty(name))
        	name = "FileStore_" + stack.getActivity().tempVarName();
        
        String vname = name;
        
        String folder = stack.stringFromSource("RootFolder");
        String path = stack.stringFromSource("RootPath");
        
        if (StringUtil.isEmpty(folder)) {
        	OperationContext.get().errorTr(534);
			this.nextOpResume(stack);
			return;
        }
        
        Path lpath = null;
        
        try {
        	lpath = StringUtil.isNotEmpty(path) ? Paths.get(folder, path.substring(1)) : Paths.get(folder);
        }
        catch (Exception x) {
        	OperationContext.get().errorTr(535, x);
			this.nextOpResume(stack);
			return;
        }

        FileSystemDriver drv = new FileSystemDriver(lpath);
        
        stack.addVariable(vname, drv);
        
        this.setTarget(stack, drv);
		
		this.nextOpResume(stack);
	}
}
