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

import divconq.filestore.CommonPath;
import divconq.filestore.local.FileSystemDriver;
import divconq.filestore.local.FileSystemFile;
import divconq.lang.op.OperationContext;
import divconq.script.StackEntry;
import divconq.script.inst.With;
import divconq.struct.Struct;
import divconq.util.FileUtil;
import divconq.util.StringUtil;

/**
 * There is an implicit temp folder available to any script, it is only created on demand
 * and is a global variable _TempFolder  
 * 
 * @author andy
 *
 */
public class TempFile extends With {
	@Override
	public void prepTarget(StackEntry stack) {
        String name = stack.stringFromSource("Name");
        
        if (StringUtil.isEmpty(name))
        	name = "TempFile_" + stack.getActivity().tempVarName();
        
        String vname = name;
        
        String tpath = stack.stringFromSource("Path");
        String text = stack.stringFromSource("Ext");
        
        if (StringUtil.isEmpty(tpath))
        	tpath = "/" + (StringUtil.isNotEmpty(text) ? FileUtil.randomFilename(text) : FileUtil.randomFilename());
        
        CommonPath path = null;
        
        try {
            path = new CommonPath(tpath);
        }
        catch (Exception x) {
        	OperationContext.get().errorTr(539);
			this.nextOpResume(stack);
			return;
        }

        Struct tf = stack.getActivity().queryVariable("_TempFolder");
        FileSystemDriver drv = null;
        
        if (tf instanceof FileSystemDriver) {
        	drv = (FileSystemDriver)tf;
        }
        else {
            Path tfpath = FileUtil.allocateTempFolder2();

            drv = new FileSystemDriver(tfpath);
            drv.isTemp(true);
            
            stack.getActivity().addVariable("_TempFolder", drv);
        }
        
        FileSystemFile fh = new FileSystemFile(drv, path, false);
        
        stack.addVariable(vname, fh);
        this.setTarget(stack, fh);
		
		this.nextOpResume(stack);
	}
}
