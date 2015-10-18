package divconq.test.net;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import divconq.lang.chars.Utf8Decoder;
import divconq.net.ssl.SslHandshakeCompletionEvent;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class LocalEchoServerHandler extends ChannelInboundHandlerAdapter {
	protected Thread worker = null;
	protected LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
	protected Channel chan = null;
	protected AtomicLong gotten = new AtomicLong();
	protected AtomicLong proc = new AtomicLong();
	
	public LocalEchoServerHandler() {
		this.worker = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						String msg = queue.take();
						
						long gg = gotten.get();
						long pp = proc.incrementAndGet();
						
						if (msg.length() == 512)
							System.out.println("Processing " + pp + " of " + gg);
						else
							System.out.println("ERROR Processing " + pp + " of " + gg + " ERROR ==============================================================");
						
						// do processing
						//Thread.sleep(2);
						
						//System.out.println("Echo and read: " + msg);
						
						//ByteBuf b = Unpooled.wrappedBuffer(Utf8Encoder.encode(">" + msg + "<"));
						
						//chan.writeAndFlush(b);
						
						//chan.config().setAutoRead(true);
						
						if (queue.size() == 0)
							chan.read();
					} 
					catch (InterruptedException x) {
					}
				}
			}
		});
		
		this.worker.setDaemon(true);
		this.worker.start();
	}
	
    private ByteBuf buf = null;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.buf = ctx.alloc().buffer(512); // (1)
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
    	this.buf.release(); // (1)
    	this.buf = null;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
    	super.channelActive(ctx);
    	
    	// allow first read
    	ctx.channel().read();
    }
    
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (this.chan == null)
			this.chan = ctx.channel();
		
		this.chan.config().setAutoRead(false);
		
		ByteBuf m = (ByteBuf) msg;
		boolean more = m.readableBytes() > 0;
        
		//System.out.println("got: " + m.readableBytes());
		
		while (more) {
			int amt = Math.min(this.buf.writableBytes(), m.readableBytes());
			
	        this.buf.writeBytes(m, m.readerIndex(), amt); // (2)
	        m.skipBytes(amt);
	        
	        if (m.readableBytes() == 0) {
	        	m.release();
	        	more = false;
	        }
	        
	        if (this.buf.writerIndex() == 512) {
				this.queue.add(Utf8Decoder.decode(this.buf).toString());
				this.gotten.incrementAndGet();
				
				this.buf.clear();
	        }
		}
		
        // Write back as received
        //ctx.write(msg);
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        //ctx.flush();
		
		//System.out.println("read complete");
	}
	

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
    		throws Exception {
    	
    	// on success the request first read
    	if (evt == SslHandshakeCompletionEvent.SUCCESS) {
    		ctx.read();
    	}
    }
}