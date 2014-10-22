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
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteBufferInputStream extends InputStream {
	protected ByteBuffer buf = null;

	/**
	 * be aware this resets the position to 0
	 * 
	 * @param buf source for the stream
	 */
	public ByteBufferInputStream(ByteBuffer buf) {
		buf.position(0);
		this.buf = buf;
	}

	public synchronized int read() throws IOException {
		if (!buf.hasRemaining()) 
			return -1;
		
		return (int)(buf.get() & 0xFF);
	}

	public synchronized int read(byte[] bytes, int off, int len) throws IOException {
		if (!buf.hasRemaining()) 
			return -1;
		
		len = Math.min(len, buf.remaining());
		buf.get(bytes, off, len);
		return len;
	}
}
