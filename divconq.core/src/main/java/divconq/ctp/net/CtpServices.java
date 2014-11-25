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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import divconq.net.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import divconq.bus.net.SocketInfo;
import divconq.bus.net.SslContextFactory;
import divconq.ctp.CtpAdapter;
import divconq.ctp.s.CtpsHandler;
import divconq.hub.Hub;
import divconq.hub.ISystemWork;
import divconq.hub.SysReporter;
import divconq.lang.op.OperationContext;
import divconq.log.Logger;
import divconq.net.acl.AclFilter;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class CtpServices {
	protected AclFilter acl = new AclFilter();		// TODO support

	protected Lock connectLock = new ReentrantLock();
	
	// desired listeners
	protected List<SocketInfo> listeners = new CopyOnWriteArrayList<>();
	protected final ConcurrentHashMap<SocketInfo, Channel> activelisteners = new ConcurrentHashMap<>();
	
    /*
     * set localHub before calling this
     */
    public void init(XElement config) {
    	int conninterval = 5;	

    	if (config != null) {
        	for(XElement node : config.selectAll("Acl")) 
        		this.acl.loadConfig(node);
        	
        	for(XElement node : config.selectAll("Listener")) {
        		SocketInfo si = new SocketInfo();
        		si.loadConfig(node);
        		this.addListener(si);
        	}
        	
        	if (config.hasAttribute("ConnectingInterval")) 
        		conninterval = (int) StringUtil.parseInt(config.getAttribute("ConnectingInterval"), conninterval);
    	}
		
    	int connint = conninterval;
    	
    	ISystemWork busconnector = new ISystemWork() {			
			@Override
			public void run(SysReporter reporter) {
				reporter.setStatus("dcCtp Services Connect");
				
				if (!Hub.instance.isStopping())
					CtpServices.this.connect();
				
				reporter.setStatus("After dcCtp Services Connect");
			}
			
			@Override
			public int period() {
				return connint;
			}
		};
    	
	    Hub.instance.getClock().addSlowSystemWorker(busconnector);
    }
	
	public void addListener(SocketInfo info) {
		if (info == null)
			return;

		this.listeners.add(info);
	}
	
	public void removeListener(SocketInfo info) {
		this.listeners.remove(info);
	}
    
	public void connect() {
		// never try to connect until init has run
		if (Hub.instance.isStopping())
			return;
		
		// if connect method is already running then skip - it will try again later 
		if (!this.connectLock.tryLock())
			return;
		
		try {
			// ==========================================================================
			//   Add server binding when missing
			// ==========================================================================
			
			for (SocketInfo info : this.listeners) {
				// only if not currently bound
				if (this.activelisteners.containsKey(info))
					continue;

				// -------------------------------------------------
				// stream port
				// -------------------------------------------------
				ServerBootstrap b = new ServerBootstrap()		        
		        	.group(Hub.instance.getEventLoopGroup())
		        	.channel(NioServerSocketChannel.class)
		        	.option(ChannelOption.ALLOCATOR, Hub.instance.getBufferAllocator())
		        	//.option(ChannelOption.SO_BACKLOG, 125)			// this is probably not needed but serves as note to research
		        	.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
			    	        ChannelPipeline pipeline = ch.pipeline();
			    	        
			    	        if (info.isUseSsl())
			    	        	pipeline.addLast("ssl", new SslHandler(SslContextFactory.getServerEngine()));		// TODO this should be the external SSL not the BUS one 
							
			    	        pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(60));  // TODO config
			    	        pipeline.addLast("writeTimeoutHandler", new WriteTimeoutHandler(45));   // TODO config

                            //pipeline.addLast("logger", new LoggingHandler(LogLevel.INFO));
			    	        
			    	        // start as guest until authenticated
	              			CtpAdapter adapter = new CtpAdapter(OperationContext.allocateGuest());
	              			adapter.setHandler(new CtpsHandler());
	              			
	              			pipeline.addLast("ctp", new CtpHandler(adapter, true));
	              		}
	              	});

		        try {
		        	// must wait here, both to keep the activelisteners listeners up to date
		        	// and also to make sure we don't release connectLock too soon
			        ChannelFuture bfuture = b.bind(info.getAddress()).sync();
			        
			        if (bfuture.isSuccess()) {
			        	Logger.info("dcCtp Server listening");
				        this.activelisteners.put(info, bfuture.channel());
			        }
			        else
			        	Logger.error("dcCtp Server unable to bind: " + bfuture.cause());
		        }
		        catch (InterruptedException x) {
		        	Logger.warn("dcCtp Server interrupted while binding: " + x);
		        }
		        catch (Exception x) {
		        	Logger.error("dcCtp Server unable to bind: " + x);
		        }
			}

			// ==========================================================================
			//   Remove server binding as needed
			// ==========================================================================

			for (final SocketInfo info : this.activelisteners.keySet()) {
				// all is well if in the listeners list
				if (this.listeners.contains(info))
					continue;
				
				// otherwise we don't want to bind anymore
				this.stopSocketListener(info);
			}
		}
		finally {
			this.connectLock.unlock();
		}
	}
    
    protected void stopSocketListener(SocketInfo info) {
		// tear down message port
		Channel ch = this.activelisteners.remove(info);

        try {
        	// must wait here, both to keep the activelisteners listeners up to date
        	// but also to make sure we don't release connectLock too soon
        	ChannelFuture bfuture = ch.close().sync();
	        
	        if (bfuture.isSuccess()) 
		        System.out.println("dcCtp Server unbound");
	        else
	        	System.out.println("dcCtp Server unable to unbind: " + bfuture.cause());
        }
        catch (InterruptedException x) {
        	System.out.println("dcCtp Server unable to unbind: " + x);
        }
    }
    
    public void stopMatrix() {
		this.connectLock.lock();
		
		try {
			// we don't want to listen anymore
			for (final SocketInfo info : this.activelisteners.keySet()) 
				this.stopSocketListener(info);
		}
		finally {
			this.connectLock.unlock();
		}
    }

	public AclFilter getAcl() {
		return this.acl ;
	}
}
