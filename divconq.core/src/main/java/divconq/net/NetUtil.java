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
package divconq.net;

import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.util.Map;

import com.googlecode.ipv6.IPv6Address;

public class NetUtil {
    static public String urlEncodeUTF8(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } 
        catch (UnsupportedEncodingException e) {
            // NA
        }
        
        return null;
    }
    
    static public String urlEncodeUTF8(Map<String,String> map) {
        StringBuilder sb = new StringBuilder();
        
        for (Map.Entry<String,String> entry : map.entrySet()) {
            if (sb.length() > 0) 
                sb.append("&");
            
            sb.append(urlEncodeUTF8(entry.getKey()) + "=" + urlEncodeUTF8(entry.getValue()));
        }
        
        return sb.toString();       
    }
    
    static public String formatIpAddress(InetSocketAddress addr) {
    	if (addr.getAddress() instanceof Inet4Address)
    		return addr.getHostString();
    	
		if (addr.getAddress() instanceof Inet6Address) {
			IPv6Address got = IPv6Address.fromInetAddress(addr.getAddress());
			
			return got.toString();
		}
		
		return null;
    }
}
