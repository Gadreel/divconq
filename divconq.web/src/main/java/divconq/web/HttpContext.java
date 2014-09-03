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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import divconq.bus.Message;
import divconq.session.Session;
import divconq.util.MimeUtil;
import divconq.xml.XElement;

// TODO combine with WebContext??
public class HttpContext {
	protected Channel chan = null;
    protected XElement config = null;
    
    // used with HTTP only, not WS
	protected IContentDecoder decoder = null;    	
    protected Request request = null;
    protected Response response = null;
    protected Session session = null;
    
    protected boolean isWebsocket = false;
    
    public Request getRequest() {
		return this.request;
	}
    
    public Response getResponse() {
		return this.response;
	}
    
    public void setSession(Session v) {
		this.session = v;
	}
    
    public Session getSession() {
		return this.session;
	}
    
    public void isWebSocket(boolean v) {
    	this.isWebsocket = v;
    }
    
    public boolean isWebSocket() {
    	return this.isWebsocket;
    }
    
	public void load(ChannelHandlerContext ctx, HttpRequest req) {
		IContentDecoder d = this.decoder;
		
		if (d != null) {
			d.release();  
			this.decoder = null;
		}
		
        this.request = new Request();
        this.request.load(ctx, req);
        
        this.response = new Response();
        this.response.load(ctx, req);
	}
	
	public XElement getConfig() {
		return this.config;
	}
	
	public Channel getChannel() {
		return this.chan;
	}
	
	public void setChannel(Channel chan) {
		this.chan = chan;
	}
    
    public void setDecoder(IContentDecoder v) {
		this.decoder = v;
	}
	
	public HttpContext(XElement config) {
		this.config = config;
	}
	
	public void offerContent(HttpContent v) {
		this.session.touch();
		
		IContentDecoder d = this.decoder;
		
		if (d != null) 
			d.offer(v);
		
		// TODO in netty 5 alpha 1 this is getting called after each normal http get and is forcing us to close everytime
		// we may want to review later
		
		//else
		//	this.sendBadRequest();
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
	
	public void sendNotFound() {
		if (this.response != null) {
			this.response.setStatus(HttpResponseStatus.NOT_FOUND);
			this.send();
		}
	}
	
	public void sendForbidden() {
		if (this.response != null) {
			this.response.setStatus(HttpResponseStatus.FORBIDDEN);
			this.response.setKeepAlive(false);
			this.send();
		}
	}
	
	public void sendInternalError() {
		if (this.response != null) {
			this.response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
			this.response.setKeepAlive(false);
			this.send();
		}
	}
	
	public void sendRequestBad() {
		if (this.response != null) {
			this.response.setStatus(HttpResponseStatus.BAD_REQUEST);
			this.response.setKeepAlive(false);
			this.send();
		}
	}
	
	public void sendRequestOkClose() {
		if (this.response != null) {
			this.response.setStatus(HttpResponseStatus.OK);
			this.response.setKeepAlive(false);
			this.send();
		}
	}
	
	public void sendRequestOk() {
		if (this.response != null) {
			this.response.setStatus(HttpResponseStatus.OK);
			//this.response.setKeepAlive(true);
			this.send();
		}
	}
	
	public void send() {
		//if ((this.chan != null) && this.chan.isWritable() && (this.response != null)) 
		if ((this.chan != null) && (this.response != null)) 
			this.response.write(this.chan);
	}
	
	public void sendChunked() {
		//if ((this.chan != null) && this.chan.isWritable() && (this.response != null)) 
		if ((this.chan != null) && (this.response != null)) 
			this.response.writeChunked(this.chan);
	}
	
	public void sendDownloadHeaders(String name, String mime) {
		//if ((this.chan != null) && this.chan.isWritable() && (this.response != null)) 
		if ((this.chan != null) && (this.response != null)) 
			this.response.writeDownloadHeaders(this.chan, name, mime);
	}
	
	// should already have TaskContext if needed (SERVICES and HELLO do not need task context)
	public void send(Message m) {
		try {
			//if ((this.chan != null) && this.chan.isWritable()) {
			if (this.chan != null) {
				if (this.isWebsocket)
					this.chan.writeAndFlush(new TextWebSocketFrame(m.toString()));  //.sync();   we do not need to sync - HTTP is one request, one response.  we would not pile messages on this channel
				else {
					// include the version hash for the current deployed files
					m.setField("DeployVersion", WebSiteManager.instance.getVersion());
					
					// we are always using UTF 8, charset is required with any "text/*" mime type that is not ANSI text 
					this.response.setHeader(Names.CONTENT_TYPE, MimeUtil.getMimeType("json") + "; charset=utf-8");
					
					// TODO enable CORS - http://www.html5rocks.com/en/tutorials/file/xhr2/
					// TODO possibly config to be more secure for some users - see CORS handler in Netty 
					this.response.setHeader(Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
					
					this.response.setBody(m);
					this.response.write(this.chan);
				}
			}
		}
		catch (Exception x) {
		}
	}
	
	public void send(HttpContent chunk) {
		try {
			if ((this.chan != null)) {
				this.chan.writeAndFlush(chunk);   // we do not need to sync - HTTP is one request, one response.  we would not pile messages on this channel
			}
		}
		catch (Exception x) {
		}
	}
	
	public void sendDownload(HttpContent chunk) {
		try {
			if ((this.chan != null)) 
				this.chan.writeAndFlush(chunk).sync();    // for downloads we do need sync so we don't overwhelm client
		}
		catch (Exception x) {
		}
	}

	public void closed() {
		
		IContentDecoder d = this.decoder;
		
		if (d != null) 
			d.release();
	}
}
