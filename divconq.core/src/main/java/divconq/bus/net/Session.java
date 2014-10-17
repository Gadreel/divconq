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
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import divconq.net.ssl.SslHandler;
import divconq.bus.Message;
import divconq.bus.HubRouter;
import divconq.hub.Hub;
import divconq.lang.OperationContext;
import divconq.log.Logger;
import divconq.util.KeyUtil;
import divconq.util.StringUtil;

public class Session {
	protected Channel chan = null;
    protected HubRouter router = null;
    protected SocketInfo info = null;
	protected boolean isServerConnector = false;
    protected String mode = null;
	
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
	
	public Session(SocketInfo info, boolean isServer) {
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
	public boolean write(Message m) {
		try {
			//if ((this.chan != null) && this.chan.isWritable()) {
			if (this.chan != null) {
				this.chan.writeAndFlush(new TextWebSocketFrame(m.toString()));   // TODO shouldn't need this.sync();		// TODO prefer another approach that doesn't block...
				
				// if we get here there is a decent chance the message was sent (no proof, just good chance)
				return true;
			}
		}
		catch (Exception x) {
			Logger.error("Error writing bus message: " + m);
			Logger.error("Error writing bus message: " + x);
			
			x.printStackTrace();
			
			// TODO close channel ?
		}
		
		Logger.error("Could not write bus message");
		
		return false;
	}
	
	public void keepAlive() {
		try {
			if (this.chan != null) {
				Logger.trace("dcBus session keep alive");
				Message m = Hub.instance.getBus().getLocalHub().buildHello(OperationContext.getHubId());
				this.chan.writeAndFlush(new TextWebSocketFrame(m.toString()));
			}
		}
		catch (Exception x) {
			System.out.println("Error writing keep alive message: " + x);
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

	public void receiveMessage(Session session, Channel ch, Message msg) {        
    	if ("HELLO".equals(msg.getFieldAsString("Kind"))) {
    		String rhid = msg.getFieldAsString("Id");
    		
            if (OperationContext.getHubId().equals(rhid)) {
            	System.out.println("dcBus " + this.getSessionMode() + " tried to connect to self, got: " + msg);
            	ch.close();		// don't stay with bad messages
            	return;
            }
    		
            String expectedhubid = this.info.getHubId();
            
            if (StringUtil.isNotEmpty(expectedhubid) && !expectedhubid.equals(rhid)) {
            	System.out.println("dcBus " + this.getSessionMode() + " tried to connect to " + expectedhubid + ", got: " + msg);
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
    	            
    				this.router = Hub.instance.getBus().allocateOrGetHub(rhid);
    				this.router.addSession(this);

    				this.chan = ch;
    				
                    System.out.println("dcBus " + this.getSessionMode() + " Greeted!");

	    	    	// only server replies to HELLO, client started it
	    	    	if (this.isServerConnector) {    	    	    	
    	    	        // Send HELLO to server to initial sequence of identity and service indexing
    	    	        System.out.println("dcBus " + this.getSessionMode() + " sending HELLO");

    	    			Message icmd = Hub.instance.getBus().getLocalHub().buildHello(rhid);
    	                this.write(icmd);
	    	    	}
    			}
    		}
    	}

        // only accept HELLO messages until we get a valid one
        if (!this.isInitialized()) {
        	System.out.println("dcBus " + this.getSessionMode() + " expceted HELLO message, got: " + msg);
        	ch.close();		// don't stay with bad messages
        	return;
        }
		
		this.router.receiveMessage(session, msg);
	}
}
