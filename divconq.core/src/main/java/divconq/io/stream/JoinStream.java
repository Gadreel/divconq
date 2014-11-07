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
package divconq.io.stream;

import io.netty.buffer.ByteBuf;

import divconq.script.StackEntry;
import divconq.xml.XElement;

public class JoinStream extends BaseStream implements IStreamSource {
    public JoinStream() {
    }

	@Override
	public void init(StackEntry stack, XElement el) {
	}
    
	// make sure we don't return without first releasing the file reference content
    @Override
    public HandleReturn handle(StreamMessage msg) {
    	if (msg == StreamMessage.FINAL) 
    		return this.downstream.handle(msg);

    	ByteBuf in = msg.getPayload();

		return this.downstream.handle(this.nextMessage(in, msg));
    }
    
    public StreamMessage nextMessage(ByteBuf out, StreamMessage curr) {
		// create the output message
		StreamMessage blk = new StreamMessage();
		
        blk.setPayload(out);
        blk.setModified(System.currentTimeMillis());		
        
        // keep the path, just vary the name to the template
        blk.setPath(curr.getPath().resolvePeer("/file.bin"));		// TODO support other names, currently assumes we are writing to a file dest instead of folder dest so name ignored
       	blk.setFileSize(0);						// don't know size ahead of time
        
        return blk;
    }
    
    @Override
    public void request() {
    	this.upstream.request();
    }
}
