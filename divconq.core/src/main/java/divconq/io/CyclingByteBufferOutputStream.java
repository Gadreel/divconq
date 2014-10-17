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
package divconq.io;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.io.OutputStream;

public class CyclingByteBufferOutputStream extends OutputStream {
	protected ByteBuf buf = null;

	public void installBuffer(ByteBuf buf) {
		this.buf = buf;
	}

	public ByteBuf uninstallBuffer() {
		ByteBuf buf = this.buf;
		this.buf = null;
		return buf;
	}
	
	public CyclingByteBufferOutputStream() {
	}

	public synchronized void write(int b) throws IOException {
		buf.writeByte(b);
	}

	public synchronized void write(byte[] bytes, int off, int len) throws IOException {
		buf.writeBytes(bytes, off, len);
	}
}
