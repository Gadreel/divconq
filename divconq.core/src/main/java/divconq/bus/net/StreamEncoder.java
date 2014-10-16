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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class StreamEncoder extends MessageToByteEncoder<StreamMessage> {

	@Override
	protected void encode(ChannelHandlerContext ctx, StreamMessage msg, ByteBuf out) throws Exception {
		msg.toSerial(out);
		
		if (msg.hasData()) {
			ByteBuf bb = msg.getData();
			
			try {
				bb.readerIndex(0);
				out.writeInt(bb.readableBytes());
				out.writeBytes(bb);
			}
			finally {
				bb.release();		// buffer no longer needed
			}
		}
		else
			out.writeInt(0);
	}
}
