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

import java.net.InetAddress;

// TODO test with IPv4 and IPv6

public class Subnet {
	private String cidr = null; 
    private byte[] address = null;
    private byte[] mask = null;
    private byte[] filter = null;

    public Subnet(String cidrvalue) throws Exception {
    	this.cidr = cidrvalue;
        String[] parts = cidrvalue.split("/");

        this.address = InetAddress.getByName(parts[0]).getAddress();
        int prefix = Integer.parseInt(parts[1]);

        this.mask = new byte[this.address.length];

        for (int i = 0; (i < this.address.length) && (prefix > 0); i++)
        {
        	this.mask[i] = (prefix >= 8) ? (byte)0xFF : (byte)(0xFF << (8 - prefix));
            prefix -= 8;
        }

        this.filter = new byte[this.address.length];

        for (int i = 0; i < this.address.length; i++)
        	this.filter[i] = (byte)(this.address[i] & this.mask[i]);
    }

    public boolean match(byte[] address) {
        if (address.length != address.length) 
        	return false;

        for (int i = 0; i < address.length; i++)
            if (this.filter[i] != (byte)(address[i] & this.mask[i])) 
            	return false;

        return true;
    }

    @Override
    public String toString() {
        return this.cidr;
    }
}
