/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package divconq.test.net.discardtunnel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

/**
 * Handles a server-side channel.
 */
public class DiscardServerHandler extends SimpleChannelInboundHandler<Object> {
	protected int count = 0;
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		
		System.out.println("server active");
		
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        
        // Configure SSL.
        final SslContext sslCtx =     SslContextBuilder
            		.forServer(ssc.certificate(), ssc.privateKey())
            		.build();
		
		ctx.channel().pipeline().addBefore("handler", "ssl", sslCtx.newHandler(ctx.channel().alloc()));
	}
	
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        // discard
    	
    	ByteBuf bb = (ByteBuf)msg;
    	
    	System.out.println(System.currentTimeMillis() + " - got chunk: " + bb.readableBytes());
    	
    	if (this.count == 255) {
    		System.out.println("*");
    		this.count = 0;
    	}
    	else {
    		System.out.print(".");
    		this.count++;
    	}
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
