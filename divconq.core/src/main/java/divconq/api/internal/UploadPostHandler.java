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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import divconq.api.HyperSession;
import divconq.hub.Hub;
import divconq.lang.op.OperationCallback;
import divconq.lang.op.OperationResult;
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
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder.ErrorDataEncoderException;
import divconq.net.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.Future;

public class UploadPostHandler extends SimpleChannelInboundHandler<HttpObject> { 
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

                 // Remove the following line if you don't want automatic content decompression.
                 //pipeline.addLast("inflater", new HttpContentDecompressor());

                 // to be used since huge file transfer
                 pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());

                 // so we can get the upload response (200 or not)
                 pipeline.addLast("handler", UploadPostHandler.this);
             }
         });

        System.out.println("Web Data Client connecting");
        
        try {
        	// must wait here to make sure we don't release connectLock too soon
        	// we want chanel init (above) to complete before we try connect again
        	ChannelFuture f = b.connect(parent.getInfo().getAddress()).sync();
        	
        	if (!f.isSuccess()) {
            	or.error(1, "Web Client unable to successfully connect: " + f.cause());
            	System.out.println("Web Client unable to successfully connect: " + f.cause());
        	}
        	
        	// it has appeared that sometimes we "overshoot" the ssl handshake in code - to prevent
        	// that lets wait for the handshake to be done for sure
        	if (sslready.get() != null) {
        		Future<Channel> sf = sslready.get().sync();
            	
            	if (!sf.isSuccess()) {
                	or.error(1, "Web Client unable to securely connect: " + sf.cause());
                	System.out.println("Web Client unable to securely connect: " + sf.cause());
            	}
        	}
        	
        	return f.channel();
        }
        catch (InterruptedException x) {
        	or.error(1, "Web Client interrupted while connecting: " + x);
        	System.out.println("Web Client interrupted while connecting: " + x);
        }
        catch (Exception x) {
        	or.error(1, "Web Client unable to connect: " + x);
        	System.out.println("Web Client unable to connect: " + x);
        }
        
        return null;
	}
    
	public void start(final HyperSession parent, ReadableByteChannel src, String chanid, Map<String, Cookie> cookies, long size, final OperationCallback callback) {
    	this.src = src;
    	this.cookies = cookies;
    	this.callback = callback;
		
    	this.dest = this.allocateChannel(parent, callback);
    	
    	if (this.callback.hasErrors()) {
        	callback.complete();
        	return;
    	}
		
		// send a request to get things going		
		
        HttpDataFactory factory = new DefaultHttpDataFactory(false); // no disk 
		
		HttpRequest req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/upload/" + chanid + "/Final");
		
		req.headers().set(Names.HOST, parent.getInfo().getHost());
		req.headers().set(Names.USER_AGENT, "DivConq HyperAPI Client 1.0");
		req.headers().set(Names.CONNECTION, HttpHeaders.Values.CLOSE);
        req.headers().set(Names.COOKIE, ClientCookieEncoder.STRICT.encode(this.cookies.values()));
        req.headers().set(Names.TRANSFER_ENCODING, Values.CHUNKED);
        
        HttpPostRequestEncoder bodyRequestEncoder = null;
        
        try {
            bodyRequestEncoder = new HttpPostRequestEncoder(factory, req, true); // true => multipart
            
            bodyRequestEncoder.addBodyHttpData(new UploadStream(src, "file", "fname", "application/octet-stream", "binary", null, size, callback));
            
            
            req = bodyRequestEncoder.finalizeRequest();
        } 
        catch (ErrorDataEncoderException x) {
        	callback.error(1, "Problem with send encoder: " + x);
        	callback.complete();
        	return;
        }

        // send request headers
        this.dest.write(req);

    	try {
			this.dest.writeAndFlush(bodyRequestEncoder).sync();
			
			// wait for a response - then close, see messageReceived
		} 
    	catch (InterruptedException x) {
    		callback.error(1, "Unable to write to socket: " + x);
        	callback.complete();
		}
	}
	
	public void finish() {
		//System.out.println("client finished with content");
		
		try {
			this.src.close();
		} 
		catch (IOException x) {
		}
		
		this.closeDest();
		this.callback.complete();
	}
	
	@Override
	public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
		//System.out.println("upload client got object: " + msg.getClass().getName());

		this.finish();
	}

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    	// TODO logging
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
