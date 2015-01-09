package divconq.hub;

import io.netty.handler.codec.http.HttpResponse;

import javax.net.ssl.SSLEngine;

import divconq.lang.op.OperationContext;
import divconq.log.Logger;
import divconq.struct.Struct;
import divconq.util.StringUtil;
import divconq.xml.XAttribute;
import divconq.xml.XElement;

public class SecurityPolicy {
	public void hardenHttpResponse(HttpResponse resp) {
		OperationContext ctx = OperationContext.get();
		
		String did = ctx.getUserContext().getDomainId();
		
		if (StringUtil.isNotEmpty(did)) {
			DomainInfo domain = Hub.instance.getDomainInfo(did);
			
			XElement config = domain.getSettings();
			
			XElement http = null;
			
			if (config != null)
				http = config.selectFirst("Harden/Http");
			
			this.hardenHttpResponseConfig(resp, http, false);
		}
		
		HubResources resources = Hub.instance.getResources(); 
		
		// impossible, so something is wrong with Hub just skip this
        if (resources == null)
        	return;
		
		XElement http = resources.getConfig().selectFirst("Harden/Http");
		
		this.hardenHttpResponseConfig(resp, http, true);
	}
	
	public void hardenHttpResponseConfig(HttpResponse resp, XElement http, boolean root) {
		// domains don't insert defaults, only root does
		if (!root && (http == null))
			return;

		// if root and no config, provide a default config
		if (http == null) 
			http = new XElement("Http", new XAttribute("Hsts", "SelfPlus"));
		
		XElement csp = http.find("ContentSecurityPolicy");
		
		String hsts = http.getAttribute("Hsts", "None");
		boolean hstsForce = Struct.objectToBoolean(http.getAttribute("HstsForce", "False"));

		if (!resp.headers().contains("Strict-Transport-Security") || hstsForce) {
			if ("SelfPlus".equals(hsts))
				resp.headers().set("Strict-Transport-Security", "max-age=157680000; includeSubDomains");
			else if ("Self".equals(hsts))
				resp.headers().set("Strict-Transport-Security", "max-age=157680000;");
			else if ("Custom".equals(hsts))
				resp.headers().set("Strict-Transport-Security", http.getAttribute("HstsValue", "max-age=157680000; includeSubDomains"));
		}		
		
		// domains don't insert defaults, only root does
		if (!root && (csp == null))
			return;
		
		// if root and no config, provide a default config
		if (csp == null)
			csp = new XElement("ContentSecurityPolicy");
		
		boolean cspForce = Struct.objectToBoolean(csp.getAttribute("Force", "False"));
		boolean cspReport = Struct.objectToBoolean(csp.getAttribute("ReportOnly", "False"));
		String mode = csp.getAttribute("Mode", "Strict");
		
		String report = "";
		
		if (cspReport)
			report = "-Report-Only";
		
		if (!resp.headers().contains("Content-Security-Policy" + report) || cspForce) {
			if ("Strict".equals(mode)) {
				resp.headers().set("Content-Security-Policy" + report, 
					"default-src 'self'; img-src 'self' data:; media-src mediastream:; frame-ancestors 'self'; connect-src *;");
			}
			else if ("Loose".equals(mode)) {
				resp.headers().set("Content-Security-Policy" + report, 
					"default-src 'self'; img-src *; media-src *; font-src *; style-src 'unsafe-inline' *; frame-ancestors 'self'; connect-src *;");
			}
			
			// TODO
		}		
   	}
	
	public void hardenPublic(SSLEngine engine) {
		this.harden(engine);
	}
	
	public void hardenBus(SSLEngine engine) {
		this.harden(engine);
	}
	
	public void harden(SSLEngine engine) {
		HubResources resources = Hub.instance.getResources(); 
		
		// impossible, so something is wrong with Hub just skip this
        if (resources == null)
        	return;
        
        // default not using SSLv2Hello, SSLv3, TLSv1, TLSv1.1 - see issue #22
        // also use only top of the line ciphers

        //engine.setEnabledProtocols(new String[] { "SSLv2Hello", "TLSv1", "TLSv1.1", "TLSv1.2" });
		
		XElement tls = resources.getConfig().selectFirst("Harden/TLS");
		
		if ((tls == null) || "Strict".equals(tls.getAttribute("Mode", "Strict"))) {
	        engine.setEnabledProtocols(new String[] { "TLSv1.2" });
	        
	        engine.setEnabledCipherSuites(new String[] {
	        		// AES 256 GCM SHA 384
	        		"TLS_RSA_WITH_AES_256_GCM_SHA384",
	        		"TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
	        		"TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
	        		"TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384",
	        		"TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384",
	        		"TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
	        		"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
	        		// AES 256 CBC SHA 384
	        		"TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384",
	        		"TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384",    		
	        		"TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
	        		"TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
	        		// AES 256 CBC SHA 256
	        		"TLS_RSA_WITH_AES_256_CBC_SHA256",
	        		"TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
	        		"TLS_DHE_DSS_WITH_AES_256_CBC_SHA256",
	        		// AES 128 GCM SHA 256
	        		"TLS_RSA_WITH_AES_128_GCM_SHA256",
	        		"TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
	        		"TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",        		
	        		"TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256",
	        		"TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256",
	        		"TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
	        		"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
	        		// SCSV
	        		"TLS_EMPTY_RENEGOTIATION_INFO_SCSV" });
		}
		else if ("Loose".equals(tls.getAttribute("Mode"))) {
	        engine.setEnabledProtocols(new String[] { "TLSv1", "TLSv1.1", "TLSv1.2" });
	        
	        engine.setEnabledCipherSuites(new String[] {
	        		// AES 256 GCM SHA 384
	        		"TLS_RSA_WITH_AES_256_GCM_SHA384",
	        		"TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
	        		"TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
	        		"TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384",
	        		"TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384",
	        		"TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
	        		"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
	        		// AES 256 CBC SHA 384
	        		"TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384",
	        		"TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384",    		
	        		"TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
	        		"TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
	        		// AES 256 CBC SHA 256
	        		"TLS_RSA_WITH_AES_256_CBC_SHA256",
	        		"TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
	        		"TLS_DHE_DSS_WITH_AES_256_CBC_SHA256",
	        		// AES 128 GCM SHA 256
	        		"TLS_RSA_WITH_AES_128_GCM_SHA256",
	        		"TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
	        		"TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",        		
	        		"TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256",
	        		"TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256",
	        		"TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
	        		"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
	        		// AES 128 CBC SHA 256
	        		"TLS_RSA_WITH_AES_128_CBC_SHA256",
	        		"TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
	        		"TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
	        		"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
	        		"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
	        		"TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256",
	        		"TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256",
	        		// AES 128 CBC SHA 128
	        		"TLS_RSA_WITH_AES_128_CBC_SHA",
	        		"TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
	        		"TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
	        		"TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
	        		"TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
	        		"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
	        		"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
	        		// SCSV
	        		"TLS_EMPTY_RENEGOTIATION_INFO_SCSV",
	        		// RC4 128 SHA1
	        		"TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
	        		"TLS_ECDHE_RSA_WITH_RC4_128_SHA",
	        		"TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
	        		"TLS_ECDH_RSA_WITH_RC4_128_SHA"
	       		});
		}
		// custom
		else {
	        engine.setEnabledProtocols(tls.getAttribute("Protocols", "").split(","));
	        
	        engine.setEnabledCipherSuites(tls.getAttribute("Suites", "").split(","));
		}
		
		if (engine.getEnabledProtocols().length == 0)
			Logger.warn("No Protocols are enabled!!");
		
		if (engine.getEnabledCipherSuites().length == 0)
			Logger.warn("No Cipher are enabled!!");
		
		/*
        System.out.println("Enabled");
        
        for (String p : engine.getEnabledProtocols())
        	System.out.println("Proto: " + p);
        
        for (String p : engine.getEnabledCipherSuites())
        	System.out.println("Suite: " + p);
        
        System.out.println();        
        System.out.println("Supported");
        System.out.println();        
        
        for (String p : engine.getSupportedProtocols())
        	System.out.println("Proto: " + p);
        
        for (String p : engine.getSupportedCipherSuites())
        	System.out.println("Suite: " + p);
        */
	}
	
}
