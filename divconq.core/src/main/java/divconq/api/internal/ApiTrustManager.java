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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.net.ssl.X509TrustManager;

import divconq.util.KeyUtil;
import divconq.xml.XElement;

public class ApiTrustManager implements X509TrustManager {
	protected CopyOnWriteArraySet<String> trustedCerts = new CopyOnWriteArraySet<>();
	protected boolean validatessl = false;
	protected boolean limittrust = false;
	
	public void init(XElement config) {
		if (config == null)
			return;
		
		if (config.hasAttribute("Thumbprint"))
			this.trustedCerts.add(config.getAttribute("Thumbprint").toLowerCase().replace(":", ""));
		
		// trust all Trust entries
		for (XElement thumbs : config.selectAll("Trust"))
			if (thumbs.hasAttribute("Thumbprint"))
				this.trustedCerts.add(thumbs.getAttribute("Thumbprint").toLowerCase().replace(":", ""));
		
		// TODO probably want to switch these to ON by default, but NCC expects off so until convert
		this.validatessl = config.getAttribute("Validate", "Disabled").contains("SSL");
		this.limittrust = config.getAttribute("Validate", "Disabled").contains("Trust");
	}
	
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[] { };
    }
    
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		if (chain.length == 0)
        	throw new CertificateException("MISSING CLIENT CERTIFICATE");
		
		chain[0].checkValidity();
		
		/* TODO need to verify cert with master
		X509Certificate parent = ...;
		X509Certificate certToVerify = ...;
		certToVerify.verify(parent.getPublicKey());
		*/
		
    	String thumbprint = KeyUtil.getCertThumbprint(chain[0]);  
    	String subject = chain[0].getSubjectDN().toString();
    	
    	if (thumbprint == null)
        	throw new CertificateException("BAD CLIENT CERTIFICATE - CANNOT COMPUTE THUMBPRINT: " + subject);
    	
    	if (!this.trustedCerts.contains(thumbprint)) {
            System.err.println("UNTRUSTED CLIENT CERTIFICATE: " + subject + " - thumbprint: " + thumbprint);
        	throw new CertificateException("UNTRUSTED CLIENT CERTIFICATE: " + subject);
    	}
    	
        System.err.println("TRUSTED CLIENT CERTIFICATE: " + subject + " - thumbprint: " + thumbprint);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		if (chain.length == 0)
        	throw new CertificateException("MISSING SERVER CERTIFICATE");
		
		if (this.validatessl) {
			chain[0].checkValidity();
		
			/* TODO need to verify cert with master
			X509Certificate parent = ...;
			X509Certificate certToVerify = ...;
			certToVerify.verify(parent.getPublicKey());
			*/
		}

    	String subject = chain[0].getSubjectDN().toString();
    	String thumbprint = KeyUtil.getCertThumbprint(chain[0]);  
    	
    	if (thumbprint == null)
        	throw new CertificateException("BAD SERVER CERTIFICATE - CANNOT COMPUTE THUMBPRINT: " + subject);
    	
		if (this.limittrust) {	    	
	    	if (!this.trustedCerts.contains(thumbprint)) {
	            System.err.println("UNTRUSTED SERVER CERTIFICATE: " + subject + " - thumbprint: " + thumbprint);
	        	throw new CertificateException("UNTRUSTED SERVER CERTIFICATE: " + subject);
	    	}
		}
    	
        System.err.println("TRUSTED SERVER CERTIFICATE: " + subject + " - thumbprint: " + thumbprint);
    }
}
