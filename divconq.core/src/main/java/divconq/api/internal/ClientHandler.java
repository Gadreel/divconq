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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import divconq.api.ClientInfo;
import divconq.api.ApiSession;
import divconq.api.ClientInfo.ConnectorKind;
import divconq.bus.Message;
import divconq.bus.MessageUtil;
import divconq.lang.Memory;
import divconq.lang.op.FuncResult;
import divconq.log.Logger;
import divconq.struct.CompositeParser;
import divconq.struct.CompositeStruct;
import divconq.struct.RecordStruct;
import divconq.web.HttpBodyRequestDecoder;
import divconq.web.IBodyCallback;
import divconq.web.IContentDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.CharsetUtil;

public class ClientHandler extends SimpleChannelInboundHandler<Object> {
    //protected WebSocketClientHandshaker handshaker = null;
    protected ClientInfo info = null;
    protected Channel chan = null;
    protected ApiSession session = null;
    protected Map<String, Cookie> cookies = new HashMap<>();
    protected IContentDecoder decoder = null;
    protected Semaphore wsready = new Semaphore(0);

    public void setChannel(Channel v) {
		this.chan = v;
	}
    
    public Map<String, Cookie> getCookies() {
		return this.cookies;
	}
    
    public ClientHandler(ApiSession session, ClientInfo info) {
    	this.session = session;
    	this.info = info;
    }
    
	public void send(Message msg) {
		Logger.debug("Sending message: " + msg);
		
		try {
			if (this.chan != null) {
				if (this.info.getKind() == ConnectorKind.WebSocket)
					this.chan.writeAndFlush(new TextWebSocketFrame(msg.toString()));
				else {
					DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, this.info.getPath());
					
					req.headers().set(Names.HOST, this.info.getHost());
					req.headers().set(Names.USER_AGENT, "DivConq HyperAPI Client 1.0");
					req.headers().set(Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
					req.headers().set(Names.CONTENT_ENCODING, "UTF-8");
					req.headers().set(Names.CONTENT_TYPE, "application/json; charset=utf-8");
		            req.headers().set(Names.COOKIE, ClientCookieEncoder.encode(this.cookies.values()));
				    
		            // TODO make more efficient - UTF8 encode directly to buffer
		            ByteBuf buf = Unpooled.copiedBuffer(msg.toString(), CharsetUtil.UTF_8);
		            int clen = buf.readableBytes();
		            req.content().writeBytes(buf);
		            buf.release();
		            
		            // Add 'Content-Length' header only for a keep-alive connection.
		            req.headers().set(Names.CONTENT_LENGTH, clen);
					
					this.chan.writeAndFlush(req);
				}
			}
		}
		catch (Exception x) {
			Logger.error("Send HTTP Message error: " + x);
		}
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		this.chan = ctx.channel();
		
		Logger.debug("ca");
		
		/*
		if (this.info.getKind() == ConnectorKind.WebSocket) {
	        HttpHeaders customHeaders = new DefaultHttpHeaders();
	        customHeaders.add("x-DivConq-Mode", Hub.instance.getResources().getMode());
	        
	        this.handshaker = WebSocketClientHandshakerFactory.newHandshaker(this.info.getUri(), WebSocketVersion.V13, null, false, customHeaders);
	        
			this.handshaker.handshake(this.chan);
	    }
	    */
	}
	
	@Override
	public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    	Logger.debug("Got object: " + msg);            

    	/*
    	if ((this.handshaker != null) && !this.handshaker.isHandshakeComplete()) {
    		HttpResponse httpres = (HttpResponse) msg;	    		
    		
        	DefaultFullHttpResponse freq = new DefaultFullHttpResponse(httpres.getProtocolVersion(), httpres.getStatus());
        	
        	freq.headers().add(httpres.headers());
        	
        	this.handshaker.finishHandshake(ctx.channel(), freq);
    		
        	Logger.info("Web Client connected!");            
        	
        	return;
    	}
    	*/
    	
	    if (msg instanceof HttpObject) 
	        this.handleHttpRequest(ctx, (HttpObject) msg);
	    else if (msg instanceof WebSocketFrame) 
	    	this.handleWebSocketFrame(ctx, (WebSocketFrame) msg);        
	}

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Logger.info("Web Client disconnected!");
        
        this.session.stopped();
    }

    public void handleHttpRequest(ChannelHandlerContext ctx, HttpObject obj) throws Exception {
    	Logger.debug("got http message " + obj);
    	
    	if (obj instanceof HttpContent) {
    		if (this.decoder == null) {
    			Logger.error("Got chunk before getting headers!");	
    			return;
    		}
    		
    		this.decoder.offer((HttpContent)obj);
    		return;
    	}    	

    	if (!(obj instanceof HttpResponse)) {
			Logger.error("Got unknown instead of headers!");	
            return;
        }

    	HttpResponse resp = (HttpResponse) obj;

    	// keep the cookies - especially Session!
        List<String> cookies = resp.headers().getAll(Names.SET_COOKIE);
        
        for (String cookie : cookies) {
        	Set<Cookie> cset = CookieDecoder.decode(cookie);
        	
        	for (Cookie c : cset) 
        		this.cookies.put(c.getName(), c);
        }
        
        this.decoder = new HttpBodyRequestDecoder(4096 * 1024, new IBodyCallback() {			
			@Override
			public void ready(Memory mem) {
				// if response is empty ignore
				if (mem.getLength() == 0)
					return;
				
				FuncResult<CompositeStruct> pres = CompositeParser.parseJson(mem);
				
				if (pres.hasErrors()) {
					Logger.error("Error parsing response JSON!");  
					return;
				}
				
				CompositeStruct croot = pres.getResult();
				
				if ((croot == null) || !(croot instanceof RecordStruct)) {
					Logger.error("Error parsing response JSON!");  
					return;
				}
				
		        ClientHandler.this.session.receiveMessage(MessageUtil.fromRecord((RecordStruct) croot));
			}
			
			@Override
			public void fail() {
				Logger.error("Failure processing http response");
			}
		});
    }

    public void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        Channel ch = ctx.channel();
        
        Logger.debug("ws1: " + frame);
        
        if (frame instanceof TextWebSocketFrame) {
        	String data = ((TextWebSocketFrame) frame).text();
        	
        	Logger.debug("Web client received message: " + data);
            
            FuncResult<CompositeStruct> res = CompositeParser.parseJson(data);
            
            if (res.hasErrors()) {
            	Logger.error("Web client got a bad message: " + res.getMessage());
            	ch.close();		// don't stay with bad messages
            	return;
            }

            this.session.receiveMessage(MessageUtil.fromRecord((RecordStruct)res.getResult()));      
            return;
        }
        
        Logger.error("unhandled frame type");

        // unhandled frame type drop connection
        ch.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    	super.userEventTriggered(ctx, evt);
    	
    	Logger.debug("ue: " + evt);
    	
    	if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
    		Logger.debug("handshake done");
            //this.handshaker = null;
            this.wsready.release();
    	}
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    	Logger.error("Error with api session channel: " + cause);
        ctx.close();
    }

	public void close() {
		try {
			if (this.chan != null)
				this.chan.close().await(2000);
		} 
		catch (InterruptedException x) {
			// ignore 
		}
	}

	public void waitConnect() {
    	if (this.info.getKind() == ConnectorKind.WebSocket)
			try {
				this.wsready.acquire();
			} 
    		catch (InterruptedException x) {
			}
	}
}
