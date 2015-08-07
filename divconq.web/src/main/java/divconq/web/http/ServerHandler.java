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
package divconq.web.http;

import java.net.InetSocketAddress;

import divconq.bus.Message;
import divconq.bus.MessageUtil;
import divconq.bus.net.StreamMessage;
import divconq.hub.DomainInfo;
import divconq.hub.Hub;
import divconq.hub.HubState;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.log.Logger;
import divconq.net.NetUtil;
import divconq.net.ssl.SslHandler;
import divconq.session.DataStreamChannel;
import divconq.session.ISessionAdapter;
import divconq.session.IStreamDriver;
import divconq.session.Session;
import divconq.struct.CompositeParser;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;
import divconq.web.HttpBodyRequestDecoder;
import divconq.web.HttpContext;
import divconq.web.IContentDecoder;
import divconq.web.IWebExtension;
import divconq.web.Request;
import divconq.web.Response;
import divconq.web.RpcHandler;
import divconq.web.WebDomain;
import divconq.web.WebSiteManager;
import divconq.xml.XElement;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;

/**
 * Handles handshakes and messages
 */
public class ServerHandler extends SimpleChannelInboundHandler<Object> {
	static protected final String RPC_PATH = "/rpc";
	static protected final String BUS_PATH = "/bus";
	static protected final String STATUS_PATH = "status";
	static protected final String DOWNLOAD_PATH = "download";
	static protected final String UPLOAD_PATH = "upload";

	// this is the context used until we figure out if we have a session or not
	static protected OperationContext defaultOpContext = OperationContext.useNewGuest();  
	
    protected HttpContext context = null; 
    protected WebSocketServerHandshaker handshaker = null;		// TODO when handshaker completes then set context.session to a different session adaptor (direct messages to client instead of queue)
    
    public ServerHandler(XElement config, WebSiteManager siteman) {
    	this.context = new HttpContext(config, siteman);
    	//System.out.println("new server handler!!");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    	Session s = this.context.getSession();
    	
    	if (s != null)
    		Logger.info("Web Server connection inactive: " + s.getId());
        
    	if (Logger.isDebug())
    		Logger.debug("Connection inactive was " + ctx.channel().localAddress() 
    				+ " from " + ctx.channel().remoteAddress()); // + " session " + this.context.getSession().getId());
    	
        this.context.closed();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    	//System.out.println("server got object: " + msg.getClass().getName());
    	OperationContext.set(ServerHandler.defaultOpContext);
    	
    	if (this.context.getChannel() == null)
    		this.context.setChannel(ctx.channel());
    	
        if (msg instanceof HttpObject) 
            this.handleHttpRequest(ctx, (HttpObject) msg);
        else if (msg instanceof WebSocketFrame) 
        	this.handleWebSocketFrame(ctx, (WebSocketFrame) msg);        
    }

    // TODO this may not be a real threat but review it anyway
    // http://www.christian-schneider.net/CrossSiteWebSocketHijacking.html
    
    // https://www.owasp.org/index.php/HTML5_Security_Cheat_Sheet
    // https://www.owasp.org/index.php/Cross_Site_Scripting_Flaw
    // https://www.owasp.org/index.php/XSS_(Cross_Site_Scripting)_Prevention_Cheat_Sheet
    // https://code.google.com/p/owasp-java-encoder/source/browse/trunk/core/src/main/java/org/owasp/encoder/HTMLEncoder.java
    // http://kefirsf.org/kefirbb/
    // http://codex.wordpress.org/Validating_Sanitizing_and_Escaping_User_Data
    // http://excess-xss.com/
    // http://en.wikipedia.org/wiki/HTTP_cookie
    
    //  If you wish to support both HTTP requests and websockets in the one server, refer to the io.netty.example.http.websocketx.server.WebSocketServer example. To know once a handshake was done you can intercept the ChannelInboundHandler.userEventTriggered(ChannelHandlerContext, Object) and check if the event was of type WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE.
    
    // TODO CORS
    // also review
    // https://github.com/netty/netty/pull/2427/files
    // http://www.html5rocks.com/en/tutorials/file/xhr2/
    // http://www.html5rocks.com/en/tutorials/cors/
    // http://enable-cors.org/server.html
    
    
    /*
GET http://229097002.log.optimizely.com/event?a=229097002&d=229097002&y=false&x761570292=750582396&s231842852=gc&s231947722=search&s232031415=false&n=http%3A%2F%2Fwww.telerik.com%2Fdownload%2Ffiddler%2Ffirst-run&u=oeu1393506471224r0.17277055932208896&wxhr=true&t=1398696975163&f=702401691,760731745,761570292,766240693,834650096 HTTP/1.1
Host: 229097002.log.optimizely.com
Connection: keep-alive
User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.131 Safari/537.36
Origin: http://www.telerik.com
Accept: * /*
Referer: http://www.telerik.com/download/fiddler/first-run
Accept-Encoding: gzip,deflate,sdch
Accept-Language: en-US,en;q=0.8
Cookie: fixed_external_20728634_bucket_map=; fixed_external_9718688_bucket_map=; fixed_external_138031368_bucket_map=; end_user_id=oeu1393506471224r0.17277055932208896; bucket_map=761570292%3A750582396



HTTP/1.1 200 OK
Access-Control-Allow-Credentials: true
Access-Control-Allow-Methods: POST, GET
Access-Control-Allow-Origin: http://www.telerik.com
Content-Type: application/json
Date: Mon, 28 Apr 2014 14:56:18 GMT
P3P: CP="IDC DSP COR CURa ADMa OUR IND PHY ONL COM STA"
Server: nginx/1.2.7
Content-Length: 2
Connection: keep-alive

{}



Chrome Web Socket Request:

GET /rpc HTTP/1.1
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: v8MIKFOPlaVtRK2C1iOJ4Q==
Host: localhost:9443
Sec-WebSocket-Origin: http://localhost:9443
Sec-WebSocket-Version: 13
x-DivConq-Mode: Private


Java API with Session Id

POST /rpc HTTP/1.1
Host: localhost
User-Agent: DivConq HyperAPI Client 1.0
Connection: keep-alive
Content-Encoding: UTF-8
Content-Type: application/json; charset=utf-8
Cookie: SessionId=00700_fa2h199tkc2e8i2cs4e8s9ujhh_EetvVV9EocXc; $Path="/"





	 * 
     */
    
    public void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
    	if (!this.context.isWebSocket()) {
	    	this.context.isWebSocket(true);
			
	    	// upgrade how session handles responses 
			this.context.getSession().setAdatper(new ISessionAdapter() {				
				@Override
				public void stop() {
					ServerHandler.this.context.close();
				}
				
				@Override
				public ListStruct popMessages() {
					return null;
				}
				
				@Override
				public void deliver(Message msg) {
					// clean up so it looks like an RPC message
					msg.removeField("ToHub");
					msg.removeField("Context");
					msg.removeField("RespondTo");
					msg.removeField("RespondTag");
					msg.removeField("Version");
					
					ServerHandler.this.context.getChannel().writeAndFlush(new TextWebSocketFrame(msg.toString()));
				}
			});			
    	}
    	
        Channel ch = ctx.channel();
        
        // Check for closing frame
        if (frame instanceof CloseWebSocketFrame) {
            Logger.info("Web Server received close");
            ch.close();
            return;
        }
        
        if (frame instanceof PingWebSocketFrame) {
        	Logger.info("Web Server received ping");
            ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        
        if (frame instanceof PongWebSocketFrame) {
        	Logger.info("Web Server received pong");
            return;
        }
        
        if (frame instanceof TextWebSocketFrame) {
    		// use a clean (empty) context - session will add context if necessary 
    		OperationContext.clear();
    		
        	String data = ((TextWebSocketFrame) frame).text();
        	
            //System.out.println("dcBus " + this.session.getSessionMode() + " received message: " + data);
            
            FuncResult<CompositeStruct> res = CompositeParser.parseJson(data);
            
            if (res.hasErrors()) {
            	// TODO logging
            	Logger.warn("Web Server got a bad message: " + res.getMessage());
            	ch.close();		// don't stay with bad messages
            	return;
            }

            // TODO adapt ws message to bus message - see WsServer
            //this.context.receiveMessage(this.context, ch, MessageUtil.fromRecord((RecordStruct)res.getResult()));      
    		
    		CompositeStruct croot = res.getResult();
    		
    		if ((croot == null) || !(croot instanceof RecordStruct)) {
            	Logger.warn("Web Server got a bad message: " + res.getMessage());
            	ch.close();		// don't stay with bad messages
    			return;
    		}
    		
    		RecordStruct mrec = (RecordStruct) croot;
    		
    		// check that the request conforms to the schema for RpcMessage
    		OperationResult rootres = mrec.validate("RpcMessage");
    		
    		if (rootres.hasErrors()) {
            	Logger.warn("Web Server got a bad message: " + res.getMessage());
            	ch.close();		// don't stay with bad messages
    			return;
    		}
    		
    		// if so convert the Record into a Message for transport over our bus
    		Message msg = MessageUtil.fromRecord(mrec);  
    		
       		this.context.getSession().sendMessage(msg);
            
            return;
        }

        // TODO unhandled frame type
        // TODO logging
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Logger.warn("Web server connection exception was " + cause);
    	
    	if (Logger.isDebug())
    		Logger.debug("Web server connection exception was " + ctx.channel().localAddress() 
    				+ " from " + ctx.channel().remoteAddress()); // + " session " + this.context.getSession().getId());
    	
    	// TODO logging
    	//System.out.println("EC?");
        //?cause.printStackTrace();        
        ctx.close();
    }

    public static String getWebSocketLocation(boolean secure, HttpRequest req) {
    	//boolean secure = "True".equals(this.config.getAttribute("Secure"));
    	
        return (secure ? "wss://" : "ws://") + req.headers().get(Names.HOST) + RPC_PATH;
    }

	// TODO improve to ignore large POSTs on most Paths
	// TODO ip lockout
	// TODO acl
	// TODO debug level based in ip address
	// TODO any where along the way, especially RPC, ping Remote Trust Center with down votes if doesn't work out
    public void handleHttpRequest(ChannelHandlerContext ctx, HttpObject httpobj) throws Exception {
    	if (httpobj instanceof HttpContent) {
    		this.context.offerContent((HttpContent)httpobj);
    		return;
    	}    	
    	
    	if (Logger.isDebug())
    		Logger.debug("Web server request " + httpobj.getClass().getName() + "  " + ctx.channel().localAddress() 
    				+ " from " + ctx.channel().remoteAddress()); // + " session " + this.context.getSession().getId());
    	
    	if (!(httpobj instanceof HttpRequest)) {
        	this.context.sendRequestBad();
            return;
        }
    		
		HttpRequest httpreq = (HttpRequest) httpobj;
    	
    	this.context.load(ctx, httpreq);
        
        // Handle a bad request.
        if (!httpreq.getDecoderResult().isSuccess()) {
        	this.context.sendRequestBad();
            return;
        }
        
        Request req = this.context.getRequest();
        Response resp = this.context.getResponse();
        
		// to avoid lots of unused sessions
		if (req.pathEquals("/favicon.ico")) {
			this.context.sendNotFound();
			return;
		}
		
		// make sure we don't have a leftover task context
		OperationContext.clear();
		
		String origin = "http:" + NetUtil.formatIpAddress((InetSocketAddress)ctx.channel().remoteAddress());
		
		// TODO use X-Forwarded-For  if available, maybe a plug in approach to getting client's IP?
		
		DomainInfo dinfo = this.context.getSiteman().resolveDomainInfo(req.getHeader("Host"));
		
		if (dinfo == null) {
	    	if (Logger.isDebug())
	    		Logger.debug("Domain not found for: " + req.getHeader("Host"));
	    	
        	this.context.sendForbidden();
            return;
		}
		
		WebDomain wdomain = this.context.getSiteman().getDomain(dinfo.getId());
		
		// check into url re-routing
		String reroute = wdomain.route(req, (SslHandler)ctx.channel().pipeline().get("ssl"));
		
		if (StringUtil.isNotEmpty(reroute)) {
	    	if (Logger.isDebug())
	    		Logger.debug("Routing the request to: " + reroute);
	    	
			this.context.getResponse().setStatus(HttpResponseStatus.FOUND);
			this.context.getResponse().setHeader("Location", reroute);
			this.context.send();
            return;
		}
				
		Cookie sesscookie = req.getCookie("SessionId");
		Session sess = null;
		
		if (sesscookie != null) {
			String v = sesscookie.getValue();
			String sessionid = v.substring(0, v.lastIndexOf('_'));
			String accesscode = v.substring(v.lastIndexOf('_') + 1);
			
			sess = Hub.instance.getSessions().lookupAuth(sessionid, accesscode);
		}
		
		if (sess == null) {			
			sess = Hub.instance.getSessions().create(origin, dinfo.getId());
			
			Logger.info("Started new session: " + sess.getId() + " on " + req.getPath() + " for " + origin);
			 
			// TODO if ssl set client key on user context
			//req.getSecuritySession().getPeerCertificates();
			
			sess.setAdatper(new ISessionAdapter() {				
				protected volatile ListStruct msgs = new ListStruct();
				
				@Override
				public void stop() {
			    	if (Logger.isDebug())
			    		Logger.debug("Web server session adapter got a STOP request.");
			    	
					ServerHandler.this.context.close();
				}
				
				@Override
				public ListStruct popMessages() {
					ListStruct ret = this.msgs;
					this.msgs = new ListStruct();
					return ret;
				}
				
				@Override
				public void deliver(Message msg) {
					// keep no more than 100 messages - this is not a "reliable" approach, just basic comm help					
					while (this.msgs.getSize() > 99)
						this.msgs.removeItem(0);
					
					this.msgs.addItem(msg);
				}
			});			
			
			Cookie sk = new DefaultCookie("SessionId", sess.getId() + "_" + sess.getKey());
			sk.setPath("/");
			sk.setHttpOnly(true);
			
			resp.setCookie(sk);		 
		}
		
		this.context.setSession(sess);
		
		sess.touch();
		
		OperationContext tc = sess.setContext(origin);

		if (!"rpc".equals(req.getPath().getName(0)))
			tc.info("Web request for host: " + req.getHeader("Host") +  " url: " + req.getPath() + " by: " + origin + " session: " + sess.getId());
		
		/*
		System.out.println("sess proto: " + ((SslHandler)ctx.channel().pipeline().get("ssl")).engine().getSession().getProtocol());
		System.out.println("sess suite: " + ((SslHandler)ctx.channel().pipeline().get("ssl")).engine().getSession().getCipherSuite());
		*/
		
		try {			
	        if (req.pathEquals(ServerHandler.BUS_PATH)) {
		        // Allow only GET methods.
		        if (req.getMethod() != HttpMethod.GET) {
		        	this.context.sendForbidden();
		            return;
		        }
		        
		    	if (Logger.isDebug())
		    		Logger.debug("Setup a bus (ws) handshake " + sess.getId());
		
		        // Handshake
		        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
		        		ServerHandler.getWebSocketLocation("True".equals(this.context.getConfig().getAttribute("Secure")), httpreq), null, false);
		        
		        this.handshaker = wsFactory.newHandshaker(httpreq);
		        
		        if (this.handshaker == null) 
		            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
		        else  {
		        	DefaultFullHttpRequest freq = new DefaultFullHttpRequest(httpreq.getProtocolVersion(), httpreq.getMethod(), httpreq.getUri());
		        	
		        	freq.headers().add(httpreq.headers());
		        	
		        	this.handshaker.handshake(ctx.channel(), freq);
		        	
		        	return;
		        }
		        
	        	this.context.sendForbidden();
		        return;
	        }
	        
			// "upload" is it's own built-in extension.  
	        if ((req.getPath().getNameCount() == 3) && req.getPath().getName(0).equals(ServerHandler.UPLOAD_PATH)) {
				if (!Hub.instance.isRunning()) {		// only allow uploads when running
					this.context.sendRequestBad();
					return;
				}
				
				// currently only supporting POST/PUT of pure binary - though support for form uploads can be restored, see below
				// we cannot rely on content type being meaningful
				//if (!"application/octet-stream".equals(req.getContentType().getPrimary())) {
	            //    this.context.sendRequestBad();
	            //    return;
				//}

				// TODO add CORS support if needed
				
				if ((req.getMethod() != HttpMethod.PUT) && (req.getMethod() != HttpMethod.POST)) {
	                this.context.sendRequestBad();
	                return;
				}
				
				final String cid = req.getPath().getName(1);
				final String op = req.getPath().getName(2);
		        
		    	if (Logger.isDebug())
		    		Logger.debug("Initiating an upload block on " + cid + " for " + sess.getId());
				
				final DataStreamChannel dsc = sess.getChannel(cid);
				
	    		if (dsc == null) {
	                this.context.sendRequestBad();
	                return;
	    		}
	    		
	    		dsc.setDriver(new IStreamDriver() {
	    			@Override
	    			public void cancel() {
	    				Logger.error("Transfer canceled on channel: " + cid);
						dsc.complete();
						ServerHandler.this.context.sendRequestBad();	// TODO headers?
	    			}
	    			
	    			@Override
	    			public void nextChunk() {
	    				Logger.debug("Continue on channel: " + cid);
						ServerHandler.this.context.sendRequestOk();
	    			}
	    			
	    			@Override
	    			public void message(StreamMessage msg) {
	    				if (msg.isFinal()) {
	    					Logger.debug("Final on channel: " + cid);
	    					dsc.complete();
							ServerHandler.this.context.sendRequestOk();
	    				}
	    			}
	    		});	
	    		
	    		//if (req.getMethod() == HttpMethod.PUT) {
		            this.context.setDecoder(new IContentDecoder() {
		            	protected boolean completed = false;
		            	protected int seq = 0;
		            	
						@Override
						public void release() {
							// trust that http connection is closing or what ever needs to happen, we just need to deal with datastream
							
							Logger.debug("Releasing data stream: " + cid + " completed: " + completed);
							
							// TODO this is not true, client may switch connections to continue upload, only a session timeout or datastream timeout count as true problems
							// if not done with request then something went wrong, kill data channel
							//if (!this.completed)
							//	dsc.abort();
						}
						
						@Override
						public void offer(HttpContent chunk) {
							boolean finalchunk = (chunk instanceof LastHttpContent); 
							
							//System.out.println("Chunk: " + finalchunk);
							
							ByteBuf buffer = chunk.content();
							
							if (!dsc.isClosed()) {
					            int size = buffer.readableBytes();
					
					            //System.out.println("Chunk size: " + size);
					            if (Logger.isDebug())
					            	Logger.debug("Offered chunk on: " + cid + " size: " + size + " final: " + finalchunk);
					            
					            dsc.touch();	// TODO try to set progress on dsc
					            
					            // TODO set hint in netty as to where this buffer was handled and sent
					            
					            if (size > 0) {
					            	buffer.retain();		// we will be using a reference up during send
					            	
						    		StreamMessage b = new StreamMessage("Block", buffer);  
						    		b.setField("Sequence", this.seq);
						    		
						    		//System.out.println("Buffer ref cnt a: " + buffer.refCnt());
						    		
						    		OperationResult or = dsc.send(b);
						    		
						    		//System.out.println("Buffer ref cnt b: " + buffer.refCnt());
						    		
						    		// indicate we have read the buffer?
						    		buffer.readerIndex(buffer.writerIndex());
						    		
						    		if (or.hasErrors()) {
						    			dsc.close();
						    			return;
						    		}
						    		
						    		this.seq++;
					            }
					
					            // if last buffer of last block then mark the upload as completed
					    		if (finalchunk) {
					    			if ("Final".equals(op))  
					    				dsc.send(MessageUtil.streamFinal());
					    			else
					    				dsc.getDriver().nextChunk();   
								}
							}
							else {
					            if (Logger.isDebug())
					            	Logger.debug("Offered chunk on closed channel: " + cid);
							}
							
							// means this block is completed, not necessarily entire file uploaded
				    		if (finalchunk) 
					            this.completed = true;
						}
					});
		            
	                //return;
	    		//}
				
	    		/* old approach that supported multipart posts TODO review/remove
	    		if (req.getMethod() == HttpMethod.POST) {
		    		StreamingDataFactory sdf = new StreamingDataFactory(dsc, op);
		    		
		    		// TODO consider supporting non-multipart?
		    		final HttpPostMultipartRequestDecoder prd = new HttpPostMultipartRequestDecoder(sdf, httpreq); 

		            this.context.setDecoder(new IContentDecoder() {					
						@Override
						public void release() {
							// trust that http connection is closing or what ever needs to happen, we just need to deal with datastream
							
							// if not done with request then something went wrong, kill data channel
							if ((prd.getStatus() != MultiPartStatus.EPILOGUE) && (prd.getStatus() != MultiPartStatus.PREEPILOGUE))
								dsc.kill();
						}
						
						@Override
						public void offer(HttpContent chunk) {
							//the only thing we care about is the file, the file will stream to dsc - the rest can disappear
							prd.offer(chunk);		
						}
					});
		            
	                return;
	    		}					    		
				*/
	    		
                //this.context.sendRequestBad();
				return;
			}
			
			// "download" is it's own built-in extension.  
	        if ((req.getPath().getNameCount() == 2) && req.getPath().getName(0).equals(ServerHandler.DOWNLOAD_PATH)) {
				if (!Hub.instance.isRunning()) {		// only allow downloads when running
					this.context.sendRequestBad();
					return;
				}
				
	    		if (req.getMethod() != HttpMethod.GET) {
	                this.context.sendRequestBad();
	                return;
	    		}
				
				String cid = req.getPath().getName(1);
				
	            if (Logger.isDebug())
		    		Logger.debug("Initiating an download on " + cid + " for " + sess.getId());
				
				final DataStreamChannel dsc = sess.getChannel(cid);
				
	    		if (dsc == null) {
	                this.context.sendRequestBad();
	                return;
	    		}
	    		
	    		dsc.setDriver(new IStreamDriver() {
	    			//protected long amt = 0;
	    			protected long seq = 0;
	    			
	    			@Override
	    			public void cancel() {
	    				Logger.debug("Transfer canceled on channel: " + cid);
						dsc.complete();
	    				ServerHandler.this.context.close();
	    			}
	    			
	    			@Override
	    			public void nextChunk() {
	    				// meaningless in download
	    			}
	    			
	    			@Override
	    			public void message(StreamMessage msg) {
	    				int seqnum = (int) msg.getFieldAsInteger("Sequence", 0);
	    				
	    				if (seqnum != this.seq) {
	    					this.error(1, "Bad sequence number: " + seqnum);
	    					return;
	    				}
	    				
    					if (msg.hasData()) {
    						if (Logger.isDebug())
    							Logger.error("Transfer data: " + msg.getData().readableBytes());
    	    				
	    					//this.amt += msg.getData().readableBytes();
	    					HttpContent b = new DefaultHttpContent(Unpooled.copiedBuffer(msg.getData()));		// TODO not copied
	    					ServerHandler.this.context.sendDownload(b);
    					}
    					
    					this.seq++;

    					// TODO update progress
    					
    					if (msg.isFinal()) {
    						if (Logger.isDebug())
    							Logger.error("Transfer completed: " + msg.getData().readableBytes());
    	    				
	    					ServerHandler.this.context.sendDownload(new DefaultLastHttpContent());
		    				ServerHandler.this.context.close();
	    					dsc.complete();
    					}
	    			}
	    			
	    			public void error(int code, String msg) {
						if (Logger.isDebug())
							Logger.error("Transfer error - " + code + ": " + msg);
	    				
	    				dsc.send(MessageUtil.streamError(code, msg));
	    				ServerHandler.this.context.close();
	    			}
	    		});		

	    		// for some reason HyperSession is sending content. 
	    		this.context.setDecoder(new IContentDecoder() {					
					@Override
					public void release() {
					}
					
					@Override
					public void offer(HttpContent chunk) {
						if (!(chunk instanceof LastHttpContent))
							Logger.error("Unexplained and unwanted content during download: " + chunk);
					}
				});

	    		// tell the client that chunked content is coming
	    		this.context.sendDownloadHeaders(dsc.getPath() != null ? dsc.getPath().getFileName() : null, dsc.getMime());
	    		
				if (Logger.isDebug())
					Logger.error("Singal Transfer Start - " + cid);
	    		
	    		// get the data flowing
	    		dsc.send(new StreamMessage("Start"));
	    		
				return;
			}
	        
			if ((req.getPath().getNameCount() == 1) && req.getPath().getName(0).equals(ServerHandler.STATUS_PATH)) {
				if (Hub.instance.getState() == HubState.Running)
	                this.context.sendRequestOk();
				else
					this.context.sendRequestBad();
                
                return;
	        }
	        
			// "rpc" is it's own built-in extension.  all requests to rpc are routed through
			// DivConq bus, if the request is valid
	        if (req.pathEquals(ServerHandler.RPC_PATH)) {
	    		if (req.getMethod() != HttpMethod.POST) {
	                this.context.sendRequestBad();
	                return;
	    		}
	    		
	    		//System.out.println("looks like we have a rpc message");
	    		
	    		// max 4MB of json? -- TODO is that max chunk size or max total?  we don't need 4MB chunk... 
	            this.context.setDecoder(new HttpBodyRequestDecoder(4096 * 1024, new RpcHandler(this.context)));
	        	return;
	        }        
			
			// otherwise we need to figure out which extension is being called
			// "local" is also used to mean default extension
			String ext = req.pathEquals("/") ? "local" : req.getPath().getName(0);
			
			IWebExtension ex = "local".equals(ext) 
					? this.context.getSiteman().getDefaultExtension()
					: this.context.getSiteman().getExtension(ext);
			
			// still cannot figure it out, use default
			if (ex == null)
				ex = this.context.getSiteman().getDefaultExtension();
					
			// then have extension handle it
			if (ex != null) {
				//OperationResult res = new OperationResult();  
				
				if (Logger.isDebug())
					Logger.error("Request pasted to web extension: " + sess.getId());
				
				OperationResult res = ex.handle(sess, this.context);
				//resp.addBody("Hello");
				//this.context.send();
				
				// no errors starting page processing, return 
				if (!res.hasErrors())
					return;
				
				resp.setHeader("X-dcResultCode", res.getCode() + "");
				resp.setHeader("X-dcResultMesage", res.getMessage());
				this.context.sendNotFound();					
				return;
			}
		}
		catch (Exception x) {
			if (Logger.isDebug())
				Logger.error("Request triggered exception: " + sess.getId() + " - " + x);
			
			this.context.sendInternalError();
            return;
		}

        this.context.sendNotFound();
    }
}
