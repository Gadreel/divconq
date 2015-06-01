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
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.ServerCookieEncoder;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import divconq.bus.Message;
import divconq.hub.Hub;
import divconq.io.InputWrapper;
import divconq.io.OutputWrapper;
import divconq.lang.Memory;
import divconq.net.NetUtil;
import divconq.util.FileUtil;
import divconq.util.MimeUtil;
import divconq.util.StringUtil;
import divconq.www.http.parse.DateParser;

public class Response {
    protected Map<String, Cookie> cookies = new HashMap<>();
    protected boolean keepAlive = false; 
    protected Memory body = new Memory();
    protected Map<CharSequence, String> headers = new HashMap<>();
    protected HttpResponseStatus status = HttpResponseStatus.OK;
    
    protected PrintStream stream = null;

    public PrintStream getPrintStream() {
    	if (this.stream == null)
			try {
				this.stream = new PrintStream(new OutputWrapper(this.body), true, "UTF-8");
			} 
    		catch (UnsupportedEncodingException x) {
				// ignore, utf8 is supported
			}
    	
    	return this.stream;
    }
    
    public void setCookie(Cookie v) {
    	this.cookies.put(v.getName(), v);
    }
    
    public void setHeader(CharSequence name, String value) {
    	this.headers.put(name, value);
    }
    
    public void setDateHeader(CharSequence name, long value) {
    	DateParser parser = new DateParser();
    	this.headers.put(name, parser.convert(value));
    }
    
    public void setStatus(HttpResponseStatus v) {
		this.status = v;
	}
    
    public void setKeepAlive(boolean v) {
		this.keepAlive = v;
	}
    
    public void write(PrintStream out) {
        this.body.setPosition(0);
        this.body.copyToStream(out);
	}
    
    public void write(Channel ch) {
        if ((this.status != HttpResponseStatus.OK) && (this.body.getLength() == 0))
        	this.body.write(this.status.toString());
    	
        // Build the response object.
    	FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, this.status);

        int clen = 0;
        this.body.setPosition(0);
        
		try {
			clen = response.content().writeBytes(new InputWrapper(this.body), this.body.getLength());
		} 
		catch (IOException e) {
		}
    	
        response.headers().set(Names.CONTENT_TYPE, "text/plain; charset=UTF-8");

        if (this.keepAlive) {
            // Add 'Content-Length' header only for a keep-alive connection.
            response.headers().set(Names.CONTENT_LENGTH, clen);
        	
            // Add keep alive header as per:
            // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
            response.headers().set(Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        // Encode the cookies
        for (Cookie c : this.cookies.values()) 
	        response.headers().add(Names.SET_COOKIE, ServerCookieEncoder.encode(c));
        
        for (Entry<CharSequence, String> h : this.headers.entrySet())
        	response.headers().set(h.getKey(), h.getValue());
        
        Hub.instance.getSecurityPolicy().hardenHttpResponse(response);
        
        // Write the response.
        ChannelFuture future = ch.writeAndFlush(response);

        // Close the non-keep-alive connection after the write operation is done.
        if (!this.keepAlive) 
            future.addListener(ChannelFutureListener.CLOSE);
        
        /*  we do not need to sync - HTTP is one request, one response.  we would not pile messages on this channel
         * 
         *  furthermore, when doing an upload stream we can actually get locked up here because the "write" from our stream
         *  is locked on the write process of the data bus and the response to the session is locked on the write of the response
         *  here - but all the HTTP threads are busy with their respective uploads.  If they all use the same data bus session 
         *  then all HTTP threads can get blocked trying to stream upload if even one of those has called an "OK" to upload and
         *  is stuck here.  so be sure not to use sync with HTTP responses.  this won't be a problem under normal use.
         *   
        try {
			future.sync();
		} 
        catch (InterruptedException x) {
			// TODO should we close channel?
		}
		*/
    }
    
    public void writeStart(Channel ch, int contentLength) {
        // Build the response object.
    	HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, this.status);

        response.headers().set(Names.CONTENT_TYPE, "text/plain; charset=UTF-8");

        if (this.keepAlive) {
            // Add 'Content-Length' header only for a keep-alive connection.
            response.headers().set(Names.CONTENT_LENGTH, contentLength);
        	
            // Add keep alive header as per:
            // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
            response.headers().set(Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        // Encode the cookies
        for (Cookie c : this.cookies.values()) 
	        response.headers().add(Names.SET_COOKIE, ServerCookieEncoder.encode(c));
        
        for (Entry<CharSequence, String> h : this.headers.entrySet())
        	response.headers().set(h.getKey(), h.getValue());
        
        Hub.instance.getSecurityPolicy().hardenHttpResponse(response);
        
        // Write the response.
        ch.write(response);
    }
    
    public void writeEnd(Channel ch) {
        // Write the response.
        ChannelFuture future = ch.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

        // Close the non-keep-alive connection after the write operation is done.
        if (!this.keepAlive) 
            future.addListener(ChannelFutureListener.CLOSE);
        
        /*  we do not need to sync - HTTP is one request, one response.  we would not pile messages on this channel
         * 
         *  furthermore, when doing an upload stream we can actually get locked up here because the "write" from our stream
         *  is locked on the write process of the data bus and the response to the session is locked on the write of the response
         *  here - but all the HTTP threads are busy with their respective uploads.  If they all use the same data bus session 
         *  then all HTTP threads can get blocked trying to stream upload if even one of those has called an "OK" to upload and
         *  is stuck here.  so be sure not to use sync with HTTP responses.  this won't be a problem under normal use.
         *   
        try {
			future.sync();
		} 
        catch (InterruptedException x) {
			// TODO should we close channel?
		}
		*/
    }
    
    public void writeChunked(Channel ch) {
        // Build the response object.
    	HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, this.status);

        response.headers().set(Names.CONTENT_TYPE, "text/plain; charset=UTF-8");

        if (this.keepAlive) {
            // Add keep alive header as per:
            // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
            response.headers().set(Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }
        
        // TODO add a customer header telling how many messages are in the session adaptor's queue - if > 0

        // Encode the cookies
        for (Cookie c : this.cookies.values()) 
	        response.headers().add(Names.SET_COOKIE, ServerCookieEncoder.encode(c));
        
        for (Entry<CharSequence, String> h : this.headers.entrySet())
        	response.headers().set(h.getKey(), h.getValue());

    	response.headers().set(Names.TRANSFER_ENCODING, Values.CHUNKED);
        
        // Write the response.
        ChannelFuture future = ch.writeAndFlush(response);

        // Close the non-keep-alive connection after the write operation is done.
        if (!this.keepAlive) 
            future.addListener(ChannelFutureListener.CLOSE);
        
        /*  we do not need to sync - HTTP is one request, one response.  we would not pile messages on this channel
        try {
			future.sync();
		} 
        catch (InterruptedException x) {
			// TODO should we close channel?
		}
		*/
    }
    
    public void writeDownloadHeaders(Channel ch, String name, String mime) {
        // Build the response object.
    	HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, this.status);

        response.headers().set(Names.CONTENT_TYPE, StringUtil.isNotEmpty(mime) ? mime : MimeUtil.getMimeTypeForFile(name));
        
        if (StringUtil.isEmpty(name))
        	name = FileUtil.randomFilename("bin");
        
        response.headers().set("Content-Disposition", "attachment; filename=\"" + NetUtil.urlEncodeUTF8(name) + "\"");
        
		Cookie dl = new DefaultCookie("fileDownload", "true");
		dl.setPath("/");
        
        response.headers().add(Names.SET_COOKIE, ServerCookieEncoder.encode(dl));

        // Encode the cookies
        for (Cookie c : this.cookies.values()) 
	        response.headers().add(Names.SET_COOKIE, ServerCookieEncoder.encode(c));
        
        for (Entry<CharSequence, String> h : this.headers.entrySet())
        	response.headers().set(h.getKey(), h.getValue());

    	response.headers().set(Names.TRANSFER_ENCODING, Values.CHUNKED);
        
        // Write the response.
        ch.writeAndFlush(response);
    }
    
	public void load(ChannelHandlerContext ctx, HttpRequest req) {
		this.keepAlive = HttpHeaders.isKeepAlive(req);
	}
	
	public void loadVoid() {
	}

	public void setBody(Message m) {
		// TODO make more efficient
		// TODO cleanup the content of the message some for browser?
		this.body.write(m.toString());
	}

	public void addBody(String v) {
		this.body.write(v);
	}

	public void setBody(Memory v) {
		this.body = v;
	}
	
	public Memory getBody() {
		return this.body;
	}
}
