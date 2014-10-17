package divconq.io.ctp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;

abstract public class CtpMessage {
	protected byte msgType = 0;		// 1 byte
	
	public byte getMsgType() {
		return this.msgType;
	}
	
	public void setMsgType(byte msgType) {
		this.msgType = msgType;
	}
	
	abstract public ByteBuf encode(ByteBufAllocator ba);

	abstract public void release();

	abstract public boolean decode(ChannelHandlerContext ctx, ByteBuf in);
}
