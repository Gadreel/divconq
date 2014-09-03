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
package divconq.bus.net;

import java.nio.ByteBuffer;

import divconq.struct.RecordStruct;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCounted;

public class StreamMessage extends RecordStruct {
	protected ByteBuf data = null;
	
	public boolean hasData() {
		return (this.data != null);
	}
	
	public ByteBuf getData() {
		return this.data;
	}
	
	public void setData(ByteBuf v) {
		this.data = v;
	}
	
	public StreamMessage() {
	}
	
	public StreamMessage(String op) {
		this.setField("Op", op);
	}

	public StreamMessage(String op, ByteBuf buf) {
		this(op);
		this.data = buf;
	}

	// TODO clean up this usage - it means something different than above, confusing
	public StreamMessage(String op, ByteBuffer buf) {
		this(op);
		this.data = Unpooled.copiedBuffer(buf.array(), 0, buf.position());
	}

	public boolean hasError() {
		return (this.getFieldAsInteger("Result", 0) > 0);
	}

	public boolean isFinal() {
		return "Final".equals(this.getFieldAsString("Op"));
	}

	public boolean isStart() {
		return "Start".equals(this.getFieldAsString("Op"));
	}

	public boolean isBlock() {
		return "Block".equals(this.getFieldAsString("Op"));
	}

	public int refCnt() {
		if (this.data != null)
			return this.data.refCnt();
		
		return 0;
	}

	public ReferenceCounted retain() {
		if (this.data != null)
			return this.data.retain();
		
		return null;
	}

	public ReferenceCounted retain(int increment) {
		if (this.data != null)
			return this.data.retain(increment);
		
		return null;
	}

	public boolean release() {
		if (this.data != null)
			return this.data.release();
		
		return true;
	}

	public boolean release(int decrement) {
		if (this.data != null)
			return this.data.release(decrement);
		
		return true;
	}
}
