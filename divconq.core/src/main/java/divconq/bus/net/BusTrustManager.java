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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.net.ssl.X509TrustManager;

import divconq.util.KeyUtil;
import divconq.xml.XElement;

public class BusTrustManager implements X509TrustManager {
	protected CopyOnWriteArraySet<String> trustedCerts = new CopyOnWriteArraySet<>();
	
	public void init(XElement config) {
		if (config == null)
			return;
		
		// trust all Trust entries
		for (XElement thumbs : config.selectAll("Trust"))
			if (thumbs.hasAttribute("Thumbprint"))
				this.trustedCerts.add(thumbs.getAttribute("Thumbprint").toLowerCase().replace(":", ""));
		
		// by inference trust all Connector thumbprints too
		for (XElement thumbs : config.selectAll("Connector"))
			if (thumbs.hasAttribute("TargetThumbprint"))
				this.trustedCerts.add(thumbs.getAttribute("TargetThumbprint").toLowerCase().replace(":", ""));
		
		// by inference trust all Listener thumbprints too
		for (XElement thumbs : config.selectAll("Listener"))
			if (thumbs.hasAttribute("TargetThumbprint"))
				this.trustedCerts.add(thumbs.getAttribute("TargetThumbprint").toLowerCase().replace(":", ""));
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
		
		chain[0].checkValidity();
		
		/* TODO need to verify cert with master
		X509Certificate parent = ...;
		X509Certificate certToVerify = ...;
		certToVerify.verify(parent.getPublicKey());
		*/
    	
    	String subject = chain[0].getSubjectDN().toString();
    	String thumbprint = KeyUtil.getCertThumbprint(chain[0]);  
    	
    	if (thumbprint == null)
        	throw new CertificateException("BAD SERVER CERTIFICATE - CANNOT COMPUTE THUMBPRINT: " + subject);
    	
    	if (!this.trustedCerts.contains(thumbprint)) {
            System.err.println("UNTRUSTED SERVER CERTIFICATE: " + subject + " - thumbprint: " + thumbprint);
        	throw new CertificateException("UNTRUSTED SERVER CERTIFICATE: " + subject);
    	}
    	
        System.err.println("TRUSTED SERVER CERTIFICATE: " + subject + " - thumbprint: " + thumbprint);
    }
}
