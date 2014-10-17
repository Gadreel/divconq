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

import divconq.interchange.CommonPath;
import divconq.interchange.IFileStoreDriver;
import divconq.interchange.IFileStoreFile;
import divconq.lang.FuncCallback;
import divconq.script.StackEntry;
import divconq.script.inst.With;
import divconq.struct.Struct;
import divconq.util.StringUtil;

public class File extends With {
	@Override
	public void prepTarget(StackEntry stack) {
        String name = stack.stringFromSource("Name");
        
        if (StringUtil.isEmpty(name))
        	name = "Folder_" + stack.getActivity().tempVarName();
        
        String vname = name;
        
        Struct ss = stack.refFromSource("In");
        
        if ((ss == null) || (!(ss instanceof IFileStoreDriver) && !(ss instanceof IFileStoreFile))) {
        	stack.log().errorTr(536);
    		this.nextOpResume(stack);
        	return;
        }
        
        CommonPath path = null;
        
        try {
            path = new CommonPath(stack.stringFromSource("Path", "/"));
        }
        catch (Exception x) {
			stack.log().errorTr(537);
			this.nextOpResume(stack);
			return;
        }

        IFileStoreDriver drv = null;
        
        if (ss instanceof IFileStoreDriver) {
            drv = (IFileStoreDriver)ss;
        }
        else {
        	drv = ((IFileStoreFile)ss).driver();
        	path = ((IFileStoreFile)ss).resolvePath(path);
        }
        
        drv.getFileDetail(path, new FuncCallback<IFileStoreFile>() {
			@Override
			public void callback() {
				stack.log().copyMessages(this);
				
				if (this.hasErrors()) {
					stack.log().errorTr(538);
					File.this.nextOpResume(stack);
					return;
				}
				
	            IFileStoreFile fh = this.getResult();			            
	            
	            if (!fh.exists() && stack.getInstruction().getXml().getName().equals("Folder"))
	            	fh.isFolder(true); 
				
	            stack.addVariable(vname, (Struct)fh);
	            
	            File.this.setTarget(stack, (Struct)fh);
	            
	    		File.this.nextOpResume(stack);
			}
		});
	}
}
