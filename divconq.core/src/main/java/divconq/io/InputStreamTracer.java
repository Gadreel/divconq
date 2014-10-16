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

public class InputStreamTracer extends InputStream {
	protected InputStream real = null;
	
	public InputStreamTracer(InputStream real) {
		this.real = real;
	}
	
	@Override
	public int read() throws IOException {
		System.out.println("read 1");
		return this.real.read();
	}
	
	@Override
	public int available() throws IOException {
		System.out.println("avail");
		return this.real.available();
	}
	
	@Override
	public void close() throws IOException {
		System.out.println("close");
		this.real.close();
	}
	
	@Override
	public synchronized void mark(int readlimit) {
		System.out.println("mark");
		this.real.mark(readlimit);
	}
	
	@Override
	public boolean markSupported() {
		System.out.println("mark support");
		return this.real.markSupported();
	}
	
	@Override
	public int read(byte[] b) throws IOException {
		System.out.println("read b: " + b.length);
		return this.real.read(b);
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		System.out.println("read: " + len);
		return this.real.read(b, off, len);
	}
	
	@Override
	public synchronized void reset() throws IOException {
		System.out.println("reset");
		this.real.reset();
	}
	
	@Override
	public long skip(long n) throws IOException {
		byte[] k = new byte[(int)n];
		
		System.out.println("skip: " + n);
		return this.real.read(k);
	}
}
