package divconq.io.stream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import divconq.io.ctp.CtpMessage;

public class TerminateStream extends CtpMessage {
	public TerminateStream() {
		this.msgType = 7;
	}

	@Override
	public ByteBuf encode(ByteBufAllocator ba) {
		return ba.buffer(1).writeByte(7);
	}

	@Override
	public void release() {
		// na
	}

	@Override
	public boolean decode(ChannelHandlerContext ctx, ByteBuf in) {
		return true;
	}
}
