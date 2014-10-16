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
package divconq.web;

import java.nio.ByteBuffer;

import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import divconq.lang.Memory;

public class HttpBodyRequestDecoder implements IContentDecoder {
	protected Memory m = new Memory();
	protected int max = 0;
	protected IBodyCallback callback = null;

	public HttpBodyRequestDecoder(int max, IBodyCallback cb) {
		this.callback = cb;
		this.max = max;
	}
	
	public void offer(HttpContent chunk) {
		int newsize = chunk.content().readableBytes() + m.getLength();
		
		if (newsize > this.max) {
			this.callback.fail();
			return;
		}
		
		for (ByteBuffer b : chunk.content().nioBuffers())
			m.write(b);
		
		if (chunk instanceof LastHttpContent) 
			this.callback.ready(this.m);
	}
	
	@Override
	public void release() {
	}
	
	@Override
	public String toString() {
		return this.m.toString();
	}
}
