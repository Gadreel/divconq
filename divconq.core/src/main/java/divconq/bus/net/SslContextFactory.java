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

import java.io.FileInputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;

import divconq.hub.Hub;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public final class SslContextFactory {
    static protected SSLContext ServerContext = null;
    static protected SSLContext ClientContext = null;

    static public void init(XElement config) {
    	if (config == null)
    		return;
        
        BusTrustManager tm = new BusTrustManager();
        tm.init(config);
        
        TrustManager[] trustManagers = new TrustManager[] { tm };
    	
    	XElement sslconfig = config.selectFirst("SslContext");
    	
    	if (sslconfig != null) {
	        String algorithm = sslconfig.getAttribute("Algorithm", "SunX509");
	        String protocol = sslconfig.getAttribute("Protocol", "TLSv1.2");
	        
	        String jksfile = sslconfig.getAttribute("File");
	        String jkspass = Hub.instance.getClock().getObfuscator().decryptHexToString(sslconfig.getAttribute("Password"));	
	        
	        if (StringUtil.isNotEmpty(jksfile))
		        try {
		        	// load keystore
		            KeyStore ks = KeyStore.getInstance("JKS");
		            ks.load(new FileInputStream(jksfile), jkspass.toCharArray());
		
		            KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
		            kmf.init(ks, jkspass.toCharArray());
		
		            // init server context
		            SSLContext serverContext = SSLContext.getInstance(protocol);
		            serverContext.init(kmf.getKeyManagers(), trustManagers, null);
		            
		            SslContextFactory.ServerContext = serverContext;
		            
		            // init client context
		            SSLContext clientContext = SSLContext.getInstance(protocol);
		            clientContext.init(kmf.getKeyManagers(), trustManagers, null);
		            
		            SslContextFactory.ClientContext = clientContext;
		        } 
		        catch (Exception x) {
		        	// TODO
		            throw new Error("Failed to initialize the SSLContext", x);
		        }
    	}
    }

    public static SSLContext getServerContext() {
        return SslContextFactory.ServerContext;
    }

	public static SSLEngine getServerEngine() {
        SSLEngine engine = SslContextFactory.ServerContext.createSSLEngine();
        engine.setUseClientMode(false);
        engine.setWantClientAuth(true);
		return engine;
	}

    public static SSLContext getClientContext() {
        return SslContextFactory.ClientContext;
    }

	public static SSLEngine getClientEngine() {
        SSLEngine engine = SslContextFactory.ClientContext.createSSLEngine();
        engine.setUseClientMode(true);
        return engine;
	}

    private SslContextFactory() {
    }
}