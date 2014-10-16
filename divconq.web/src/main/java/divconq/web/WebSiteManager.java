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
package divconq.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Adler32;

import divconq.hub.DomainInfo;
import divconq.hub.Hub;
import divconq.mod.Bundle;
import divconq.mod.ExtensionLoader;
import divconq.mod.IExtension;
import divconq.net.IpAddress;
import divconq.util.MimeUtil;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class WebSiteManager {
	static public final WebSiteManager instance = new WebSiteManager(); 
	
	protected WebModule module = null;
	protected Map<String,IWebExtension> extensions = new HashMap<String,IWebExtension>();
	protected String defaultExtension = null;
	
	protected String version = null;
	
	// TODO when module is unloaded, clean up all references to classes
	protected Map<String,Class<? extends IViewBuilder>> builderClasses = new HashMap<String,Class<? extends IViewBuilder>>();
	protected Map<String,Class<? extends IContentInfo>> contentClasses = new HashMap<String,Class<? extends IContentInfo>>();
	protected Map<String,IWebMacro> macros = new HashMap<String,IWebMacro>();
	protected ValuesMacro vmacros = new ValuesMacro();
	
	protected List<XElement> devices = new ArrayList<XElement>();
	
	public Collection<IWebExtension> getSites() {
		return this.extensions.values();
	}
	
	public WebModule getModule() {
		return this.module;
	}
	
	public String getVersion() {
		return this.version;
	}
	
	public void start(WebModule module, XElement config) {
		this.module = module;

		if (config != null) {
			// ideally we would only load in Hub level settings, try to use sparingly
			MimeUtil.load(config);
			
			this.devices = config.selectAll("DeviceRule");
			
			for (XElement builder : config.selectAll("Format")) {
				String fmt = builder.getAttribute("Name");
				
				if (StringUtil.isEmpty(fmt))
					continue;
				
				String bname = builder.getAttribute("BuilderClass");
				
				if (StringUtil.isNotEmpty(bname)) {
					Class<?> cls = this.module.getLoader().getClass(bname);
					
					if (cls != null) {
						Class<? extends IViewBuilder> tcls = cls.asSubclass(IViewBuilder.class);
						
						if (tcls != null) 
							this.builderClasses.put(fmt, tcls);
					} 
	
					// TODO log
					//System.out.println("unable to load class: " + cname);
				}
				
				String cname = builder.getAttribute("ContentClass");
				
				if (StringUtil.isNotEmpty(cname)) {
					Class<?> cls = this.module.getLoader().getClass(cname);
					
					if (cls != null) {
						Class<? extends IContentInfo> tcls = cls.asSubclass(IContentInfo.class);
						
						if (tcls != null) 
							this.contentClasses.put(fmt, tcls);
					} 
	
					// TODO log
					//System.out.println("unable to load class: " + cname);
				}
				
				/*
				String pname = builder.getAttribute("ParserClass");
				
				if (StringUtil.isNotEmpty(pname)) {
					Class<?> cls = this.module.getLoader().getClass(pname);
					
					if (cls != null) {
						Class<? extends IViewParser> tcls = cls.asSubclass(IViewParser.class);
						
						if (tcls != null)
							try {
								this.formatParsers.put(fmt, tcls.newInstance());
							} 
							catch (Exception x) {
								// TODO log
							}
					} 
	
					// TODO log
					//System.out.println("unable to load class: " + pname);
				}
				*/
			}
			
			for (XElement macros : config.selectAll("Macro")) {
				String name = macros.getAttribute("Name");
				
				if (StringUtil.isEmpty(name))
					continue;
				
				String bname = macros.getAttribute("Class");
				
				if (StringUtil.isNotEmpty(bname)) {
					Class<?> cls = this.module.getLoader().getClass(bname);
					
					if (cls != null) {
						Class<? extends IWebMacro> tcls = cls.asSubclass(IWebMacro.class);
						
						if (tcls != null)
							try {
								this.macros.put(name, tcls.newInstance());
							} 
							catch (Exception x) {
								x.printStackTrace();
							}
					} 

					// TODO log
					//System.out.println("unable to load class: " + cname);
				}
				
				String value = macros.getAttribute("Value");
				
				if (StringUtil.isNotEmpty(value)) 
					this.vmacros.add(name, value);
			}
		}
	
		// prepare extensions (web apps)
	
		XElement lcf = module.getLoader().getConfig();
		Adler32 ad = new Adler32();
		
		if (lcf != null)
	    	for(XElement node : lcf.selectAll("Extension")) {
	    		String name = node.getAttribute("Name");
	    		
	    		Bundle bundle = this.module.getLoader().getExtension(name);
	    		
	    		if (! (bundle instanceof ExtensionLoader))
	    			return;

	    		IExtension ex = ((ExtensionLoader)bundle).getExtension();
	    		
	    		if (! (ex instanceof IWebExtension))
	    			return;
	    		
	    		// only compute if this is a web extension
	    		bundle.adler(ad);
	    		
	    		IWebExtension sm = (IWebExtension)ex;

	    		this.extensions.put(name, sm);
	    		
	    		if (this.defaultExtension == null)
	    			this.defaultExtension = name;
	    	}
		
		this.version = Long.toHexString(ad.getValue());

		/* TODO
		Hub.instance.listenOnline(new OperationCallback() {
			@Override
			public void callback() {
				// load dynamic web parts only when domains are loaded by Hub
				for (IWebExtension ext : WebSiteManager.this.extensions.values())
					ext.loadDynamic();
			}
		} );	
		*/	
	}
	
	public String resolveHost(Request req) {
		return this.resolveHost(req.getHeader("Host"));
	}
	
	public String resolveHost(String dname) {
		if (StringUtil.isNotEmpty(dname)) {
			int cpos = dname.indexOf(':');
			
			if (cpos > -1)
				dname = dname.substring(0, cpos);
		}
		
		if (StringUtil.isEmpty(dname) || IpAddress.isIpLiteral(dname))
			dname = "localhost";
		
		return dname;
	}
	
	public DomainInfo resolveDomainInfo(Request req) {
		return Hub.instance.resolveDomainInfo(this.resolveHost(req));
	}
	
	public DomainInfo resolveDomainInfo(String dname) {
		return Hub.instance.resolveDomainInfo(this.resolveHost(dname));
	}

	public IWebMacro getMacro(String name) {
		if (this.vmacros.hasKey(name))
			return this.vmacros;
		
		return this.macros.get(name);
	}

	public IWebExtension getExtension(String name) {
		return this.extensions.get(name);
	}
	
	public IWebExtension getDefaultExtension() {
		return this.extensions.get(this.defaultExtension);
	}

	public String getDefaultName() {
		return this.defaultExtension;
	}
	
	public List<String> getDevice(Request req) {
		List<String> res = new ArrayList<String>();
		String agent = req.getHeader("User-Agent");
		
		if (StringUtil.isEmpty(agent)) {
			res.add("simple");
			res.add("std");
			res.add("mobile");
			
			return res;
		}
		
		for (XElement el : this.devices) {
			boolean fnd = (el.findIndex("AnyAgent") > -1);
			
			if (!fnd) {
				for (XElement fel : el.selectAll("IfAgent")) {
					if (agent.contains(fel.getAttribute("Contains"))) {
						fnd = true;
						break;
					}
				}
			}
			
			if (fnd) {
				for (XElement fel : el.selectAll("TryDevice")) {
					String name = fel.getAttribute("Name");
					
					if (StringUtil.isNotEmpty(name)) 
						res.add(name);
				}
				
				break;
			}
		}
		
		return res;
	}
	
	public Class<? extends IViewBuilder> getBuilder(String format) {
		if (StringUtil.isEmpty(format))
			return null;
		
		return this.builderClasses.get(format);
	}
	
	public Class<? extends IContentInfo> getContentLoader(String format) {
		if (StringUtil.isEmpty(format))
			return null;
		
		return this.contentClasses.get(format);
	}

	/*
	public IViewParser getFormatParser(String fmt) {
		return this.formatParsers.get(fmt);
	}
	*/
	
}
