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
package divconq.net.acl;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import divconq.xml.XElement;

public class AclFilter  {
	// TODO review if we need to normalize IPv6 addresses or not...
	
    protected List<AclRule> rules = new ArrayList<AclRule>();

    /*
     * 		<Acl>
     * 			<Rule Kind="Allow|Deny" Cidr="" />
     * 		</Acl>		
     * 
     */
    public void loadConfig(XElement rules) {
    	if (rules == null)
    		return;
    	
    	for(XElement node : rules.selectAll("Rule")) 
    		this.loadRule(node);
    }

    public void loadRule(XElement rulenode)
    {
        AclRule rule = new AclRule();
        rule.loadConfig(rulenode);
        this.rules.add(rule);
    }

	public void addRule(AclRule rule) {
        this.rules.add(rule);
	}

    protected AclRule findRule(byte[] address)
    {
        for (AclRule acr : this.rules)
            if (acr.appliesTo(address)) 
            	return acr;

        return null;
    }

    public AclRule findRule(InetAddress addr) {
        return this.findRule(addr.getAddress());
    }

    protected AclKind check(byte[] address) {
        AclRule acr = this.findRule(address);
        
        return (acr == null) ? AclKind.Unknown : acr.check(address);
    }

    public AclKind check(InetAddress addr) {
        return this.check(addr.getAddress());
    }

    /*
    private boolean isBlocked(IoSession session) {
        SocketAddress remoteAddress = session.getRemoteAddress();
        
        if (remoteAddress instanceof InetSocketAddress) {
            InetAddress address = ((InetSocketAddress) remoteAddress).getAddress(); 
            
            // allow unless explicitly denied
            if (this.check(address.getAddress()) != AclKind.Deny)
                return false;
        }

        return true;
    }
    */
}
