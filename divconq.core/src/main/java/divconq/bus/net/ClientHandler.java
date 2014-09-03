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

import divconq.bus.Message;
import divconq.hub.Hub;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

public class ClientHandler extends CommonHandler {
    protected WebSocketClientHandshaker handshaker = null;

    public ClientHandler(SocketInfo info) {
    	super(info, false);
    	
        HttpHeaders customHeaders = new DefaultHttpHeaders();
        customHeaders.add("x-DivConq-Layer", "dcPrivate");

        this.handshaker = WebSocketClientHandshakerFactory.newHandshaker(info.getUri(), WebSocketVersion.V13, null, false, customHeaders);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
    	this.handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();
        
    	// TODO logging
        if (!this.handshaker.isHandshakeComplete()) {
        	this.handshaker.finishHandshake(ch, (FullHttpResponse) msg);
            System.out.println("dcBus Client connected!");            
	        
	        // Send HELLO to server to initial sequence of identity and service indexing
	        System.out.println("dcBus Client sending HELLO");

			Message icmd = Hub.instance.getBus().getLocalHub().buildHello(this.session.getSocketInfo().getHubId());
            ch.writeAndFlush(new TextWebSocketFrame(icmd.toString()));
            
            return;
        }
    	
    	super.channelRead0(ctx, msg);
    }
}
