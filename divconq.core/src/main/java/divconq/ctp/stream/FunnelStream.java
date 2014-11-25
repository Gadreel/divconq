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

public class FunnelStream extends BaseStream implements IStreamSource {
	protected int aperture = 1;
	protected FileDescriptor current = null;
	protected ByteBuf currbuf = null;
	protected boolean relayed = false;

	// TODO currently only small Aperture is supported well because we may not get large buffers from 
	// source.  we should accumulate small buffers into a large buffer so we always pass the correct size
	// down, except at EOF of course.  see remnant in UngzipStream for an example of this sort of buffer gathering
	
	@Override
	public void init(StackEntry stack, XElement el) {
		this.aperture = (int) stack.intFromElement(el, "Aperture", this.aperture);
	}
	
	public boolean hasMore() {
		FileDescriptor curr = this.current;

		if (curr == null)
			return false;
		
		if (!this.relayed)		// TODO what about EOF, we need to send that along, so even first is not enough?
			return true;
		
		ByteBuf payload = this.currbuf;
		
		return (payload != null) && payload.isReadable();
	}
	
	public ReturnOption nextMessage() {
		FileDescriptor curr = this.current;
		
		if (curr == null)
			return ReturnOption.CONTINUE;

		FileDescriptor blk = new FileDescriptor();
		blk.copyAttributes(curr);
		
		ByteBuf payload = this.currbuf;
		
		if ((payload != null) && payload.isReadable()) {
			int ramt = Math.min(this.aperture, payload.readableBytes());
			
			ByteBuf pslice = payload.copy(payload.readerIndex(), ramt);
			
			payload.skipBytes(ramt);
			
			// TODO blk.payloadoffset = 0;			
			
			blk.setEof(!payload.isReadable() && curr.isEof());
			
			if (blk.isEof()) {
				payload.release();
				
				this.current = null;
				this.currbuf = null;
			}
			
			payload = pslice;
		}
		else {
			blk.setEof(curr.isEof());
			
			if (payload != null)
				payload.release();
			
			payload = null;
			this.current = null;
			this.currbuf = null;
		}
		
		// current has been sent at least once
		this.relayed = true;
		
		return this.downstream.handle(blk, payload);
	}
	
	@Override
	public ReturnOption handle(FileDescriptor file, ByteBuf data) {
    	if (file == FileDescriptor.FINAL) 
    		return this.downstream.handle(file, data);
    	
		this.current = file;
		this.currbuf = data;
		this.relayed = false;
		
		while (this.hasMore()) {
			ReturnOption ret = this.nextMessage();
			
			if (ret != ReturnOption.CONTINUE)
				return ret;
		}
    	
       	return ReturnOption.CONTINUE;
	}

	@Override
	public void read() {
		while (this.hasMore()) {
			ReturnOption ret = this.nextMessage();
			
			if (ret != ReturnOption.CONTINUE)
				return;
		}
		
    	this.upstream.read();
	}

	@Override
	public void close() {
		ByteBuf curr = this.currbuf;
		
		if (curr != null) 
			curr.release();
		
		this.currbuf = null;
		this.current = null;
		
		super.close();
	}
	
}
