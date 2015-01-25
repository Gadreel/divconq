package divconq.hub;

import io.netty.handler.codec.http.HttpResponse;

import javax.net.ssl.SSLEngine;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;

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
		if (http == null)
			return;

		// is Http is Strict then just set a standard template 
		if ("Strict".equals(http.getAttribute("Mode")))
			http = new XElement("Http", 
					new XAttribute("Mode", "Strict"),
					new XElement("ContentSecurityPolicy", new XAttribute("Mode", "Strict")),
					new XElement("Hsts", new XAttribute("Mode", "Strict")),
					new XElement("Header", new XAttribute("Name", "X-Content-Type-Options"), new XAttribute("Value", "nosniff")),
					new XElement("Header", new XAttribute("Name", "X-XSS-Protection"), new XAttribute("Value", "1;mode=block")),
					new XElement("Header", new XAttribute("Name", "X-Frame-Options"), new XAttribute("Value", "deny"))
			);
		
		XElement hsts = http.find("Hsts");

		// custom STS can use Header tags instead
		if (hsts != null) {
			boolean hstsForce = Struct.objectToBoolean(hsts.getAttribute("Override", "False"));
			String mode = hsts.getAttribute("Mode", "Strict");
	
			if (!resp.headers().contains("Strict-Transport-Security") || hstsForce) {
				String age = hsts.getAttribute("Age", "P5Y");
				
				try {
					Period period = ISOPeriodFormat.standard().parsePeriod(age);
					
					long mage = new DateTime().plus(period).getMillis() / 1000;

					if ("Strict".equals(mode))
						resp.headers().set("Strict-Transport-Security", "max-age=" + mage + "; includeSubDomains");
					else if ("Self".equals(mode))
						resp.headers().set("Strict-Transport-Security", "max-age=" + mage + ";");
				}
				catch (Exception x) {
					OperationContext.get().error("Bad age value for Strict-Transport-Security");
				}
			}		
		}
		
		XElement csp = http.find("ContentSecurityPolicy");
		
		// if root and no config, provide a default config
		// custom CSP can use Header tags instead
		if (csp != null) {
			boolean cspForce = Struct.objectToBoolean(csp.getAttribute("Override", "False"));
			boolean cspReport = Struct.objectToBoolean(csp.getAttribute("ReportOnly", "False"));
			String mode = csp.getAttribute("Mode", "Strict");
			
			String name = "Content-Security-Policy";
			
			if (cspReport)
				name = "-Report-Only";
			
			if (!resp.headers().contains(name) || cspForce) {
				if ("Strict".equals(mode)) {
					resp.headers().set(name, 
						"default-src 'self'; img-src 'self' data:; media-src mediastream:; frame-ancestors 'self'; connect-src *;");
				}
				else if ("Loose".equals(mode)) {
					resp.headers().set(name, 
						"default-src 'self'; img-src *; media-src *; font-src *; style-src 'unsafe-inline' *; frame-ancestors 'self'; connect-src *;");
				}
			}
		}
		
		for (XElement hdr : http.selectAll("Header")) {
			boolean force = Struct.objectToBoolean(hdr.getAttribute("Override", "False"));
			String name = hdr.getAttribute("Name");
			String value = hdr.getAttribute("Value");
			
			if (StringUtil.isEmpty(name))
				continue;
			
			if (!resp.headers().contains(name) || force) {
				if (StringUtil.isEmpty(value))
					resp.headers().remove(name);
				else
					resp.headers().set(name, value);
			}
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
