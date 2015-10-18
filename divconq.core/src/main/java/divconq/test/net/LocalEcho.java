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
package divconq.test.net;

import io.netty.channel.Channel;

public final class LocalEcho {
	static public Channel chan = null;
	
	public static void start() {
	        // Address to bind on / connect to.
	        //final LocalAddress addr = new LocalAddress(PORT);

	    	/*
	    	XElement config = new XElement("Bus", 
	    			new XElement("SslContext", 
	    					new XAttribute("Password", "A1s2d3f4"),
	    					new XAttribute("File", "./packages/dcTest/keys/backend.jks")
	    			),
	    			new XElement("Trust", new XAttribute("Thumbprint", "F7:15:BB:A0:68:01:B3:4A:C0:ED:19:58:26:77:1D:E8:98:13:27:AF"))
	    	);		  
	    	
	    	SslContextFactory.init(config);
	    	
	        EventLoopGroup el = new NioEventLoopGroup();
	        */
	    	
		/*
	        EventLoopGroup el = Hub.instance.getEventLoopGroup();
	    	
            // Note that we can use any event loop to ensure certain local channels
            // are handled by the same event loop thread which drives a certain socket channel
            // to reduce the communication latency between socket channels and local channels.
            ServerBootstrap sb = new ServerBootstrap();
            sb.group(el)
              .channel(NioServerSocketChannel.class)
              .childOption(ChannelOption.AUTO_READ, false)
              .handler(new ChannelInitializer<ServerSocketChannel>() {
                  @Override
                  public void initChannel(ServerSocketChannel ch) throws Exception {
                      //ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                  }
              })
              .childHandler(new ChannelInitializer<SocketChannel>() {
                  @Override
                  public void initChannel(SocketChannel ch) throws Exception {
                      ch.pipeline().addLast(
                    		 new SslHandler(SslContextFactory.getServerEngine()),
                             //new LoggingHandler(LogLevel.INFO),
                    		 //new CtpMessageDecoder(),
                    		 new StreamInboundHandler()
                              //new LocalEchoServerHandler()
                    	);
                  }
              });

            Bootstrap cb = new Bootstrap();
            cb.group(el)
              .channel(NioSocketChannel.class)
              .handler(new ChannelInitializer<SocketChannel>() {
                  @Override
                  public void initChannel(SocketChannel ch) throws Exception {
                      ch.pipeline().addLast(
                    		  new SslHandler(SslContextFactory.getClientEngine()),
                    		  
                              //new LoggingHandler(LogLevel.INFO),
                    		  new CtpMessageEncoder()
                    		  
                              //new LocalEchoClientHandler()
                    	);
                  }
              });

            try {
	            // Start the server.
	            sb.bind(8181).sync();
	
	            // Start the client.
	            Channel ch = cb.connect("localhost", 8181).sync().channel();
	            
	            LocalEcho.chan = ch;

	            LocalEcho.chan.writeAndFlush(new ClientHelloMessage());
            }
            catch (Exception x) {
            	System.out.println("Test dcBus could not start!");
            }
            */
	}
	
	public static void test1() {
		/*
        StreamMessage msg = new StreamMessage();
        
        msg.setPath("/one/two/three/done.txt");
        msg.setFileSize(909090);

        LocalEcho.chan.write(msg);
        
        msg = new StreamMessage();
        
        msg.setPath("/xyz/frog.txt");
        msg.setFileSize(4545);

        LocalEcho.chan.write(msg);
        
        LocalEcho.chan.flush();
        */
	}
	
	public static void test2() {
		/*
		FileSystemFile src = StreamUtil.localFile(Paths.get("c:/temp/test/source"));
		//FileSystemFile src = StreamUtil.localFile(Paths.get("c:/temp/test/betty_2013-10-06_FULL_S1_R1.sig"));
		//FileSystemFile dest = StreamUtil.localFile(Paths.get("c:/temp/testtar/test-files-0.tar.gz"));
		
		src.refreshProps();
		
		Task t = new Task()
			.withTitle("Streaming Out Test")
			.withTimeout(0);
		
		@SuppressWarnings("resource")
		TaskRun trun = StreamUtil.composeStream(t, 
				src.allocSrc(), 
				new TarStream().withNameHint("test-files-9"), 
				new CtpStreamDest(LocalEcho.chan));
		
		trun.addObserver(new TaskObserver() {
			@Override
			public void completed(TaskRun or) {
				System.out.println("Transfer Out is complete!!");
			}
		});
		
		Hub.instance.getWorkPool().submit(trun);
		*/
	}
	
    static final String PORT = System.getProperty("port", "test_port");

    public static void main(String[] args) throws Exception {
        // Address to bind on / connect to.
        //final LocalAddress addr = new LocalAddress(PORT);

    	/*
    	XElement config = new XElement("Bus", 
    			new XElement("SslContext", 
    					new XAttribute("Password", "A1s2d3f4"),
    					new XAttribute("File", "./packages/dcTest/keys/backend.jks")
    			),
    			new XElement("Trust", new XAttribute("Thumbprint", "F7:15:BB:A0:68:01:B3:4A:C0:ED:19:58:26:77:1D:E8:98:13:27:AF"))
    	);		  
    	
    	SslContextFactory.init(config);
    	
        EventLoopGroup el = new NioEventLoopGroup();
        */
    	
    	/*
        EventLoopGroup el = Hub.instance.getEventLoopGroup();
    	
        try {
            // Note that we can use any event loop to ensure certain local channels
            // are handled by the same event loop thread which drives a certain socket channel
            // to reduce the communication latency between socket channels and local channels.
            ServerBootstrap sb = new ServerBootstrap();
            sb.group(el)
              .channel(NioServerSocketChannel.class)
              .childOption(ChannelOption.AUTO_READ, false)
              .handler(new ChannelInitializer<ServerSocketChannel>() {
                  @Override
                  public void initChannel(ServerSocketChannel ch) throws Exception {
                      //ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                  }
              })
              .childHandler(new ChannelInitializer<SocketChannel>() {
                  @Override
                  public void initChannel(SocketChannel ch) throws Exception {
                      ch.pipeline().addLast(
                    		  new SslHandler(SslContextFactory.getServerEngine()) //,
                    		 //new CtpMessageDecoder() 
                              //new LoggingHandler(LogLevel.INFO),
                              //new LocalEchoServerHandler()
                    		 );
                  }
              });

            Bootstrap cb = new Bootstrap();
            cb.group(el)
              .channel(NioSocketChannel.class)
              .handler(new ChannelInitializer<SocketChannel>() {
                  @Override
                  public void initChannel(SocketChannel ch) throws Exception {
                      ch.pipeline().addLast(
                    		  new SslHandler(SslContextFactory.getClientEngine()),
                    		  
                    		  new CtpMessageEncoder(),
                    		  
                              //new LoggingHandler(LogLevel.INFO),
                              new LocalEchoClientHandler());
                  }
              });

            // Start the server.
            sb.bind(8181).sync();

            // Start the client.
            Channel ch = cb.connect("localhost", 8181).sync().channel();

            // Read commands from the stdin.
            System.out.println("Enter text (quit to end)");
            
            /*
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            
            for (;;) {
                String line = in.readLine();
                if (line == null || "quit".equalsIgnoreCase(line)) {
                    break;
                }

                // Sends the received line to the server.
                lastWriteFuture = ch.writeAndFlush(line);
            }
            * /

        	AtomicLong ctd = new AtomicLong(50000);
        	AtomicReference<ChannelFutureListener> cfl = new AtomicReference<>();
        	AtomicReference<Runnable> sr = new AtomicReference<>();
        	
        	cfl.set(new ChannelFutureListener() {					
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					long v = ctd.decrementAndGet();
					
					//System.out.println("Sending: " + v);
					
					if (future.isSuccess()) {
						if (v > 0)
							/*
							ch.writeAndFlush(StringUtil.buildSecurityCode(512)).addListener(new ChannelFutureListener() {

								@Override
								public void operationComplete(
										ChannelFuture future) throws Exception {
									System.out.println("got there!");
								}
							});
							* /
							//future.channel().writeAndFlush(StringUtil.buildSecurityCode(512)).addListener(cfl.get());
						
							//ch.writeAndFlush(StringUtil.buildSecurityCode(512)).addListener(cfl.get());
							ch.eventLoop().execute(sr.get());
						else 
							System.out.println("DONE sending! ");
					}
					else {
						System.out.println("ERROR sending - DONE sending!  " + future.cause());
					}	
				}
			});
        	
        	sr.set(new Runnable() {				
				@Override
				public void run() {
					String code = StringUtil.buildSecurityCode(512);
					
					ByteBuf b = Unpooled.wrappedBuffer(Utf8Encoder.encode(code));
					
					ch.writeAndFlush(b).addListener(cfl.get());
				}
			});
        	
        	//ch.eventLoop().n
        	
        	//ch.
        	
            // Sends the random string to the server.
        	//ch.writeAndFlush(StringUtil.buildSecurityCode(512)).addListener(cfl.get());
        	//ch.writeAndFlush(StringUtil.buildSecurityCode(512));
        	//ch.writeAndFlush(StringUtil.buildSecurityCode(512));
        	//ch.writeAndFlush(StringUtil.buildSecurityCode(512));
        	
        	ch.eventLoop().execute(sr.get());

            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            
            for (;;) {
                String line = in.readLine();
                if (line == null || "quit".equalsIgnoreCase(line)) {
                    break;
                }

                //System.out.println("Gotten: " + ((LocalEchoServerHandler) ch.pipeline().last()).gotten.get());
                
                // Sends the received line to the server.
                //lastWriteFuture = ch.writeAndFlush(line);
            }
        } 
        finally {
            el.shutdownGracefully();
        }
        */
    }
}