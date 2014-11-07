package divconq.io.stream;

import io.netty.buffer.ByteBuf;
import divconq.script.StackEntry;
import divconq.xml.XElement;

public class FunnelStream extends BaseStream implements IStreamSource {
	protected int aperture = 1;
	protected StreamMessage current = null;
	protected boolean relayed = false;

	// TODO currently only small Aperture is supported well because we may not get large buffers from 
	// source.  we should accumulate small buffers into a large buffer so we always pass the correct size
	// down, except at EOF of course.  see remnant in UngzipStream for an example of this sort of buffer gathering
	
	@Override
	public void init(StackEntry stack, XElement el) {
		this.aperture = (int) stack.intFromElement(el, "Aperture", this.aperture);
	}
	
	public boolean hasMore() {
		StreamMessage curr = this.current;

		if (curr == null)
			return false;
		
		if (!this.relayed)
			return true;
		
		ByteBuf payload = curr.getPayload();
		
		return (payload != null) && payload.isReadable();
	}
	
	public StreamMessage nextMessage() {
		StreamMessage curr = this.current;
		
		if (curr == null)
			return null;

		StreamMessage blk = new StreamMessage();
		
		blk.setMsgType(curr.getMsgType());
		blk.setPath(curr.getPath());
		blk.setIsFolder(curr.isFolder());
		blk.setFileSize(curr.getFileSize());
		blk.setModified(curr.getModified());
		blk.setPermission(curr.getPermission());  
		
		ByteBuf payload = curr.getPayload();
		
		if ((payload != null) && payload.isReadable()) {
			int ramt = Math.min(this.aperture, payload.readableBytes());
			
			blk.setPayload(payload.copy(payload.readerIndex(), ramt));
			
			payload.skipBytes(ramt);
			
			// TODO blk.payloadoffset = 0;			
			
			blk.setEof(!payload.isReadable() && curr.isEof());
			
			if (blk.isEof()) {
				payload.release();
				
				this.current = null;
			}
		}
		else {
			blk.setEof(curr.isEof());
			
			if (payload != null)
				payload.release();
			
			this.current = null;
		}
		
		// current has been sent at least once
		this.relayed = true;
		
		return blk;
	}
	
	@Override
	public HandleReturn handle(StreamMessage msg) {
    	if (msg == StreamMessage.FINAL) 
    		return this.downstream.handle(msg);
    	
		this.current = msg;
		this.relayed = false;
		
		while (this.hasMore()) {
			HandleReturn ret = this.downstream.handle(this.nextMessage());
			
			if (ret != HandleReturn.CONTINUE)
				return ret;
		}
    	
       	return HandleReturn.CONTINUE;
	}

	@Override
	public void request() {
		while (this.hasMore()) {
			HandleReturn ret = this.downstream.handle(this.nextMessage());
			
			if (ret != HandleReturn.CONTINUE)
				return;
		}
		
    	this.upstream.request();
	}

	@Override
	public void close() {
		StreamMessage curr = this.current;
		
		if (curr != null) {
			curr.release();
			this.current = null;
		}
		
		// TODO Auto-generated method stub
		super.close();
	}
	
}
