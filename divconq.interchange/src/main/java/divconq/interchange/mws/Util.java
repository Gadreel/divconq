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
package divconq.interchange.mws;

import java.net.URI;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import divconq.util.Base64;
import divconq.util.StringUtil;

/**
 * Cherry picked a few methods from Amazon's MWS.  Let's keep things simple :)
 * 
 * @author Andy White
 */
public class Util {
    /** Match "%2F". */
    private static final Pattern pct2FPtn = Pattern.compile("%2F", Pattern.LITERAL);

    /** Match "%7E". */
    private static final Pattern pct7EPtn = Pattern.compile("%7E", Pattern.LITERAL);

    /** Match a + character. */
    private static final Pattern plusPtn = Pattern.compile("+", Pattern.LITERAL);
    
    /** Match an asterisk character. */
    private static final Pattern asteriskPtn = Pattern.compile("*", Pattern.LITERAL);

    /**
     * Computes RFC 2104-compliant HMAC signature.
     * 
     * @param data
     *            The data to sign.
     * @param key
     *            The key to use for signing.
     * 
     * @param algorithm
     *            The signing algorithm.
     * 
     * @return The signature.
     */
    public static String sign(String data, String key, String algorithm) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            
            mac.init(new SecretKeySpec(key.getBytes("UTF-8"), algorithm));
            
            return Base64.encodeToString(mac.doFinal(data.getBytes("UTF-8")), false); 
        } 
        catch (Exception x) {
            return null;
        }
    }

    /**
     * Determine if a url uses the standard port.
     * Port 80 for http, 443 for https.
     * 
     * @param uri
     * 
     * @return true if standard port is used.
     */
    public static boolean usesStandardPort(URI uri) {
        int portNumber = uri.getPort();
        
        if (portNumber == -1) 
            return true;
        
        int standardPort = uri.getScheme().equals("https") ? 443 : 80;
        
        return (portNumber == standardPort);
    }
    
    /**
     * URL encode a value.
     * 
     * @param value
     * 
     * @param path
     *            true if is a path and '/' should not be encoded.
     * 
     * @return The encoded string.
     */
    public static String urlEncode(String value, boolean path) {
        try {
            value = URLEncoder.encode(value, "UTF-8");
            
            value = replaceAll(value, plusPtn, "%20");
            value = replaceAll(value, asteriskPtn, "%2A");
            value = replaceAll(value, pct7EPtn, "~");
            
            if (path) 
                value = replaceAll(value, pct2FPtn, "/");
        } 
        catch (Exception x) {
        }
        
        return value;
    }

    /**
     * Replace a pattern in a string.
     * <p>
     * Do not do recursive replacement. Return the original string if no changes
     * are required.
     * 
     * @param s
     *            The string to search.
     * 
     * @param p
     *            The pattern to search for.
     * 
     * @param r
     *            The string to replace occurrences with.
     * 
     * @return The new string.
     */
    static String replaceAll(String s, Pattern p, String r) {
    	if (StringUtil.isEmpty(s))
    		return s;

        Matcher m = p.matcher(s);
        
        if (!m.find()) 
            return s;
        
        int k = 0;
        int n = s.length();
        
        StringBuilder buf = new StringBuilder(n + 12);
        
        do {
            buf.append(s, k, m.start());
            buf.append(r);
            k = m.end();
        } 
        while (m.find());
        
        if (k < n) 
            buf.append(s, k, n);
        
        return buf.toString();
    }
}
