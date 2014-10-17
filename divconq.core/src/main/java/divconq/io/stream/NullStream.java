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
import divconq.work.TaskRun;
import divconq.xml.XElement;

public class NullStream extends BaseStream implements IStreamDest {
    protected int files = 0;
    protected long bytes = 0;

    @Override
    public void init(StackEntry stack, XElement el, boolean autorelative) {
    }
    
	// make sure we don't return without first releasing the file reference content
    @Override
    public HandleReturn handle(TaskRun cb, StreamMessage msg) {
    	if (msg == StreamMessage.FINAL) {
    		cb.info("Null got " + this.bytes + " bytes and " + this.files + " files/folders.");
    		
    		cb.complete();
           	return HandleReturn.DONE;
    	}
       	
    	if (msg.isEof())
    		this.files++;
    	
    	ByteBuf pay = msg.getPayload();
    	
    	if (pay != null) {
    		this.bytes += pay.readableBytes();
    		pay.release();
    	}
    	
       	return HandleReturn.CONTINUE;
    }
    
    @Override
    public void request(TaskRun cb) {
    	this.upstream.request(cb);
    }

	@Override
	public void execute(TaskRun cb) {
		this.upstream.request(cb);
	}
}
