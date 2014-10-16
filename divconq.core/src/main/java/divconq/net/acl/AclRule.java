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

import divconq.net.Subnet;
import divconq.xml.XElement;

public class AclRule {
    protected Subnet subnet = null;
    private AclKind kind = AclKind.Deny;

    public AclKind check(byte[] address) {
        if ((this.subnet == null) || this.subnet.match(address)) 
        	return this.kind;
        
        // does not match filter, go to next rule
        return AclKind.Unknown;  
    }

    public boolean appliesTo(byte[] address) {
        return (this.check(address) != AclKind.Unknown);
    }

    public void loadConfig(XElement rule) {
    	if ("Allow".equals(rule.getAttribute("Kind")))
    		this.kind = AclKind.Allow;

        if (rule.hasAttribute("Cidr")) 
        	try {
	            this.subnet = new Subnet(rule.getAttribute("Cidr"));
	        }
        	catch (Exception x) {
        		// TODO log bad rule
        	}
    }

	public void setSubnet(Subnet sn) {
		this.subnet = sn;
	}

	public Subnet getSubnet() {
		return this.subnet;
	}

	public void setKind(AclKind kind) {
		this.kind = kind;
	}

	public AclKind getKind() {
		return this.kind;
	}
}
