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
package divconq.bus;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import divconq.net.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import divconq.bus.net.ClientHandler;
import divconq.bus.net.Session;
import divconq.bus.net.StreamHandler;
import divconq.bus.net.ServerHandler;
import divconq.bus.net.SocketInfo;
import divconq.bus.net.SslContextFactory;
import divconq.bus.net.StreamDecoder;
import divconq.bus.net.StreamEncoder;
import divconq.bus.net.StreamMessage;
import divconq.bus.net.StreamSession;
import divconq.hub.Hub;
import divconq.hub.ISystemWork;
import divconq.hub.SysReporter;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.log.Logger;
import divconq.net.acl.AclFilter;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;
import divconq.xml.XElement;

/*
 * Service, Attachments, Extension are all reserved headers - Feature and Op semi-reserved.  All other headers are fine as long as the don't start with _.
 */

public class Bus {
	// info for this Hub
	protected HubRouter localhub = null;
    
	protected boolean proxymode = false;
	
	// includes Hubs exposed through PP2 connectors
	protected final ConcurrentHashMap<String, ServiceRouter> servicerouters = new ConcurrentHashMap<>();	
	protected final ConcurrentHashMap<String, HubRouter> hubrouters = new ConcurrentHashMap<>();

	protected AclFilter acl = new AclFilter();		// TODO support

	protected Lock connectLock = new ReentrantLock();
	protected Lock hubLock = new ReentrantLock();
	
	// desired listeners
	protected List<SocketInfo> listeners = new CopyOnWriteArrayList<>();
	protected final ConcurrentHashMap<SocketInfo, Channel> activelisteners = new ConcurrentHashMap<>();
	protected final ConcurrentHashMap<SocketInfo, Channel> activestreamlisteners = new ConcurrentHashMap<>();
	
	// desired connectors
	protected ConcurrentHashMap<String,SocketInfo> connectors = new ConcurrentHashMap<>();
	
	// bus event group is separate from rest
	protected EventLoopGroup eventLoopGroup = null;
	
    /*
     * set localHub before calling this
     */
    public void init(OperationResult or, XElement config) {
    	// TODO this should be separate from keep alive.  Sync Services should happen on demand as services change on Hub 
    	// - update based on timestamp, so keep alive may have the timestamp but not the services payload - other side then needs to poll services if out of date.
    	// keep alive can be more like 1 minute or so, except when pushed to indicate status change (Hub Status or Service Status changes)
    	int syncperiodsec = 15;		
    	int conninterval = 5;	
		
		this.localhub = new HubRouter();
		
		this.hubrouters.put(this.localhub.getHubId(), this.localhub);

    	if (config != null) {
    		this.proxymode = "True".equals(config.getAttribute("Proxy"));
    		
    		SslContextFactory.init(config);
    		
        	for(XElement node : config.selectAll("Acl")) 
        		this.acl.loadConfig(node);
        	
        	for(XElement node : config.selectAll("Listener")) {
        		SocketInfo si = new SocketInfo();
        		si.loadConfig(node);
        		this.addListener(si);
        	}
        	
        	for(XElement node : config.selectAll("Connector")) {
        		SocketInfo si = new SocketInfo();
        		si.loadConfig(node);
        		this.addConnector(si);
        	}
        	
        	/*
        	// TODO how many threads for eventGroup?
    		// as of yet, nothing to do - get settings as/when required
        	
        	for(XElement node : config.selectAll("Pool")) 
        		this.sessionpool.init(node);        	
        	*/
        	
        	if (config.hasAttribute("SyncServiceList")) 
        		syncperiodsec = (int) StringUtil.parseInt(config.getAttribute("SyncServiceList"), syncperiodsec);
        	
        	if (config.hasAttribute("ConnectingInterval")) 
        		conninterval = (int) StringUtil.parseInt(config.getAttribute("ConnectingInterval"), conninterval);
    	}
		
    	final int connint = conninterval;
    	
    	ISystemWork busconnector = new ISystemWork() {			
			@Override
			public void run(SysReporter reporter) {
				reporter.setStatus("dcBus Connect");
				
				if (!Hub.instance.isStopping())
					Bus.this.connect();
				
				reporter.setStatus("after dcBus Connect");
			}
			
			@Override
			public int period() {
				return connint;
			}
		};
    	
	    Hub.instance.getClock().addSlowSystemWorker(busconnector);
    	
	    final int syncperiodsec2 = syncperiodsec;
	    
	    // keep the connected hubs up to date with our services list
	    Hub.instance.getClock().addSlowSystemWorker(new ISystemWork() {			
			@Override
			public void run(SysReporter reporter) {
				reporter.setStatus("dcBus keep alive");
				
				for (HubRouter hub : Bus.this.hubrouters.values()) {
					hub.keepAlive();
				
					hub.cleanup();
				}
				
				reporter.setStatus("after dcBus keep alive");
			}
			
			@Override
			public int period() {
				return syncperiodsec2;
			}
		});
    }

	private EventLoopGroup getEventLoopGroup() {
		if (this.eventLoopGroup == null)
			this.eventLoopGroup = new NioEventLoopGroup();
		
		return this.eventLoopGroup;
	}

    public boolean isProxyMode() {
    	return this.proxymode;
    }
    
	public HubRouter getLocalHub() {
		return this.localhub;
	}
    
    public Collection<HubRouter> getHubs() {
    	return this.hubrouters.values();
    }

	public OperationResult sendMessage(Message msg) {
		return this.sendMessage(msg, (ServiceResult)null);
    }
	
	public OperationResult sendMessage(Message msg, IReply r) {
		return this.sendMessage(msg, new ServiceResult() {			
			@Override
			public void callback() {
				r.result(this);
			}
		});
	}
	
	// designed to be used either with callback or OpResult
	public OperationResult sendMessage(Message msg, ServiceResult callback) {
		OperationResult or = callback;
		
		if (or == null)
			or = new OperationResult();
		
		OperationContext tc = OperationContext.get();
		
		if (tc == null) {
			or.errorTr(219, msg);		
			
			if (callback != null) 
				callback.abandon();
			
			return or;
		}
		
    	String srv = msg.getFieldAsString("Service");
		
		if (srv == null) {
			or.errorTr(220, msg);
			
			if (callback != null) 
				callback.abandon();
			
			return or;
		}
    	
		ServiceRouter router = this.servicerouters.get(srv);
		
		if (router == null) {
			or.errorTr(221, msg);
			
			if (callback != null) 
				callback.abandon();
			
			return or;
		}
		
		// if no FromHub then add myself
		if (msg.isFieldEmpty("FromHub"))
			msg.setField("FromHub", OperationContext.getHubId());
		
		if (callback != null) 
			callback.setReplyTag(this.localhub.registerForReply(msg, callback));
		
		tc.freeze(msg);

		OperationResult routeres = router.sendMessage(msg);
		
		if (routeres.hasErrors()) {	
			if (callback != null) 
				// put the routing errors into the callback
				callback.abandon();
			
			return or;
		}
		
		// message was sent, record it here
		Hub.instance.getCountManager().countObjects("dcBusMessageSent", msg);
		
		return or;
    }
	
	public boolean isServiceAvailable(String service) {
		ServiceRouter router = this.servicerouters.get(service);
		
		if (router == null) 
			return false;
		
		return router.isAvailable();
	}
	
	/* TODO restore but no waits
	public void sendMessages(ServiceResult callback, TimeoutPlan timeout, Message... msgs) {
		final Semaphore flag = new Semaphore(0);		
		final CountDownLatch latch = new CountDownLatch(msgs.length);
		final RecordStruct responses = new RecordStruct();
		
		for (Message msg : msgs) {
			this.sendMessage(msg, new ServiceResult(timeout) {
				@Override
				public void callback() {
					Message rmsg = this.getResult();
					
					// flag is just for fair play with responses - if it fails still need to count down
					try {
						flag.acquire();
						
						String org = rmsg.getFieldAsString("Tag");
						
						if (org != null)
							responses.setField(org, rmsg);
						
						flag.release();
					} 
					catch (InterruptedException e) {
					}
					
					latch.countDown();					
				}
			});
		}		
		
		flag.release();
		
		try {
			latch.await();
			
			callback.setReply(MessageUtil.success(new RecordStruct(new FieldStruct("Responses", responses))));
		} 
		catch (InterruptedException e) {
			callback.error(1, "Latch failed");		// TODO code 
		}
		
		callback.complete();
    }
    */
	
    public void sendReply(Message msg, Message original) {
    	if (msg == null) 
    		return;
    	
    	// sender didn't want a reply
    	if (!MessageUtil.addressReply(msg, original))
    		return;
    	
    	// TODO consider a setting to disable response checks
		OperationResult mr = Hub.instance.getSchema().validateResponse(msg, original);
		
		if (mr.hasErrors()) {
			System.out.println("Bad Message Content: " + msg);
			
			msg = mr.toLogMessage();		// explain problem to original requester
		
			// TODO temp
			System.out.println("Bad Message Error: " + mr);
			
	    	MessageUtil.addressReply(msg, original);
		}
		
		this.sendMessage(msg);
    }
	
    // for when original Service/Feature/Op don't match your validation needs
    public void sendReply(Message msg, Message original, String serv, String feat, String op) {
    	if (msg == null)
    		return;
    	
    	// sender didn't want a reply
    	if (!MessageUtil.addressReply(msg, original))
    		return;
    	
    	// TODO consider a setting to disable response checks
		OperationResult mr = Hub.instance.getSchema().validateResponse(msg, serv, feat, op);
		
		if (mr.hasErrors()) {
			System.out.println("Bad Message Content: " + msg);
			
			msg = mr.toLogMessage();		// explain problem to original requester
		
			// TODO temp
			System.out.println("Bad Message Error: " + mr);
	    	
	    	MessageUtil.addressReply(msg, original);
		}
		
		this.sendMessage(msg);
    }
    
    /*
    // TODO replace with dcEvents
    public void whenAvailable(String service, OperationCallback callback) {
    	// TODO support fabric/peers and delayed discovery
    	
		ServiceRouter router = (service != null) 
				? this.servicerouters.get(service) 
				: null;
		
		if (router == null) 
			callback.error(1, "Unable to find service: " + service);		// TODO code
		
		callback.completed();
    }
    */
    
    public HubRouter allocateOrGetHub(String id) {
    	this.hubLock.lock();
   
    	try {
    		HubRouter hr = this.hubrouters.get(id);
			
			if (hr == null) {
				hr = new HubRouter(id);
				this.hubrouters.put(id, hr);
				
				Hub.instance.getCountManager().allocateSetNumberCounter("dcBusHubCount", this.hubrouters.size());
			}
			
			return hr;
		}
    	finally {
    		this.hubLock.unlock();
    	}
    }
    
	public void indexServices(HubRouter hub) {
		//System.out.println(" ****************************************** ");
		//System.out.println("       SERVICES INDEXED!!! ");
		//System.out.println(" ****************************************** ");
		
		for (String srv : hub.getServices())
			if (!this.servicerouters.containsKey(srv))
				this.servicerouters.put(srv, new ServiceRouter(srv));
		
		for (ServiceRouter rt : this.servicerouters.values())
			rt.index(hub);
	}

	public void removeServices(HubRouter hub) {
		for (ServiceRouter rt : this.servicerouters.values())
			rt.remove(hub);
	}
	
	public void addConnector(SocketInfo info) {
		if (info == null)
			return;

		this.connectors.putIfAbsent(info.getHubId(), info);
	}

	/**
	 * Unlike removeListener this does not remove any "binding"
	 * just won't try connecting again anymore
	 * 
	 * @param info descriptor of connector to remove
	 */
	public void removeConnector(SocketInfo info) {
		this.connectors.remove(info.getHubId());
		
		HubRouter router = this.hubrouters.get(info.getHubId());
		
		if (router.isLocal())
			return;
		
		router.remove(info);
	}
	
	public SocketInfo getHubConnector(String hubid) {
		return this.connectors.get(hubid);
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
			//   Add client connections when not enough
			// ==========================================================================
			
			for (final SocketInfo info : this.connectors.values()) {
				HubRouter router = this.allocateOrGetHub(info.getHubId());
				
				if (router.isLocal())
					continue;
				
				// -------------------------------------------------
				// message port
				// -------------------------------------------------
				int conncount = router.getCountSessions(info);
				
				// add a coonection only once per call to connect (should be between 2 - 15 seconds between calls)
				if (conncount < info.getCount()) {
			        Bootstrap b = new Bootstrap();
			        
			        b.group(this.getEventLoopGroup())
			         .channel(NioSocketChannel.class)
			         .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 250)		
			         .option(ChannelOption.ALLOCATOR, Hub.instance.getBufferAllocator())
			         .handler(new ChannelInitializer<SocketChannel>() {
			             @Override
			             public void initChannel(SocketChannel ch) throws Exception {
			                 ChannelPipeline pipeline = ch.pipeline();
			                 
			                 if (info.isUseSsl())
			                	 pipeline.addLast("ssl", new SslHandler(SslContextFactory.getClientEngine()));
			                 
			                 pipeline.addLast("http-codec", new HttpClientCodec());
			                 pipeline.addLast("aggregator", new HttpObjectAggregator(8192));		// TODO is this too small?
			                 
			                 pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(60));  // TODO config
			                 pipeline.addLast("writeTimeoutHandler", new WriteTimeoutHandler(30));   // TODO config
			                 
			                 pipeline.addLast("ws-handler", new ClientHandler(info));
			             }
			         });

			        Logger.debug("dcBus Client connecting");
			        
			        try {
			        	// must wait here to make sure we don't release connectLock too soon
			        	// we want channel init (above) to complete before we try connect again
			        	b.connect(info.getAddress()).sync();
			        }
			        catch (InterruptedException x) {
			        	Logger.warn("dcBus Client interrupted while connecting: " + x);
			        }
			        catch (Exception x) {
			        	Logger.debug("dcBus Client unable to connect: " + x);
			        }
				}
				
				// -------------------------------------------------
				// stream port
				// -------------------------------------------------
				conncount = router.getCountStreamSessions(info);
				
				// add a coonection only once per call to connect (should be between 2 - 15 seconds between calls)
				if (conncount < info.getStreamCount()) {
			        Bootstrap b = new Bootstrap();
			        
			        b.group(this.getEventLoopGroup())
			         .channel(NioSocketChannel.class)
			         .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 250)			
			         .option(ChannelOption.ALLOCATOR, Hub.instance.getBufferAllocator())
			         .handler(new ChannelInitializer<SocketChannel>() {
			             @Override
			             public void initChannel(SocketChannel ch) throws Exception {
							ChannelPipeline pipeline = ch.pipeline();
							 
							if (info.isUseSsl())
								pipeline.addLast("ssl", new SslHandler(SslContextFactory.getClientEngine()));
							
							// TODO consider compression
							 
							pipeline.addLast("decoder", new StreamDecoder());
							pipeline.addLast("encoder", new StreamEncoder());
			                 
							pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(60));  // TODO config
							pipeline.addLast("writeTimeoutHandler", new WriteTimeoutHandler(30));   // TODO config
			                 							
							pipeline.addLast("handler", new StreamHandler(info, false));
			             }
			         });

			        Logger.debug("dcBus Client stream connecting");
			        
			        try {
			        	// must wait here to make sure we don't release connectLock too soon
			        	// we want chanel init (above) to complete before we try connect again
			        	b.connect(info.getStreamAddress()).addListener(new GenericFutureListener<ChannelFuture>() {
							@Override
							public void operationComplete(ChannelFuture cf) throws Exception {
								if (!cf.isSuccess()) {
									Logger.debug("dcBus Stream unable to connect: " + cf.cause());
									return;
								}
						    	
								// client starts the HELLO thing once connected!
								StreamMessage icmd = Hub.instance.getBus().getLocalHub().buildStreamHello(info.getHubId());
					            cf.channel().writeAndFlush(icmd);
							}                	
						}).sync();
			        }
			        catch (InterruptedException x) {
			        	Logger.warn("dcBus Client stream interrupted while connecting: " + x);
			        }
			        catch (Exception x) {
			        	Logger.debug("dcBus Client stream unable to connect: " + x);
			        }
				}
			}

			// ==========================================================================
			//   Add server binding when missing
			// ==========================================================================
			
			for (final SocketInfo info : this.listeners) {
				// only if not currently bound
				if (this.activelisteners.containsKey(info))
					continue;

				// -------------------------------------------------
				// message port
				// -------------------------------------------------
		        ServerBootstrap b = new ServerBootstrap();
		        
		        b.group(this.getEventLoopGroup())
		         .channel(NioServerSocketChannel.class)
		         .option(ChannelOption.ALLOCATOR, Hub.instance.getBufferAllocator())
		         //.option(ChannelOption.SO_BACKLOG, 125)			// this is probably not needed but serves as note to research
		         .childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
		    	        ChannelPipeline pipeline = ch.pipeline();
		    	        
		    	        if (info.isUseSsl())
		    	        	pipeline.addLast("ssl", new SslHandler(SslContextFactory.getServerEngine()));
		    	        
		    	        pipeline.addLast("codec-http", new HttpServerCodec());
		    	        pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
		                 
		    	        pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(60));  // TODO config
		    	        pipeline.addLast("writeTimeoutHandler", new WriteTimeoutHandler(30));   // TODO config
		                 
		    	        pipeline.addLast("handler", new ServerHandler(info));
					}        	 
		         });

		        try {
		        	// must wait here, both to keep the activelisteners listeners up to date
		        	// but also to make sure we don't release connectLock too soon
			        ChannelFuture bfuture = b.bind(info.getAddress()).sync();
			        
			        if (bfuture.isSuccess()) {
			        	Logger.info("dcBus Message Server listening");
				        this.activelisteners.put(info, bfuture.channel());
			        }
			        else
			        	Logger.error("dcBus Server unable to bind: " + bfuture.cause());
		        }
		        catch (InterruptedException x) {
		        	Logger.warn("dcBus Server interrupted while binding: " + x);
		        }
		        catch (Exception x) {
		        	Logger.error("dcBus Server unable to bind: " + x);
		        }

				// -------------------------------------------------
				// stream port
				// -------------------------------------------------
		        b = new ServerBootstrap();
		        
		        b.group(this.getEventLoopGroup())
		         .channel(NioServerSocketChannel.class)
		         .option(ChannelOption.ALLOCATOR, Hub.instance.getBufferAllocator())
		         //.option(ChannelOption.SO_BACKLOG, 125)			// this is probably not needed but serves as note to research
		         .childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
		    	        ChannelPipeline pipeline = ch.pipeline();
		    	        
		    	        if (info.isUseSsl())
		    	        	pipeline.addLast("ssl", new SslHandler(SslContextFactory.getServerEngine()));
						
						// TODO consider compression
		    	        
						pipeline.addLast("decoder", new StreamDecoder());
						pipeline.addLast("encoder", new StreamEncoder());		    	        
		                 
		    	        pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(60));  // TODO config
		    	        pipeline.addLast("writeTimeoutHandler", new WriteTimeoutHandler(30));   // TODO config
		    	        
		    	        pipeline.addLast("handler", new StreamHandler(info, true));
					}        	 
		         });

		        try {
		        	// must wait here, both to keep the activelisteners listeners up to date
		        	// but also to make sure we don't release connectLock too soon
			        ChannelFuture bfuture = b.bind(info.getStreamAddress()).sync();
			        
			        if (bfuture.isSuccess()) {
			        	Logger.info("dcBus Stream Server listening");
				        this.activestreamlisteners.put(info, bfuture.channel());
			        }
			        else
			        	Logger.error("dcBus Stream Server unable to bind: " + bfuture.cause());
		        }
		        catch (InterruptedException x) {
		        	Logger.warn("dcBus Stream Server interrupted while binding: " + x);
		        }
		        catch (Exception x) {
		        	Logger.error("dcBus Stream Server unable to bind: " + x);
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

	public OperationResult sendMessage(StreamMessage msg) {
		OperationResult res = new OperationResult();
		
		if (msg == null) {
			res.error(1, "Message is missing");		// TODO log codes
			return res;
		}
		
		if (msg.isFieldEmpty("ToHub") || msg.isFieldEmpty("ToSession") || msg.isFieldEmpty("ToChannel")) {
			res.error(1, "Message is missing addressing");		// TODO log codes
			msg.release();
			return res;
		}
		
		String hub = msg.getFieldAsString("ToHub");
		
		HubRouter router = this.hubrouters.get(hub);
		
		if (router == null) {
			res.error(1, "No network path to hub");		// TODO log codes
			msg.release();
			return res;
		}
		
		OperationResult routeres = router.deliverMessage(msg);
		
		if (routeres.hasErrors()) 	
			return res;
		
		// message was sent, record it here
		Hub.instance.getCountManager().countObjects("dcBusStreamMessageSent", msg);
		
		return res;
    }
	
    public OperationResult sendReply(StreamMessage msg, RecordStruct original) {
    	//if (msg == null) { 
    	//	return ;
    	
    	MessageUtil.streamAddressReply(msg, original);
		
		return this.sendMessage(msg);
    }
    
    protected void stopSocketListener(SocketInfo info) {
		// tear down message port
		Channel ch = this.activelisteners.remove(info);

        try {
        	// must wait here, both to keep the activelisteners listeners up to date
        	// but also to make sure we don't release connectLock too soon
        	ChannelFuture bfuture = ch.close().sync();
	        
	        if (bfuture.isSuccess()) 
		        System.out.println("dcBus Server unbound");
	        else
	        	System.out.println("dcBus Server unable to unbind: " + bfuture.cause());
        }
        catch (InterruptedException x) {
        	System.out.println("dcBus Server unable to unbind: " + x);
        }

		// tear down stream port
		ch = this.activestreamlisteners.remove(info);

        try {
        	if (ch != null) {
	        	ChannelFuture bfuture = ch.close().sync();
		        
		        if (bfuture.isSuccess()) 
			        System.out.println("dcBus Stream Server unbound");
		        else
		        	System.out.println("dcBus Stream Server unable to unbind: " + bfuture.cause());
        	}
        	else
        		System.out.println("dcBus Stream Server missing channel");
        }
        catch (InterruptedException x) {
        	System.out.println("dcBus Stream Server unable to unbind: " + x);
        }
    }
    
    public void stopMatrix(OperationResult or) {
		this.connectLock.lock();
		
		try {
			// we don't want to listen anymore
			for (final SocketInfo info : this.activelisteners.keySet()) 
				this.stopSocketListener(info);
	    	
			for (HubRouter router : this.hubrouters.values())
				router.close();
		}
		finally {
			this.connectLock.unlock();
		}
    }

	public boolean isConnected() {
		for (HubRouter hub : this.hubrouters.values()) 
			if (hub.isDirect())
				return true;
		
		return false;
	}
    
    public void stopFinal(OperationResult or) {
    	// TODO sync these guys
		
		try {
			if (this.eventLoopGroup != null)
				this.eventLoopGroup.shutdownGracefully().await();
		} 
		catch (InterruptedException x) {
		}
    }
	
	public void dumpInfo() {
		System.out.println("End-points and connections: ");
		System.out.println();
		
		System.out.println();
		System.out.println("Hubs: ");
		System.out.println();
		
		for (HubRouter hub : this.hubrouters.values()) {
			String connectkind = hub.isLocal() 
					? "Self"
					: hub.isDirect()
						? "Direct"
						: hub.isTunneled()
							? "Tunneled"
							: "???";
			
			System.out.println("- " + hub.getHubId() + " - " + (hub.isActive() ? "Active" : "Inactive") + " - " + connectkind);
			System.out.println("  >>> " + StringUtil.join(hub.services, ","));
			
			if (hub.isDirect()) {
				System.out.println("  $$$ cmd: " + hub.sessions.size() + " - data: " + hub.streamsessions.size());
				
				for (Session sess : hub.sessions) {
					System.out.println("      +++ msg sess " + sess.getChannel()
							 + " - open: " + sess.getChannel().isOpen()
							 + " - active: " + sess.getChannel().isActive()
							 + " - write: " + sess.getChannel().isWritable()
							 + " - regist: " + sess.getChannel().isRegistered());					
				}				
				
				for (StreamSession sess : hub.streamsessions) {
					System.out.println("      +++ data sess " + sess.getChannel() + " written: " + sess.getWritten() + " - read: " + sess.getRead()
							 + " - open: " + sess.getChannel().isOpen()
							 + " - active: " + sess.getChannel().isActive()
							 + " - write: " + sess.getChannel().isWritable()
							 + " - regist: " + sess.getChannel().isRegistered());					
				}				
			}
			
			String proxied = StringUtil.join(hub.proxied.keySet(), ",");
			
			if (StringUtil.isNotEmpty(proxied)) 
				System.out.println("  ||| " + proxied);
		}
		
		System.out.println();
		System.out.println("Services: ");
		System.out.println();
		
		for (ServiceRouter router : this.servicerouters.values()) {
			System.out.println("- " + router.getName());
			
			List<String> al = new ArrayList<String>();
			
			for (HubRouter a : router.hubList())
				al.add(a.getHubId());
			
			System.out.println("  >>> " + StringUtil.join(al, ","));
		}
		
		Hub.instance.dumpDomainNames();
	}

	public AclFilter getAcl() {
		return this.acl ;
	}

	public ServiceRouter getServiceRouter(String srv) {
		return this.servicerouters.get(srv);
	}
}
