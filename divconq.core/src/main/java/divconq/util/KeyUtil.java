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
package divconq.util;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import divconq.lang.FuncResult;

public class KeyUtil {
	static public class CertSummary {
		public String alias = null;
		public String subject = null;
        public String thumbprint = null;  
        public boolean privatekey = false;
	}
	
	static public FuncResult<Collection<CertSummary>> loadKeystoreSummary(Path keystore, char[] password) {
		FuncResult<Collection<CertSummary>> res = new FuncResult<>();
		
		KeyStore ks = null;
		
        try {
        	// load key store
            ks = KeyStore.getInstance("JKS");
            ks.load(Files.newInputStream(keystore), password);
        }
        catch (Exception x) {
        	res.error("Unable to open/read keystore file: " + x);
        	return res;
        }
        
        try {
    		List<CertSummary> certs = new ArrayList<>();
    		res.setResult(certs);

            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            Enumeration<String> aliases = ks.aliases();
            
            while (aliases.hasMoreElements()) {
              CertSummary summ = new CertSummary();
              summ.alias =  aliases.nextElement();
              
              byte[] encoded = null;
              
              if (ks.isCertificateEntry(summ.alias)) 
	              encoded = ((KeyStore.TrustedCertificateEntry) ks.getEntry(summ.alias, null)).getTrustedCertificate().getEncoded();
              else if (ks.isKeyEntry(summ.alias)) {
	              encoded = ((KeyStore.PrivateKeyEntry) ks.getEntry(summ.alias, new KeyStore.PasswordProtection(password))).getCertificate().getEncoded();
	              summ.privatekey = true;
              }
              
              if (encoded == null) {
            	  res.error("Unable to decode: " + summ.alias);
            	  return res;
              }
              
              X509Certificate cert = (X509Certificate)certFactory.generateCertificate(new ByteArrayInputStream(encoded));				              
              
	          summ.subject = cert.getSubjectDN().toString();
	          summ.thumbprint = KeyUtil.getCertThumbprint(cert);  
              
              certs.add(summ);
            }
        } 
        catch (Exception x) {
        	res.error("Unable to decode keystore file: " + x);
        }
	    
		return res;
	}
	
    
	  /* key export notes
    if ((key instanceof PrivateKey) && "PKCS#8".equals(key.getFormat())) {
      // Most PrivateKeys use this format, but check for safety. 
      try (FileOutputStream os = new FileOutputStream(alias + ".key")) {
        os.write(key.getEncoded());
        os.flush();
      }
    }
	   */


	public static String getCertThumbprint(X509Certificate cert) {
		try {
			return HashUtil.getSha1(new ByteArrayInputStream(cert.getEncoded()));
		} 
		catch (CertificateEncodingException x) {
		}
		
		return null;
	}

	public static String getCertThumbprint(javax.security.cert.X509Certificate cert) {
		try {
			return HashUtil.getSha1(new ByteArrayInputStream(cert.getEncoded()));
		} 
		catch (javax.security.cert.CertificateEncodingException x) {
		}
		
		return null;
	}

	public static String getKeyThumbprint(java.security.Key key) {
		return HashUtil.getSha1(new ByteArrayInputStream(key.getEncoded()));
	}

	public static String getCertThumbprint(java.security.cert.Certificate cert) {
		try {
			return HashUtil.getSha1(new ByteArrayInputStream(cert.getEncoded()));
		} 
		catch (java.security.cert.CertificateEncodingException x) {
		}
		
		return null;
	}
}
