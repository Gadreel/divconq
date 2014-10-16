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
package divconq.bus.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.CharsetUtil;

import divconq.bus.MessageUtil;
import divconq.lang.FuncResult;
import divconq.struct.CompositeParser;
import divconq.struct.CompositeStruct;
import divconq.struct.RecordStruct;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;

/**
 * Handles handshakes and messages
 */
abstract public class CommonHandler extends SimpleChannelInboundHandler<Object> {
	static protected final String BUS_PATH = "/dcBus";

    protected Session session = null;

    public CommonHandler(SocketInfo info, boolean isServer) {
    	this.session = new Session(info, isServer);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    	// TODO logging
        System.out.println("dcBus " + this.session.getSessionMode() + " disconnected!");
        
        this.session.closed();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) 
            this.handleHttpRequest(ctx, (FullHttpRequest) msg);
        else if (msg instanceof WebSocketFrame) 
        	this.handleWebSocketFrame(ctx, (WebSocketFrame) msg);        
    }

    public void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
    	// TODO logging
        FullHttpResponse response = (FullHttpResponse) req;
        
        throw new Exception("dcBus Unexpected FullHttpResponse (getStatus=" + response.getStatus() + ", content="
                + response.content().toString(CharsetUtil.UTF_8) + ')');
    }

    public void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        Channel ch = ctx.channel();
        
        // Check for closing frame
        if (frame instanceof CloseWebSocketFrame) {
            System.out.println("dcBus " + this.session.getSessionMode() + " received close");
            ch.close();
            return;
        }
        
        if (frame instanceof PingWebSocketFrame) {
            System.out.println("dcBus " + this.session.getSessionMode() + " received ping");
            ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        
        if (frame instanceof PongWebSocketFrame) {
            System.out.println("dcBus " + this.session.getSessionMode() + " received pong");
            return;
        }
        
        if (frame instanceof TextWebSocketFrame) {
        	String data = ((TextWebSocketFrame) frame).text();
        	
            //System.out.println("dcBus " + this.session.getSessionMode() + " received message: " + data);
            
            FuncResult<CompositeStruct> res = CompositeParser.parseJson(data);
            
            if (res.hasErrors()) {
            	// TODO logging
            	System.out.println("dcBus " + this.session.getSessionMode() + " got a bad message: " + res.getMessage());
            	ch.close();		// don't stay with bad messages
            	return;
            }

            this.session.receiveMessage(this.session, ch, MessageUtil.fromRecord((RecordStruct)res.getResult()));      
            return;
        }

        // TODO unhandled frame type
        // TODO logging
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    	// TODO logging
        cause.printStackTrace();        
        ctx.close();
    }

    public static String getWebSocketLocation(FullHttpRequest req) {
        return "wss://" + req.headers().get(HOST) + BUS_PATH;
    }
}
