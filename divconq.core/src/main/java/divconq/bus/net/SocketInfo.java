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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import divconq.util.StringUtil;
import divconq.xml.XElement;

public class SocketInfo {
	public enum ConnectorKind {
		Loopback,
		All,
		VmPipe,
		Tcp
	}
	
	protected ConnectorKind kind = ConnectorKind.Tcp;
	protected String name = null;
	protected int port = 7443;
	protected int streamport = 0;
	protected boolean useSsl = false;
	protected int count = 2;
	protected int streamcount = 2;
	protected String hubid = null;
	protected String targetthumbprint = null;
	
	/*
	 * <Connector Kind="[enum above]" Address="[ip/name]" HubId="[hubid]"  Port="[num]" Ssl="True|False" Number="[n]" TargetThumbprint="[targetthumbprint]" />
	 * <Listener Kind="[enum above]" Address="[ip/name]" Port="[num]" Ssl="True|False" />
	 */
	public void loadConfig(XElement config) {
		if (config == null)
			return;
		
		/* TODO only TCP/IP supported currently
		String kind = config.getAttribute("Kind");
		
		if ("Loopback".equals(kind))
			this.kind = ConnectorKind.Loopback;
		else if ("Interface".equals(kind))
			this.kind = ConnectorKind.Tcp;
		else if ("Tcp".equals(kind))
			this.kind = ConnectorKind.Tcp;
		else if ("VmPipe".equals(kind))
			this.kind = ConnectorKind.VmPipe;
		else if ("All".equals(kind))
			this.kind = ConnectorKind.All;
		*/
		
		if ("Listener".equals(config.getName()))
			this.kind = ConnectorKind.All;
		
		if (config.hasAttribute("Address"))
			this.name = config.getAttribute("Address");
		
		String port = config.getAttribute("Port");
		
		try {
			if (StringUtil.isNotEmpty(port))
				this.port = Integer.parseInt(port);
		}
		catch (Exception x) {
			// TODO log error
		}
		
		port = config.getAttribute("StreamPort");
		
		try {
			if (StringUtil.isNotEmpty(port))
				this.streamport = Integer.parseInt(port);
		}
		catch (Exception x) {
			// TODO log error
		}
		
		this.useSsl = "true".equals(config.getAttribute("Ssl", "true").toLowerCase());
		
		// TODO given just the hubid we should be able to find the address/port/thumbprint/kind from
		// the MATRIX file...
		this.hubid = config.getAttribute("HubId");
		
		if (config.hasAttribute("TargetThumbprint"))
			this.targetthumbprint = config.getAttribute("TargetThumbprint").toLowerCase().replace(":", "").trim();
		
		this.count = (int) StringUtil.parseInt(config.getAttribute("Number"), 2);
		
		this.streamcount = (int) StringUtil.parseInt(config.getAttribute("StreamNumber"), 2);
	}
	
	public void setKind(ConnectorKind kind) {
		this.kind = kind;
	}
	
	public ConnectorKind getKind() {
		return this.kind;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setAddr(InetAddress addr) {
		this.name = addr.toString();
	}
	
	public InetAddress getAddr() {
		try {
			return InetAddress.getByName(this.name);
		} catch (UnknownHostException e) {
			// TODO record bad config error
		}
		
		return null;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public int getPort() {
		return this.port;
	}
	
	public int getStreamPort() {
		return (this.streamport != 0) ? this.streamport : (this.port + 1);
	}
	
	public void setUseSsl(boolean useSsl) {
		this.useSsl = useSsl;
	}
	
	public boolean isUseSsl() {
		return this.useSsl;
	}
	
	public void setCount(int count) {
		this.count = count;
	}
	
	public int getCount() {
		return this.count;
	}
	
	public void setStreamCount(int count) {
		this.streamcount = count;
	}
	
	public int getStreamCount() {
		return this.streamcount;
	}
	
	public void setHubId(String v) {
		this.hubid = v;
	}

	public String getHubId() {
		return this.hubid;
	}
	
	public String getTargetthumbprint() {
		return this.targetthumbprint;
	}
	
	public SocketAddress getAddress() {
    	switch (this.kind) {
    	case Loopback: 
			try {
				InetAddress addr = InetAddress.getByName(null);   // loopback is null
	        	return new InetSocketAddress(addr, this.port);
			} 
			catch (UnknownHostException e) {				
			}           	
			break;
    	case VmPipe: 
    		// TODO return new VmPipeAddress(this.port);
    		break;
    	case Tcp: 
    		return new InetSocketAddress(this.getAddr(), this.port);
    	case All: 
    		try {
	    		return new InetSocketAddress(InetAddress.getByName("::"), this.port);
			} 
			catch (UnknownHostException e) {				
			}           	
			break;
		default:
			break;
    	}

    	return null;
	}
	
	public SocketAddress getStreamAddress() {
    	switch (this.kind) {
    	case Loopback: 
			try {
				InetAddress addr = InetAddress.getByName(null);   // loopback is null
	        	return new InetSocketAddress(addr, this.getStreamPort());
			} 
			catch (UnknownHostException e) {				
			}           	
			break;
    	case VmPipe: 
    	//	TODO return new VmPipeAddress(this.port);
    		break;
    	case Tcp: 
    		return new InetSocketAddress(this.getAddr(), this.getStreamPort());
    	case All: 
			try {
				return new InetSocketAddress(InetAddress.getByName("::"), this.getStreamPort());
			} 
			catch (UnknownHostException e) {				
			}           	
			break;
    	}

    	return null;
	}

	public static SocketInfo buildRemote(String name, int port, boolean ssl) {
		SocketInfo info = new SocketInfo();
		info.setKind(ConnectorKind.Tcp);
		info.setUseSsl(ssl);
		info.setPort(port);
		info.setName(name);
		return info;
	}

	public static SocketInfo buildLoopback(int port, boolean ssl) {
		SocketInfo info = new SocketInfo();
		info.setKind(ConnectorKind.Loopback);
		info.setUseSsl(ssl);
		info.setPort(port);
		return info;
	}

	public static SocketInfo buildAll(int port, boolean ssl) {
		SocketInfo info = new SocketInfo();
		info.setKind(ConnectorKind.All);
		info.setUseSsl(ssl);
		info.setPort(port);
		return info;
	}

	public URI getUri() {
        try {
			return new URI((this.isUseSsl() ? "wss" : "ws") + "://" + this.name + ":" + this.port + CommonHandler.BUS_PATH);
		} 
        catch (URISyntaxException x) {
		}
        
		return null;
	}
}
