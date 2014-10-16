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
package divconq.api;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import divconq.util.StringUtil;
import divconq.xml.XElement;

public class ClientInfo {
	public enum ConnectorKind {
		Local,
		Http,
		WebSocket
	}
	
	protected ConnectorKind kind = ConnectorKind.WebSocket;
	protected String name = null;
	protected String host = null;
	protected String domain = null;
	protected int port = 0;
	protected boolean secure = false;
	protected String hubid = null;
	protected String targetthumbprint = null;
	
	/*
	 * <ApiSession Name="h1" Class="divconq.api.HyperSessionFactory" Host="localhost" Port="8443" Secure="True" HubId="[hubid]"  TargetThumbprint="[targetthumbprint]" />
	 */
	public void loadConfig(XElement config) {
		if (config == null)
			return;
		
		this.name = config.getAttribute("Name");
		this.host = config.getAttribute("Host");

		this.port = (int) StringUtil.parseInt(config.getAttribute("Port"), 0);
		
		this.secure = "True".equals(config.getAttribute("Secure"));
		
		this.hubid = config.getAttribute("HubId");
		
		if (config.hasAttribute("TargetThumbprint"))
			this.targetthumbprint = config.getAttribute("TargetThumbprint").toLowerCase().replace(":", "");
	}
	
	public void setKind(ConnectorKind kind) {
		this.kind = kind;
	}
	
	public ConnectorKind getKind() {
		return this.kind;
	}
	
	public void setName(String v) {
		this.name = v;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setHost(String v) {
		this.host = v;
	}
	
	public String getHost() {
		return this.host;
	}
	
	public void setAddr(InetAddress addr) {
		this.host = addr.toString();
	}
	
	public InetAddress getAddr() {
		try {
			return InetAddress.getByName(this.host);
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
	
	public void setSecure(boolean useSsl) {
		this.secure = useSsl;
	}
	
	public boolean isSecurel() {
		return this.secure;
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
		if ("localhost".equals(this.host) || "127.0.0.1".equals(this.host))
			try {
				InetAddress addr = InetAddress.getByName(null);   // loopback is null
	        	return new InetSocketAddress(addr, this.port);
			} 
			catch (UnknownHostException e) {				
			}           	
		else
    		return new InetSocketAddress(this.getAddr(), this.port);

    	return null;
	}
	
	public String getPath() {
    	if (this.getKind() == ConnectorKind.WebSocket)
    		return "/bus";
    	
   		return "/rpc";
	}

	public String getUrl() {
    	if (this.getKind() == ConnectorKind.WebSocket)
    		return (this.isSecurel() ? "wss" : "ws") + "://" + this.getHost() + ":" + this.getPort() + "/bus";
    	
   		return (this.isSecurel() ? "https" : "http") + "://" + this.getHost() + ":" + this.getPort() + "/rpc";
	}

	public URI getUri() {
        try {
       		return new URI(this.getUrl());
		} 
        catch (URISyntaxException x) {
		}
        
		return null;
	}
}
