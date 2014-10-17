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
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import divconq.api.HyperSession;
import divconq.hub.Hub;
import divconq.lang.OperationCallback;
import divconq.lang.OperationResult;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import divconq.net.ssl.SslHandler;
import io.netty.util.concurrent.Future;

public class DownloadHandler extends SimpleChannelInboundHandler<HttpObject> {
    protected Channel src = null;
    protected WritableByteChannel dest = null;
    protected Map<String, Cookie> cookies = new HashMap<>();
    protected OperationCallback callback = null;
    protected long size = 0;
    protected long sent = 0;
    
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
				//pipeline.addLast("decoder", new HttpResponseDecoder());
				//pipeline.addLast("encoder", new HttpRequestEncoder());

				pipeline.addLast("handler", DownloadHandler.this);
             }
         });

        or.info("Web Data Client connecting");
        
        try {
        	// must wait here to make sure we don't release connectLock too soon
        	// we want chanel init (above) to complete before we try connect again
        	ChannelFuture f = b.connect(parent.getInfo().getAddress()).sync();
        	
        	if (!f.isSuccess()) {
            	or.error(1, "Web Data Client unable to successfully connect: " + f.cause());
        	}
        	
        	// it has appeared that sometimes we "overshoot" the ssl handshake in code - to prevent
        	// that lets wait for the handshake to be done for sure
        	if (sslready.get() != null) {
        		Future<Channel> sf = sslready.get().sync();
            	
            	if (!sf.isSuccess()) {
                	or.error(1, "Web Data Client unable to securely connect: " + sf.cause());
            	}
        	}
        	
        	return f.channel();
        }
        catch (InterruptedException x) {
        	or.error(1, "Web Data Client interrupted while connecting: " + x);
        }
        catch (Exception x) {
        	or.error(1, "Web Data Client unable to connect: " + x);
        }
        
        return null;
	}
    	
	public void start(final HyperSession parent, WritableByteChannel dest, String chanid, Map<String, Cookie> cookies, long size, long offset, final OperationCallback callback) {
    	this.dest = dest;
    	this.cookies = cookies;
    	this.callback = callback;
    	this.size = size;
    	this.sent = offset;
		
    	this.src = this.allocateChannel(parent, callback);
    	
    	if (this.callback.hasErrors()) {
        	callback.complete();
        	return;
    	}
		
		// send a request to get things going
		
		HttpRequest req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/download/" + chanid);
		
		req.headers().set(Names.HOST, parent.getInfo().getHost());
		req.headers().set(Names.USER_AGENT, "DivConq HyperAPI Client 1.0");
		req.headers().set(Names.CONNECTION, HttpHeaders.Values.CLOSE);
        req.headers().set(Names.COOKIE, ClientCookieEncoder.encode(this.cookies.values()));
        
        // send request
        this.src.writeAndFlush(req);
	}
	
	public void finish() {
		//System.out.println("client finished with content");
		
		try {
			this.dest.close();
		} 
		catch (IOException x) {
		}
		
		this.closeSource();
		this.callback.complete();
	}
	
	@Override
	public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
		//System.out.println("download client got object: " + msg.getClass().getName());

    	if (msg instanceof HttpContent) {
    		HttpContent chunk = (HttpContent) msg;
        	
			try {
				//System.out.println("client offered content");
				int camt = chunk.content().readableBytes();
				
				for (ByteBuffer bb : chunk.content().nioBuffers())
					this.dest.write(bb);
				
				this.sent += camt;

				callback.setAmountCompleted((int)(this.sent * 100 / this.size));
				
				// final only if not canceled
				if (chunk instanceof LastHttpContent) 
					this.finish();
			}
			catch (IOException x) {
				callback.error(1, "Failed download because of local io error: " + x);
				this.finish();
			}
			
    		return;
    	}    	

    	if (!(msg instanceof HttpResponse)) {
			System.out.println("Got unknown instead of headers!");	// TODO
            return;
        }

    	HttpResponse resp = (HttpResponse) msg;

    	// keep the cookies - especially Session!
        List<String> cookies = resp.headers().getAll(Names.SET_COOKIE);
        
        for (String cookie : cookies) {
        	Set<Cookie> cset = CookieDecoder.decode(cookie);
        	
        	for (Cookie c : cset) 
        		this.cookies.put(c.getName(), c);
        }

		// TODO if error response then cancel otherwise 
		// ignore we don't care 	
		
		//System.out.println("Got response: " + resp);
	}

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    	// TODO logging
        System.out.println("Web Client Download disconnected!");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    	// TODO logging
        cause.printStackTrace();        
        ctx.close();
    }

	public void closeSource() {
		try {
			if (this.src != null)
				this.src.close().await(2000);
		} 
		catch (InterruptedException x) {
			// ignore 
		}
	}
}
