package divconq.io.ctp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.EncoderException;

public class CtpMessageEncoder extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ByteBuf buf = null;
        
        try {
            if (msg instanceof CtpMessage) {
            	CtpMessage cmsg = (CtpMessage)msg;
            	
                try {
                    buf = cmsg.encode(ctx.alloc());
                } 
                finally {
                    cmsg.release();
                }

                if (buf == null) {
                    ctx.write(Unpooled.EMPTY_BUFFER, promise);
                }
                else if (buf.isReadable()) {
                	System.out.println("CtpMessageEncoder sending: " + buf.readableBytes());
                	
                    ctx.write(buf, promise);
                } 
                else {
                    buf.release();
                    ctx.write(Unpooled.EMPTY_BUFFER, promise);
                }
                
                // if we got here then downstream did not blow up, make sure we don't release in finally
                buf = null;
            } 
            else {
                ctx.write(msg, promise);
            }
        } 
        catch (EncoderException e) {
            throw e;
        } 
        catch (Throwable e) {
            throw new EncoderException(e);
        } 
        finally {
        	// if downstream blows up, be sure we release
            if (buf != null) 
                buf.release();
        }
    }
}
