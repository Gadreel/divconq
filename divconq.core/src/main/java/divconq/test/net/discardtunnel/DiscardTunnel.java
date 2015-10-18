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

import java.util.concurrent.atomic.AtomicReference;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * Keeps tunneling random data to the specified address.
 */
public final class DiscardTunnel {
    static final String HOST = "::1";
    static final int CPORT = Integer.parseInt(System.getProperty("port", "8009"));
    static final int TPORT = Integer.parseInt(System.getProperty("port", "8010"));

    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        AtomicReference<SocketChannel> tunnel = new AtomicReference<SocketChannel>();
        AtomicReference<SocketChannel> client = new AtomicReference<SocketChannel>();
        
        try {
        	// client side
            ServerBootstrap b = new ServerBootstrap();
            
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler("client tunnel", LogLevel.INFO))
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) {
                     ChannelPipeline p = ch.pipeline();
                     
                     p.addLast(new SimpleChannelInboundHandler<Object>() {
                    	 @Override
                    	public void channelActive(ChannelHandlerContext ctx) throws Exception {
 							System.out.println("client tunnel active: ");
 							
                    		super.channelActive(ctx);
                    	}
                    	 
						@Override
						protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
							System.out.println("tunnel client message: " + msg);
							
							tunnel.get().writeAndFlush(msg);
						}
                    	 
					    @Override
					    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
					        // Close the connection when an exception is raised.
					        cause.printStackTrace();
					        ctx.close();
					    }
                     });
                     
                     client.set(ch);
                 }
             });

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(CPORT).sync();

            // tunnel side
            ServerBootstrap bt = new ServerBootstrap();
            
            bt.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler("server tunnel", LogLevel.INFO))
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) {
                     ChannelPipeline p = ch.pipeline();
                     p.addLast(new SimpleChannelInboundHandler<Object>() {
                    	 @Override
                    	public void channelActive(ChannelHandlerContext ctx) throws Exception {
                    		super.channelActive(ctx);
                    		
                    		System.out.println("server tunnel active");
                    	}
                    	 
						@Override
						protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
							System.out.println("server tunnel message: " + msg);
							
							if (client.get() == null)
								System.out.println("tunnel front should not get messages until client sends: " + msg);
							else
								client.get().writeAndFlush(msg);
						}
                    	 
					    @Override
					    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
					        // Close the connection when an exception is raised.
					        cause.printStackTrace();
					        ctx.close();
					    }
                     });
                     
                     tunnel.set(ch);
                 }
             });

            // Bind and start to accept incoming connections.
            ChannelFuture ft = bt.bind(TPORT).sync();
            
            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
            
            ft.channel().close();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
