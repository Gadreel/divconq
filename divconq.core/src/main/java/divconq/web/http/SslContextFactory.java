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

import io.netty.handler.ssl.OpenSsl;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

import divconq.hub.Hub;
import divconq.log.Logger;
import divconq.util.KeyUtil;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class SslContextFactory {
    protected SSLContext serverContext = null;
    protected String sslclientauth = null;
    protected List<String> keynames = new ArrayList<>();
	private List<XElement> clientcerts  = null;

    public boolean keynameMatch(String name) {
    	for (String kname : this.keynames)
    		if (kname.equals(name))
    			return true;
    	
    	int p = name.indexOf('.');
    	
    	name = name.substring(p + 1);
    	
    	for (String kname : this.keynames)
    		if (kname.endsWith(name))
    			return true;
    	
    	return false;
    }
    
    public void init(XElement config, XElement sslconfig) {
    	if (config == null)
    		return;

    	this.clientcerts   = config.selectAll("ClientCert");
    	
        WebTrustManager tm = new WebTrustManager();
        tm.init(config);
        
        TrustManager[] trustManagers = new TrustManager[] { tm };
    	
        this.sslclientauth = config.getAttribute("SslClientAuth", "None");		// Want, Need, None
    	
        this.init(sslconfig, null, trustManagers);
    }
    
    public void init(XElement sslconfig, String prepath, TrustManager[] trustManagers) {
    	if (sslconfig == null) 
    		return;
    	
        String algorithm = sslconfig.getAttribute("Algorithm", "SunX509");
        String protocol = sslconfig.getAttribute("Protocol", "TLSv1.2");
        
        String jksfile = sslconfig.getAttribute("File");
        
        if (StringUtil.isNotEmpty(prepath))
        	jksfile = prepath + jksfile;
        
        String jkspass = sslconfig.getAttribute("Password");

        // try to decrypt, if we succeed then we use it - if we don't then use plain text password
        String jkspw = Hub.instance.getClock().getObfuscator().decryptHexToString(jkspass);	
        
        if (jkspw != null)
        	jkspass = jkspw;
        
        if (StringUtil.isEmpty(jksfile)) 
        		return;
        
        try {
        	// load keystore
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(jksfile), jkspass.toCharArray());
            
            //if (Logger.isDebug()) {
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
			          
			          X500Name x500name = new JcaX509CertificateHolder(cert).getSubject();
			          RDN cn = x500name.getRDNs(BCStyle.CN)[0];
			          String scn = IETFUtils.valueToString(cn.getFirst().getValue());
			          
		              //Logger.info("Key: " + subject + " : " + thumbprint);
		              Logger.debug("Key: " + alias + " Subject: " + subject + " Thumbprint: " + thumbprint);
		              
		              this.keynames.add(scn);
		              
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
            //}
            
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
            kmf.init(ks, jkspass.toCharArray());

            // init server context
            SSLContext serverContext = SSLContext.getInstance(protocol);
            serverContext.init(kmf.getKeyManagers(), trustManagers, null);
            
            if (Logger.isTrace()) {
            	Logger.trace("TLS Provider: " + serverContext.getProvider().getName());
            	Logger.trace("TLS Protocol: " + serverContext.getProtocol());
            	
            	SSLServerSocketFactory sfactory = serverContext.getServerSocketFactory();
            	
            	Logger.trace("Default Suites");
		        
		        for (String p : sfactory.getDefaultCipherSuites())
		        	Logger.trace("Suite: " + p);
		        
		        Logger.trace("Supported Suites");
		        
		        for (String p : sfactory.getSupportedCipherSuites())
		        	Logger.trace("Suite: " + p);
            }
            
            this.serverContext = serverContext;
            
            Logger.info("OpenSSL in use (web): " + OpenSsl.isAvailable());
        } 
        catch (Exception x) {
        	// TODO
            throw new Error("Failed to initialize the SSLContext", x);
        }    	
    }

    public SSLContext getServerContext() {
        return this.serverContext;
    }

	public SSLEngine getServerEngine(String hostname) {
        SSLEngine engine = this.serverContext.createSSLEngine();
        engine.setUseClientMode(false);
        
        if ("Want".equals(this.sslclientauth))
        	engine.setWantClientAuth(true);
        
        if ("Need".equals(this.sslclientauth))
        	engine.setNeedClientAuth(true);
        
        if (this.clientcerts != null) {
        	for (XElement ccel : this.clientcerts) {
        		if (hostname.endsWith(ccel.getAttribute("Names"))) {
        			String cauth = ccel.getAttribute("SslClientAuth", "None");
        	        
        	        if ("Want".equals(cauth))
        	        	engine.setWantClientAuth(true);
        	        
        	        if ("Need".equals(cauth))
        	        	engine.setNeedClientAuth(true);
            		
            		break;
        		}
        	}
        }
        
        Hub.instance.getSecurityPolicy().hardenPublic(engine);
        
		return engine;
	}
}