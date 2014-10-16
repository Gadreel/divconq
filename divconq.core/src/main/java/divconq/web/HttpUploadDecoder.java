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

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import divconq.bus.MessageUtil;
import divconq.bus.net.StreamMessage;
import divconq.lang.OperationResult;
import divconq.session.DataStreamChannel;

public class HttpUploadDecoder implements IContentDecoder {
	protected int max = 0;
	protected int seq = 0;
	protected DataStreamChannel channel = null;

	public HttpUploadDecoder(int max, DataStreamChannel chan) {
		this.channel = chan;
		this.max = max;
	}
	
	@Override
	public void offer(HttpContent chunk) {
		if (this.channel.isClosed())
			return;		// TODO somehow connect the cancel back to netsession 
		
		if (chunk.content().readableBytes() > this.max) {
			this.channel.abort();		// TODO somehow connect the cancel back to netsession
			return;
		}
		
		ByteBuf bb = chunk.content();
		bb.retain();		// we will use it in upcoming send
		
		System.out.println("ref count a: " + bb.refCnt());
		
		StreamMessage b = new StreamMessage("Block", bb);
		b.setField("Sequence", this.seq);
		
		OperationResult or = this.channel.send(b);
		
		// bb should now be back to 1
		
		System.out.println("ref count b: " + bb.refCnt());
		
		if (or.hasErrors()) {
			this.channel.close();
			return;
		}
		
		this.seq++;

		// TODO track progress if possible
		
		// final only if not canceled
		if (chunk instanceof LastHttpContent)
			this.channel.send(MessageUtil.streamFinal());
	}
	
	@Override
	public void release() {
	}
}
