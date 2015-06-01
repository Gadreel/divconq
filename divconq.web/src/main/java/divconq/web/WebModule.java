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
package divconq.web;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import divconq.hub.Hub;
import divconq.hub.HubEvents;
import divconq.log.Logger;
import divconq.mod.ModuleBase;
import divconq.util.StringUtil;
import divconq.web.http.ServerHandler;
import divconq.web.http.SniHandler;
import divconq.xml.XElement;

//TODO integrate streaming - http://code.google.com/p/red5/
public class WebModule extends ModuleBase {
	protected ConcurrentHashMap<Integer, Channel> activelisteners = new ConcurrentHashMap<>();
    protected ReentrantLock listenlock = new ReentrantLock();
    protected WebSiteManager siteman = new WebSiteManager();

    public WebSiteManager getWebSiteManager() {
    	return this.siteman;
    }
    
	@Override
	public void start() {
		// prepare the web site manager from settings in module config
		this.siteman.start(this, this.config);
		
		// private hub then go online ASAP
		//if (!Hub.instance.getResources().isGateway())
		//	this.goOnline();
		
		Hub.instance.subscribeToEvent(HubEvents.Connected, e -> this.goOnline());
		Hub.instance.subscribeToEvent(HubEvents.Booted, e -> this.goOffline());
	}
	
	public void goOnline() {
		this.listenlock.lock();
		
		try {
			// don't try if already in online mode
			if (this.activelisteners.size() > 0)
				return;
			
			// typically we should have an extension, unless we are supporting RPC only
			// TODO if (WebSiteManager.instance.getDefaultExtension() == null) 
			//	log.warn(0, "No default extension for web server");
			
			for (final XElement httpconfig : this.config.selectAll("HttpListener")) {
		        final boolean secure = "True".equals(httpconfig.getAttribute("Secure"));
		        int httpport = (int) StringUtil.parseInt(httpconfig.getAttribute("Port"), secure ? 443 : 80);
				
				// -------------------------------------------------
				// message port
				// -------------------------------------------------
		        ServerBootstrap b = new ServerBootstrap();
		        
		        // TODO consider using shared EventLoopGroup
		        // http://normanmaurer.me/presentations/2014-facebook-eng-netty/slides.html#25.0
		        
		        b.group(Hub.instance.getEventLoopGroup())
		         .channel(NioServerSocketChannel.class)
		         .option(ChannelOption.ALLOCATOR, Hub.instance.getBufferAllocator())
		         //.option(ChannelOption.SO_BACKLOG, 125)			// this is probably not needed but serves as note to research
		         .option(ChannelOption.TCP_NODELAY, true)			
		         .childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
		    	        ChannelPipeline pipeline = ch.pipeline();
		    	        
		    	        if (secure) {
		    	        	SniHandler ssl = new SniHandler(WebModule.this.siteman);
		    	        	
		    	            //SslHandler ssl = new SslHandler(WebModule.this.siteman.findSslContextFactory("root").getServerEngine()); 
		    	        	
		    	        	pipeline.addLast("ssl", ssl);
		    	        }
		    	        
		    	        //pipeline.addLast("codec-http", new HttpServerCodec());
		    	        //pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
		    	        
		    	        pipeline.addLast("decoder", new HttpRequestDecoder(4096,8192,262144));
		    	        pipeline.addLast("encoder", new HttpResponseEncoder());
		    	        
		    	        pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
		    	        
		    	        // TODO maybe - but currently we selectively compress files which is more efficient
		    	        // this can also be a cached & compressed response that way
		    	        //pipeline.addLast("deflater", new HttpContentCompressor());
		    	        
		    	        pipeline.addLast("handler", new ServerHandler(httpconfig, WebModule.this.siteman));
					}        	 
		        });
	
		        try {
		        	// must wait here, both to keep the activelisteners listeners up to date
		        	// but also to make sure we don't release connectLock too soon
			        ChannelFuture bfuture = b.bind(httpport).sync();
			        
			        if (bfuture.isSuccess()) {
			        	Logger.info("Web Server listening - now listening for HTTP on TCP port " + httpport);
				        this.activelisteners.put(httpport, bfuture.channel());
			        }
			        else
			        	Logger.error("Web Server unable to bind: " + bfuture.cause());
		        }
		        catch (InterruptedException x) {
		        	Logger.error("Web Server interrupted while binding: " + x);
		        }
		        catch (Exception x) {
		        	Logger.error("Web Server errored while binding: " + x);
		        }
			}
		}
		finally {
			this.listenlock.unlock();
		}
		
		this.siteman.online();
		
		//for (IWebExtension ext : WebSiteManager.instance.extensions.values())
		//	ext.online();
	}
	
	public void goOffline() {
		this.listenlock.lock();
		
		try {
			// we don't want to listen anymore
			for (final Integer port : this.activelisteners.keySet()) {
				// tear down message port
				Channel ch = this.activelisteners.remove(port);
	
		        try {
		        	// must wait here, both to keep the activelisteners listeners up to date
		        	// but also to make sure we don't release connectLock too soon
		        	ChannelFuture bfuture = ch.close().sync();
			        
			        if (bfuture.isSuccess()) 
			        	Logger.info("Web Server unbound");
			        else
			        	Logger.error("Web Server unable to unbind: " + bfuture.cause());
		        }
		        catch (InterruptedException x) {
		        	Logger.error("Web Server unable to unbind: " + x);
		        }
			}
		}
		finally {
			this.listenlock.unlock();
		}
	}

	@Override
	public void stop() {
		this.goOffline();
	}
}
