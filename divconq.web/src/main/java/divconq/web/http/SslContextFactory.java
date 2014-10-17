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
package divconq.web.http;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;

import divconq.hub.Hub;
import divconq.log.Logger;
import divconq.util.KeyUtil;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public final class SslContextFactory {
    protected SSLContext serverContext = null;
    protected String sslclientauth = null;

    public void init(XElement config) {
    	if (config == null)
    		return;
        
        WebTrustManager tm = new WebTrustManager();
        tm.init(config);
        
        TrustManager[] trustManagers = new TrustManager[] { tm };
    	
        this.sslclientauth = config.getAttribute("SslClientAuth", "None");		// Want, Need, None
        
    	XElement sslconfig = config.selectFirst("SslContext");
    	
    	if (sslconfig != null) {
	        String algorithm = sslconfig.getAttribute("Algorithm", "SunX509");
	        String protocol = sslconfig.getAttribute("Protocol", "TLSv1.2");
	        
	        String jksfile = sslconfig.getAttribute("File");
	        
	        String jkspass = sslconfig.getAttribute("Password");
	        
	        // try to decrypt, if we succeed then we use it - if we don't then use plain text password
	        String jkspw = Hub.instance.getClock().getObfuscator().decryptHexToString(jkspass);	
	        
	        if (jkspw != null)
	        	jkspass = jkspw;
	        
	        if (StringUtil.isNotEmpty(jksfile))
		        try {
		        	// load keystore
		            KeyStore ks = KeyStore.getInstance("JKS");
		            ks.load(new FileInputStream(jksfile), jkspass.toCharArray());
		            
		            if (Logger.isDebug()) {
			            Enumeration<String> aliases = ks.aliases();
			            
			            Logger.debug("Certs and keys in web server key store:");
			            
			            while (aliases.hasMoreElements()) {
			              String alias = aliases.nextElement();
			              
			              if (ks.isCertificateEntry(alias)) {
				              Entry e = ks.getEntry(alias, null);
			            	  KeyStore.TrustedCertificateEntry certentry = (KeyStore.TrustedCertificateEntry) e;
			            	  //Logger.info("Trusted Cert: " + alias + " " + certentry.getTrustedCertificate().getType());
				              
				              CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
				              InputStream in = new ByteArrayInputStream(certentry.getTrustedCertificate().getEncoded());
				              X509Certificate cert = (X509Certificate)certFactory.generateCertificate(in);				              
				              
					          String subject = cert.getSubjectDN().toString();
					          String thumbprint = KeyUtil.getCertThumbprint(cert);  
				              
				              Logger.debug("Trusted Cert: " + alias + " Subject: " + subject + " Thumbprint: " + thumbprint);
			              }
			              else if (ks.isKeyEntry(alias)) {
				              Entry e = ks.getEntry(alias, new KeyStore.PasswordProtection(jkspass.toCharArray()));
			            	  KeyStore.PrivateKeyEntry keyentry = (KeyStore.PrivateKeyEntry) e;
				              
				              //String thumb = HashUtil.getCertThumbprint(keyentry.getCertificate());
				              
				              //Logger.info("Key: " + thumb + " / " + keyentry.getPrivateKey().getFormat() + " / " + keyentry.getPrivateKey().getAlgorithm());
				              
				              CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
				              InputStream in = new ByteArrayInputStream(keyentry.getCertificate().getEncoded());
				              X509Certificate cert = (X509Certificate)certFactory.generateCertificate(in);				              
				              
					          String subject = cert.getSubjectDN().toString();
					          String thumbprint = KeyUtil.getCertThumbprint(cert);  
				              
				              //Logger.info("Key: " + subject + " : " + thumbprint);
				              Logger.debug("Key: " + alias + " Subject: " + subject + " Thumbprint: " + thumbprint);
				              
				            	  /*
					              if ((key instanceof PrivateKey) && "PKCS#8".equals(key.getFormat())) {
					                // Most PrivateKeys use this format, but check for safety. 
					                try (FileOutputStream os = new FileOutputStream(alias + ".key")) {
					                  os.write(key.getEncoded());
					                  os.flush();
					                }
					              }
				            	   */
			              }
			            }
		            }
		            
		            KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
		            kmf.init(ks, jkspass.toCharArray());
		
		            // init server context
		            SSLContext serverContext = SSLContext.getInstance(protocol);
		            serverContext.init(kmf.getKeyManagers(), trustManagers, null);
		            
		            this.serverContext = serverContext;
		        } 
		        catch (Exception x) {
		        	// TODO
		            throw new Error("Failed to initialize the SSLContext", x);
		        }
    	}
    }

    public SSLContext getServerContext() {
        return this.serverContext;
    }

	public SSLEngine getServerEngine() {
        SSLEngine engine = this.serverContext.createSSLEngine();
        engine.setUseClientMode(false);
        
        if ("Want".equals(this.sslclientauth))
        	engine.setWantClientAuth(true);
        
        if ("Need".equals(this.sslclientauth))
        	engine.setNeedClientAuth(true);
        
        Hub.instance.harden(engine);
        
		return engine;
	}
}