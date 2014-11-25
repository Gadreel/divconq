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
import divconq.script.StackEntry;
import divconq.xml.XElement;

public class JoinStream extends BaseStream implements IStreamSource {
	protected FileDescriptor jfile = null;
	
    public JoinStream() {
    }

	@Override
	public void init(StackEntry stack, XElement el) {
	}
    
	// make sure we don't return without first releasing the file reference content
	@Override
	public ReturnOption handle(FileDescriptor file, ByteBuf data) {
    	if (file == FileDescriptor.FINAL) 
    		return this.downstream.handle(file, null);
    	
    	if (this.jfile == null) {
    		// create the output file desc
    		this.jfile = new FileDescriptor();
    		
    		this.jfile.setModTime(System.currentTimeMillis());		
            
            // keep the path, just vary the name to the template
    		this.jfile.setPath(file.path().resolvePeer("/file.bin"));		// TODO support other names, currently assumes we are writing to a file dest instead of folder dest so name ignored
    		this.jfile.setSize(0);						// don't know size ahead of time
    	}

		return this.downstream.handle(this.jfile, data);
    }
    
    @Override
    public void read() {
    	this.upstream.read();
    }
}
