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
package divconq.api.internal;

import java.io.FileInputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;

import divconq.hub.Hub;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public final class ApiSslContextFactory {
    protected SSLContext clientContext = null;

    public void init(XElement config) {
    	if (config == null)
    		return;
        
        ApiTrustManager tm = new ApiTrustManager();
        tm.init(config);
        
        TrustManager[] trustManagers = new TrustManager[] { tm };
    	
    	XElement sslconfig = config.selectFirst("SslContext");
    	
    	if (sslconfig == null)
    		sslconfig = new XElement("FakeContext");
    	
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
	            
	            // init client context
	            SSLContext clientContext = SSLContext.getInstance(protocol);
	            clientContext.init(kmf.getKeyManagers(), trustManagers, null);
	            
	            this.clientContext = clientContext;
	        } 
	        catch (Exception x) {
	        	// TODO
	            throw new Error("Failed to initialize the SSLContext", x);
	        }
        else 
        	try {
	            // init client context
	            SSLContext clientContext = SSLContext.getInstance(protocol);
	            clientContext.init(null, trustManagers, null);
	            
	            this.clientContext = clientContext;
	        }
        	catch (Exception x) {
        		// TODO
        		throw new Error("Failed to initialize the SSLContext", x);
        	}
    }

	public SSLEngine getClientEngine() {
        SSLEngine engine = this.clientContext.createSSLEngine();
        engine.setUseClientMode(true);
        return engine;
	}
}