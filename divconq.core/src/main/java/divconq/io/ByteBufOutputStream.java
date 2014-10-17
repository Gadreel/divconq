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

public class ByteBufOutputStream extends OutputStream {
	protected ByteBuf buf = null;

	public ByteBufOutputStream(ByteBuf buf) {
		this.buf = buf;
	}

	public ByteBufOutputStream(ByteBufWriter buf) {
		this.buf = buf.buffer;
	}

	public synchronized void write(int b) throws IOException {
		this.buf.writeByte(b);
	}

	public synchronized void write(byte[] bytes, int off, int len)
			throws IOException {
		this.buf.writeBytes(bytes, off, len);
	}
}
