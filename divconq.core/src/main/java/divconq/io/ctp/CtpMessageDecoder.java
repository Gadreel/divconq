package divconq.io.ctp;

import java.util.List;

import divconq.io.stream.StreamMessage;
import divconq.io.stream.TerminateStream;
import divconq.log.Logger;
import divconq.net.ByteToMessageDecoder;
import divconq.net.ssl.SslHandshakeCompletionEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.channel.ChannelHandlerContext;

abstract public class CtpMessageDecoder extends ByteToMessageDecoder {
	protected boolean chanauto = false;
	protected CtpMessage current = null;
    
	public CtpMessageDecoder() {
		super();
		
		this.setSingleDecode(true);
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if ((in instanceof EmptyByteBuf) || (in.readableBytes() == 0))
			return;
		
		if (this.current == null) {
			int msgtype = in.readByte();
			
			if (msgtype == 4)
				this.current = new StreamMessage();
			else if (msgtype == 6)
				this.current = new ClientHelloMessage();
			else if (msgtype == 7)
				this.current = new TerminateStream();
			
			// TODO throw an exception if type not matched
		}
		
		if (this.current.decode(ctx, in)) {
			out.add(this.current);
			this.current = null;
		}
	}
	
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    	super.handlerAdded(ctx);
    	
		// we use manual read only
		ctx.channel().config().setAutoRead(false);
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
    	super.channelActive(ctx);
    	
    	// provoke first read (for ssl)
    	ctx.read();
    }
    
    // we'll want to hand off the message from decoder...
    abstract public void handleMessage();

    /*
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf m = (ByteBuf) msg;
        
		System.out.println("got: " + m.readableBytes());
    	//m.release();
		
		super.channelRead(ctx, msg);
	}
	*/
    
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		super.channelReadComplete(ctx);
		
		if (this.chanauto) {
			System.out.println("read complete");
	
			// next read if in shared mode (or debugging mode)
			ctx.read();
		}
	}
	    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    	super.userEventTriggered(ctx, evt);
    	
    	// on SSL success signal the channel is all ours 
    	if (evt == SslHandshakeCompletionEvent.SUCCESS) 
    		this.chanauto = true;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    	Logger.error("Unhandled exception caught in CtpMessageDecoder: " + cause);
    	
        cause.printStackTrace();
        ctx.close();
    }
}
