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
package divconq.ctp.net;

import java.util.concurrent.locks.ReentrantLock;

import divconq.ctp.CtpAdapter;
import divconq.ctp.ICtpChannel;
import divconq.hub.Hub;
import divconq.log.Logger;
import divconq.net.ssl.SslHandshakeCompletionEvent;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class CtpHandler extends ChannelInboundHandlerAdapter implements ICtpChannel {
	protected CtpAdapter adapter = null;
	protected ChannelHandlerContext chan = null;
	protected boolean serverMode = true;
	
	protected ByteBuf remnant = null;
	protected boolean readRequested = true;		// initially expects a read
	protected ReentrantLock readLock = new ReentrantLock();
	
	public CtpHandler(CtpAdapter tunnel, boolean serverMode) {
		this.adapter = tunnel;
		this.serverMode = serverMode;
	}

	public void debug(String msg) {
		System.out.println("Ctp " + (this.serverMode ? "Server" : "Client") + " - " + msg);
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		this.adapter.close();
	}
	
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
		this.debug("Handler added");		
		this.chan = ctx;
    	this.adapter.setChannel(this);
    }
    
	@Override
	public void read() {
		this.readLock.lock();
		
		if (serverMode)
			System.out.println("Start Server requested read!");
		
		this.readRequested = false;		// meaning read is covered until further notice
		
		try {
			ByteBuf rem = this.remnant;
			
        	//CtpHandler.this.debug("checking remnant: " + rem);

			// if there are any unread bytes from the last read, check to see if we can collect a command
	        if (rem != null) {
	        	//CtpHandler.this.debug("checking bytes: " + rem.readableBytes());
				
	        	//System.out.println("Remnant ref cnt 1: " + rem.refCnt() + " for server: " + CtpHandler.this.serverMode);
	        	
				boolean ready = false;
				
				try {
					ready = this.adapter.decode(rem);
				} 
				catch (Exception x) {
					// TODO error and close!!
					System.out.println("Error decoding message: " + x);
					return;
				}
				
	        	//System.out.println("Remnant ref cnt 2: " + rem.refCnt());
				
				// if there are any unread bytes here we need to store them and combine with the next read
				if (!rem.isReadable()) {
					this.remnant = null;		
					rem.release();
				}
				
	        	//System.out.println("Remnant ref cnt 3: " + rem.refCnt());
				
				if (!ready) {
					this.readRequested = true;
					this.chan.read();
				}
				else
					this.adapter.handleCommand();
	        }
			else {
				this.readRequested = true;
				this.chan.read();
			}
	        
			if (serverMode)
				System.out.println("End Server requested read!");
		}
		finally {
			this.readLock.unlock();
		}
	}
	
	@Override
	public void send(ByteBuf buf, ChannelFutureListener listener) {
		try {
			if (this.chan != null) {
				ChannelFuture future = this.chan.writeAndFlush(buf);
				
				if (listener != null)
					future.addListener(listener);
			}
		}
		catch (Exception x) {
			Logger.error("Error writing Ctp buffer: " + x);
			
			this.close();
		}
	}
	
	@Override
	public void close() {
		try {
			if (this.chan != null)
				this.chan.close().await(2000);
		} 
		catch (InterruptedException x) {
			// ignore 
		}
		finally {
			this.chan = null;
		}
	}
    
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		CtpHandler.this.readLock.lock();
		
		try {
			ByteBuf buf = (ByteBuf) msg;
			
			if (serverMode)
				System.out.println("Server got network read 1: " + buf.readableBytes());
	
			ByteBuf rem = this.remnant;
	
			// if there are any unread bytes from the last read, combine with this read 
			this.remnant = buf;
			
			// TODO there are maybe better ways to do this - a queue of buffers?
			if (rem != null) {
				if (rem.isReadable())  {			
					this.remnant = Hub.instance.getBufferAllocator().heapBuffer(rem.readableBytes() + buf.readableBytes());
					this.remnant.writeBytes(rem);
					this.remnant.writeBytes(buf);
					
					buf.release();
				}
				
				rem.release();
			}
			
			if (serverMode)
				System.out.println("Server got network read 2: " + this.remnant.readableBytes());
			
			if (!this.readRequested)
				return;
			
			if (this.remnant.readableBytes() > 256 * 1024)
				System.out.println("CTP Buffer getting too large - possible issue!!!! " + this.remnant.readableBytes());
			
			// read with the updated buffer
			this.read();
		}
		finally {
			CtpHandler.this.readLock.unlock();
		}
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        //ctx.flush();
		
		//System.out.println("read complete");
		//this.debug("Read Complete");
	}
	

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    	this.debug("Exception");
		
        cause.printStackTrace();
        ctx.close();
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
    		throws Exception {
    	
    	// on success the request first read
    	if (evt == SslHandshakeCompletionEvent.SUCCESS) {
        	// make sure auto read is off
    		ctx.channel().config().setAutoRead(false);
    		
    		this.debug("SSL passed");

    		// do initial read, this is fine - just be sure streaming will work by not always reading
    		ctx.read();
    	}
    }
}
