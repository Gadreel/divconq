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
package divconq.api.internal;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import divconq.api.HyperSession;
import divconq.hub.Hub;
import divconq.lang.op.OperationCallback;
import divconq.lang.op.OperationResult;
import divconq.log.Logger;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.LastHttpContent;
import divconq.net.ssl.SslHandler;
import io.netty.util.concurrent.Future;

public class UploadPutHandler extends SimpleChannelInboundHandler<HttpObject> { 
    protected Channel dest = null;
    protected ReadableByteChannel src = null;
    protected Map<String, Cookie> cookies = new HashMap<>();
    protected OperationCallback callback = null;
	
	public Channel allocateChannel(final HyperSession parent, OperationResult or) {
		final AtomicReference<Future<Channel>> sslready = new AtomicReference<>();
		
        Bootstrap b = new Bootstrap();
        
        b.group(Hub.instance.getEventLoopGroup())
         .channel(NioSocketChannel.class)
		 .option(ChannelOption.ALLOCATOR, Hub.instance.getBufferAllocator())
         .handler(new ChannelInitializer<SocketChannel>() {
             @Override
             public void initChannel(SocketChannel ch) throws Exception {
                 ChannelPipeline pipeline = ch.pipeline();
                 
                 if (parent.getInfo().isSecurel()) {
                	 SslHandler sh = new SslHandler(parent.getSsl().getClientEngine());
                	 sslready.set(sh.handshakeFuture());
                	 pipeline.addLast("ssl", sh);
                 }
                 
                 pipeline.addLast("codec", new HttpClientCodec());

                 // so we can get the upload response (200 or not)
                 pipeline.addLast("handler", UploadPutHandler.this);
             }
         });

        or.info("Web Data Client connecting");
        
        try {
        	// must wait here to make sure we don't release connectLock too soon
        	// we want chanel init (above) to complete before we try connect again
        	ChannelFuture f = b.connect(parent.getInfo().getAddress()).sync();
        	
        	if (!f.isSuccess()) {
            	or.error(1, "Web Client unable to successfully connect: " + f.cause());
        	}
        	
        	// it has appeared that sometimes we "overshoot" the ssl handshake in code - to prevent
        	// that lets wait for the handshake to be done for sure
        	if (sslready.get() != null) {
        		Future<Channel> sf = sslready.get().sync();
            	
            	if (!sf.isSuccess()) {
                	or.error(1, "Web Client unable to securely connect: " + sf.cause());
            	}
        	}
        	
        	return f.channel();
        }
        catch (InterruptedException x) {
        	or.error(1, "Web Client interrupted while connecting: " + x);
        }
        catch (Exception x) {
        	or.error(1, "Web Client unable to connect: " + x);
        }
        
        return null;
	}
    
	public void start(final HyperSession parent, ScatteringByteChannel src, String chanid, Map<String, Cookie> cookies, long size, long offset, final OperationCallback callback) {
    	this.src = src;
    	this.cookies = cookies;
    	this.callback = callback;
		
    	this.dest = this.allocateChannel(parent, callback);
    	
    	if (this.callback.hasErrors()) {
        	callback.complete();
        	return;
    	}
    	
		// send a request to get things going		
		HttpRequest req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, "/upload/" + chanid + "/Final");
		
		req.headers().set(Names.HOST, parent.getInfo().getHost());
		req.headers().set(Names.USER_AGENT, "DivConq HyperAPI Client 1.0");
		req.headers().set(Names.CONNECTION, HttpHeaders.Values.CLOSE);
        req.headers().set(Names.COOKIE, ClientCookieEncoder.STRICT.encode(this.cookies.values()));
        req.headers().set(HttpHeaders.Names.CONTENT_LENGTH, size - offset);

        // send request headers - must flush here in case CL = 0
        this.dest.writeAndFlush(req);

        // now start sending the file
		long sent = offset;
		callback.getContext().setAmountCompleted((int)(sent * 100 / size));
			
		ByteBuf bb = null;
		
		try {
			bb = Hub.instance.getBufferAllocator().directBuffer(64 * 1024);		// TODO review if direct is best here
			
			long toskip = offset;
			
			if (src instanceof SeekableByteChannel) {
				((SeekableByteChannel)src).position(toskip);
			}
			else {
				while (toskip > 0) {
					int skip = (int) Math.min(bb.capacity(), toskip);
					toskip -= bb.writeBytes(src, skip);
					bb.clear();
				}
			}
			
			// now start writing the upload
			int amt = bb.writeBytes(src, bb.capacity());
			
			while (amt != -1) {
				bb.retain();		// this ups ref cnt to 2 - we plan to reuse the buffer
				
		        this.dest.writeAndFlush(bb).sync();
				
				sent += amt;
				
				if (size > 0) 
					callback.getContext().setAmountCompleted((int) (sent * 100 / size));
				
				// by the time we get here, that buffer has been used up and we can use it for the next buffer
				if (bb.refCnt() != 1) 
					throw new IOException("Buffer reference count is not correct");
				
				// stop writing if canceled
				if (!this.dest.isOpen()) {
					this.finish();		// might already be finished but to be sure (this is helpful when api.abortStream is called)
					break;
				}
				
				bb.clear();
				
				amt = bb.writeBytes(src, bb.capacity());
			}
			
			// we are now done with it
			bb.release();
		} 
		catch (Exception x) {
			try {
				if (bb != null)
					bb.release();
			}
			catch (Exception x2) {
			}
			
			callback.error(1, "Local read error: " + x);
			this.finish();
		} 
	}
	
	public void finish() {
		//System.out.println("client finished with content");
		
		try {
			if (this.src != null)
				this.src.close();
		} 
		catch (IOException x) {
		}
		
		this.closeDest();
		this.callback.complete();
	}
	
	@Override
	public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
		if (msg instanceof HttpResponse) {
			if (((HttpResponse)msg).getStatus().code() != 200)
				Logger.error("Upload Put Handler got unexpected read, non http response");
		}
		else if (msg != LastHttpContent.EMPTY_LAST_CONTENT)
			Logger.error("Upload Put Handler got unexpected read, non http response");

		this.finish();
	}

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Web Data Client disconnected!");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    	// TODO logging
        cause.printStackTrace();        
        ctx.close();
    }

	public void closeDest() {
		try {
			if (this.dest != null)
				this.dest.close().await(2000);
		} 
		catch (InterruptedException x) {
			// ignore 
		}
	}
}
