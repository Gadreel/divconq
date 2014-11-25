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
import divconq.script.StackEntry;
import divconq.script.inst.With;
import divconq.util.FileUtil;
import divconq.util.StringUtil;

public class TempFolder extends With {
	@Override
	public void prepTarget(StackEntry stack) {
        String name = stack.stringFromSource("Name");
        
        if (StringUtil.isEmpty(name))
        	name = "TempFolder_" + stack.getActivity().tempVarName();
        
        String vname = name;
        
        Path path = FileUtil.allocateTempFolder2();

        FileSystemDriver drv = new FileSystemDriver(path);
        FileSystemFile fh = new FileSystemFile(drv, CommonPath.ROOT, true);
        
        drv.isTemp(true);
        
        stack.addVariable("TempFS_" + stack.getActivity().tempVarName(), drv);
        stack.addVariable(vname, fh);
        this.setTarget(stack, fh);
		
		this.nextOpResume(stack);
	}
}
