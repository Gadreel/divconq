package divconq.io.ctp;

import divconq.lang.chars.Utf8Encoder;
import divconq.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;

public class ClientHelloMessage extends CtpMessage {
	protected String sessionid = null;
	protected String code = null;
	protected String chanid = null;
	
	public ClientHelloMessage() {
		this.msgType = 6;
	}

	@Override
	public ByteBuf encode(ByteBufAllocator ba) {
		int size = 1 + 1 + 66;  // msg type, token size, token content
				
		ByteBuf bb = ba.buffer(size);
		
		bb.writeByte(this.msgType);
		bb.writeByte(66);
		
		// must be 38 bytes
		if (StringUtil.isNotEmpty(this.sessionid) && (this.sessionid.length() == 38))
			Utf8Encoder.encode(this.sessionid, bb);
		else
			bb.writeZero(38);
		
		// must be 12 bytes
		if (StringUtil.isNotEmpty(this.code) && (this.code.length() == 12))
			Utf8Encoder.encode(this.code, bb);
		else
			bb.writeZero(12);
		
		// must be 16 bytes
		if (StringUtil.isNotEmpty(this.chanid) && (this.chanid.length() == 16))
			Utf8Encoder.encode(this.chanid, bb);
		else
			bb.writeZero(16);
		
		return bb;
	}

	@Override
	public void release() {
		// na
	}

	protected int decodeSize = -1;
	
	@Override
	public boolean decode(ChannelHandlerContext ctx, ByteBuf in) {
		if (this.decodeSize == -1) {
			if (in.readableBytes() < 1) 
				return false;
			
			this.decodeSize = in.readByte();
		}
        
		if (in.readableBytes() < this.decodeSize) 
			return false;
		
		// TODO fill in member vars instead of skip
		in.skipBytes(this.decodeSize);
        
		return true;
	}
}
