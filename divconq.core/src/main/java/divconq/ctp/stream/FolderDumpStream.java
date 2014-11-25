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
package divconq.ctp.stream;

import io.netty.buffer.ByteBuf;
import divconq.ctp.f.FileDescriptor;
import divconq.lang.op.OperationContext;
import divconq.script.StackEntry;
import divconq.xml.XElement;

public class FolderDumpStream extends BaseStream implements IStreamDest {

    @Override
    public void init(StackEntry stack, XElement el, boolean autorelative) {
    }
    
	// make sure we don't return without first releasing the file reference content
    @Override
    public ReturnOption handle(FileDescriptor file, ByteBuf data) {
    	if (file == FileDescriptor.FINAL) {
    		OperationContext.get().getTaskRun().complete();
           	return ReturnOption.DONE;
    	}
    	
		System.out.println(" " + file.getPath() + "     " + file.getSize()
				+ "     " + (file.isFolder() ? "FOLDER" : "FILE"));

       	return ReturnOption.CONTINUE;
    }
    
    @Override
    public void read() {
    	this.upstream.read();
    }

	@Override
	public void execute() {
		this.upstream.read();
	}
}
