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

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import divconq.net.ssl.SslHandler;
import divconq.bus.HubRouter;
import divconq.hub.Hub;
import divconq.lang.op.OperationContext;
import divconq.log.Logger;
import divconq.util.KeyUtil;
import divconq.util.StringUtil;

public class StreamSession {
	protected Channel chan = null;
    protected HubRouter router = null;
    protected SocketInfo info = null;
	protected boolean isServerConnector = false;
    protected String mode = null;
	
    protected long written = 0;
    protected long read = 0;
    
    public long getWritten() {
		return this.written;
	}
    
    public long getRead() {
		return this.read;
	}
    
	public SocketInfo getSocketInfo() {
		return this.info;
	}
	
	public Channel getChannel() {
		return this.chan;
	}
	
	public void setChannel(Channel chan) {
		this.chan = chan;
	}
	
	public HubRouter getRouter() {
		return this.router;
	}
	
	public void setRouter(HubRouter router) {
		this.router = router;
	}
	
	public boolean isServerConnector() {
		return this.isServerConnector;
	}
	
	public String getSessionMode() {
		return this.mode;
	}
	
	public StreamSession(SocketInfo info, boolean isServer) {
		this.info = info;
		this.isServerConnector = isServer;
    	this.mode = isServer ? "Server" : "Client";
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
	
	// should already have TaskContext if needed (SERVICES and HELLO do not need task context)
	public boolean write(StreamMessage m) {
		try {
			if (this.chan != null) {
				Logger.trace("Stream session " + this + " sending message : " + m);
				
				//m.retain();
				ChannelFuture cf = this.chan.writeAndFlush(m).sync();		// we overrun the queue if we don't sync
				Logger.trace("Stream session " + this + " sent message : " + m);
								
				// record how much we wrote to this channel
				if (cf.isSuccess() && (m.getData() != null))
					this.written += m.getData().writerIndex();
						
				// if we get here there is a decent chance the message was sent (no proof, just good chance)
				return true;
			}
		}
		catch (Exception x) {
			Logger.error("Error writing stream message: " + m);
			Logger.error("Error writing stream message: " + x);
			
			x.printStackTrace();
			
			// TODO close channel ?
		}
		finally {
			//m.release();		// if the data is on the network we don't need the buffer anymore  -- release in stream encoder
		}
		
		Logger.error("Could not write stream message");
		
		return false;
	}
	
	public void keepAlive() {
		try {
			if (this.chan != null) {
				Logger.trace("Stream session keep alive");
				StreamMessage m = Hub.instance.getBus().getLocalHub().buildStreamHello(OperationContext.getHubId());
				this.chan.writeAndFlush(m);
			}
		}
		catch (Exception x) {
			System.out.println("Error writing keep alive stream message: " + x);
			// TODO close channel ?
		}
	}

	public String getAttribute(String string) {
		// TODO Auto-generated method stub
		return null;
	}

	public void closed() {
        if (this.router != null)
        	this.router.removeSession(this);
	}

	public boolean isInitialized() {
		return (this.router != null);
	}

	// TODO from this point on StreamMessage needs to be released 
	public void receiveMessage(StreamSession session, Channel ch, StreamMessage msg) {  
		OperationContext.useHubContext();
		
		Logger.trace("Stream session " + this + " got message : " + msg);
		
		// record how much we read from this channel
		if (msg.getData() != null)
			this.read += msg.getData().writerIndex();
		
    	if ("HELLO".equals(msg.getFieldAsString("Op"))) {
    		String rhid = msg.getFieldAsString("Id");
    		
            if (OperationContext.getHubId().equals(rhid)) {
            	System.out.println("dcBus stream " + this.getSessionMode() + " tried to connect to self, got: " + msg);
            	msg.release();
            	ch.close();		// don't stay with bad messages
            	return;
            }
    		
            String expectedhubid = this.info.getHubId();
            
            if (StringUtil.isNotEmpty(expectedhubid) && !expectedhubid.equals(rhid)) {
            	System.out.println("dcBus stream " + this.getSessionMode() + " tried to connect to " + expectedhubid + ", got: " + msg);
            	msg.release();
            	ch.close();		// don't stay with bad messages
            	return;
            }
    		
    		if (StringUtil.isDataInteger(rhid) && (rhid.length() == 5)) {
    			if (!this.isInitialized()) {
    				if (this.info.isUseSsl()) {
	        			// TODO maybe check the getPeerCertificateChain to see if the hubid and cert match up
	    	            
	    	            try {
	    	    	        // ensure that connection has a client cert - if so it has gone through trust manager so we don't 
	    	    	        // need to verify cert, just be sure there is one
	    	            	// if client does not present a cert we could still get this far
	    	            	SslHandler sh = (SslHandler) ch.pipeline().get("ssl");
	    	            	
	    	            	// TODO store in member var
	    	            	X509Certificate[] xcerts = sh.engine().getSession().getPeerCertificateChain();
	
	    	    	    	// TODO log not sysout
	    	    	    	for (X509Certificate xc : xcerts)
	    	    	    		System.out.println("confirmed cert is present: " + xc.getSubjectDN());
	    	    	    	
	    	    	    	if (StringUtil.isNotEmpty(this.info.getTargetthumbprint())) {
	    	    	    		String expected = this.info.getTargetthumbprint();
	    	    	    		String got = KeyUtil.getCertThumbprint(xcerts[0]);
	    	    	    		
	    	    	    		if (!expected.equals(got))
	    	    	    			throw new SSLPeerUnverifiedException("Certificate does not match expected thumbprint: " + got);
	    	    	    	}
	    	            }
	    	            catch (SSLPeerUnverifiedException x) {
	    	            	// TODO log, count, raise EVENT
	    	            	System.err.println("Peer Cert Error connecting dcBus " + this.getSessionMode() + ": " + x);
	    	            	ch.close();		// don't stay with bad messages
	    	            	return;
	    	            }
    				}

    				this.chan = ch;
    	            
    				this.router = Hub.instance.getBus().allocateOrGetHub(rhid, session.getSocketInfo().isGateway());
    				this.router.addSession(this);
    				
                    Logger.info("dcBus stream " + this.getSessionMode() + " Greeted!");

	    	    	// only server replies to HELLO, client started it
	    	    	if (this.isServerConnector) {    	    	    	
	    	        	// only the "server" side responds with HELLO
	    	    		
    	    	        // Send HELLO to server to initial sequence of identity and service indexing
    	    	        System.out.println("dcBus stream " + this.getSessionMode() + " sending HELLO");

    	    			StreamMessage icmd = Hub.instance.getBus().getLocalHub().buildStreamHello(rhid);
    	                this.write(icmd);
	    	    	}
    			}
    		}
    	}

        // only accept HELLO messages until we get a valid one
        if (!this.isInitialized()) {
        	System.out.println("dcBus stream " + this.getSessionMode() + " expceted HELLO message, got: " + msg);
        	ch.close();		// don't stay with bad messages
        	msg.release();
        	return;
        }
		
		this.router.receiveMessage(session, msg);
	}
}
