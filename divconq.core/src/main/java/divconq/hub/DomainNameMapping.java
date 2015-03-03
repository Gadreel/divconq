package divconq.hub;

import java.net.IDN;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * thanks Netty :)
 */
public class DomainNameMapping<V> {
    protected static final Pattern DNS_WILDCARD_PATTERN = Pattern.compile("^\\*\\..*");
    protected Map<String, V> map = new HashMap<>();

    /*
     * Adds a mapping that maps the specified (optionally wildcard) host name to the specified output value.
     * <p>
     * <a href="http://en.wikipedia.org/wiki/Wildcard_DNS_record">DNS wildcard</a> is supported as hostname.
     * For example, you can use {@code *.netty.io} to match {@code netty.io} and {@code downloads.netty.io}.
     * </p>
     */
    public void add(String hostname, V di) {
        map.put(normalizeHostname(hostname), di);
    }
    
    public void remove(String hostname) {
        map.remove(normalizeHostname(hostname));
    }

    /*
	public void dumpDomainNames() {
		System.out.println("Domains: ");
		System.out.println();
		
		for (Entry<String, DomainInfo> en : this.map.entrySet()) {
			System.out.println("Domain: " + en.getKey() + " - " + en.getValue().getId() + " : " + en.getValue().getTitle());
		}
	}
	*/

    /**
     * Simple function to match <a href="http://en.wikipedia.org/wiki/Wildcard_DNS_record">DNS wildcard</a>.
     */
    protected static boolean matches(String hostNameTemplate, String hostName) {
        // note that inputs are converted and lowercased already
        if (DNS_WILDCARD_PATTERN.matcher(hostNameTemplate).matches()) {
            return hostNameTemplate.substring(2).equals(hostName) ||
                    hostName.endsWith(hostNameTemplate.substring(1));
        } 
        else {
            return hostNameTemplate.equals(hostName);
        }
    }

    /**
     * IDNA ASCII conversion and case normalization
     */
    protected static String normalizeHostname(String hostname) {
        if (needsNormalization(hostname)) 
            hostname = IDN.toASCII(hostname, IDN.ALLOW_UNASSIGNED);

        return hostname.toLowerCase(Locale.US);
    }

    protected static boolean needsNormalization(String hostname) {
        int length = hostname.length();
        
        for (int i = 0; i < length; i ++) {
            int c = hostname.charAt(i);
            
            if (c > 0x7F) 
                return true;
        }
        
        return false;
    }

    public V get(String name) {
        if (name != null) {
        	name = normalizeHostname(name);
        	
        	// prefer exact matches over wild
        	V exact = map.get(name);
        	
        	if (exact != null)
        		return exact;

            for (Map.Entry<String, V> entry : map.entrySet()) {
                if (matches(entry.getKey(), name)) 
                    return entry.getValue();
            }
        }

        return null;
    }
}

