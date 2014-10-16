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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ByteBufferOutputStream extends OutputStream {
	protected ByteBuffer buf = null;

	public ByteBufferOutputStream(ByteBuffer buf) {
		this.buf = buf;
	}

	public synchronized void write(int b) throws IOException {
		buf.put((byte) b);
	}

	public synchronized void write(byte[] bytes, int off, int len)
			throws IOException {
		buf.put(bytes, off, len);
	}
}
