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

import divconq.log.Logger;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class StreamHandler extends SimpleChannelInboundHandler<StreamMessage> {
    protected StreamSession session = null;

    public StreamHandler(SocketInfo info, boolean asServer) {
    	this.session = new StreamSession(info, asServer);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Logger.info("dcBus " + this.session.getSessionMode() + " stream disconnected!");
        
        this.session.closed();
    }
    
    @Override
    public void channelRead0(ChannelHandlerContext ctx, StreamMessage msg) throws Exception {
        this.session.receiveMessage(this.session, ctx.channel(), msg);      
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    	// TODO logging
        cause.printStackTrace();        
        ctx.close();
    }
}
