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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import divconq.filestore.CommonPath;
import divconq.hub.DomainInfo;
import divconq.hub.Hub;
import divconq.hub.HubEvents;
import divconq.hub.IEventSubscriber;
import divconq.io.FileStoreEvent;
import divconq.io.LocalFileStore;
import divconq.lang.op.FuncCallback;
import divconq.mod.ExtensionLoader;
import divconq.mod.IModule;
import divconq.net.IpAddress;
import divconq.util.MimeUtil;
import divconq.util.StringUtil;
import divconq.web.http.SslContextFactory;
import divconq.xml.XElement;

public class WebSiteManager {
	protected IModule module = null;
	protected IWebExtension webExtension = null;
	
	protected ConcurrentHashMap<String, WebDomain> dsitemap = new ConcurrentHashMap<String, WebDomain>();
	
	//protected String version = null;
	protected String defaultTlsPort = "443";
	
	protected List<SslContextFactory> tls = new ArrayList<>(); 
	
	// TODO when module is unloaded, clean up all references to classes

	protected Map<String,IWebMacro> macros = new HashMap<String,IWebMacro>();
	protected ValuesMacro vmacros = new ValuesMacro();
	
	protected List<XElement> devices = new ArrayList<XElement>();
	
	public String getDefaultTlsPort() {
		return this.defaultTlsPort;
	}
	
	/*
	public String getVersion() {
		return this.version;
	}
	*/
	
	public IModule getModule() {
		return this.module;
	}
	
	public void start(IModule module, XElement config) {
		this.module = module;

		if (config != null) {
	    	for (XElement scel : config.selectAll("SslContext")) {
	    		SslContextFactory tls = new SslContextFactory();
	    		tls.init(config, scel);
	    		this.tls.add(tls);
	    	}
			
			this.defaultTlsPort = config.getAttribute("DefaultTlsPort", this.defaultTlsPort);
			
			XElement settings = config.find("ViewSettings");
			
			if (settings != null) {
				// ideally we would only load in Hub level settings, try to use sparingly
				MimeUtil.load(settings);
				
				this.devices = settings.selectAll("DeviceRule");
				
				for (XElement macros : settings.selectAll("Macro")) {
					String name = macros.getAttribute("Name");
					
					if (StringUtil.isEmpty(name))
						continue;
					
					String bname = macros.getAttribute("Class");
					
					if (StringUtil.isNotEmpty(bname)) {
						Class<?> cls = Hub.instance.getClass(bname);   
						
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
		}
		
		for (ExtensionLoader el : module.getLoader().getExtensions()) {
			if (el.getExtension() instanceof IWebExtension) {
				this.webExtension = (IWebExtension) el.getExtension();
				break;
			}
		}
	
		// prepare extensions (web apps)
	
		//XElement lcf = module.getLoader().getConfig();
		
		/* TODO try to recreate this concept
		Adler32 ad = new Adler32();
		*/
		
		/*
		if (lcf != null)
	    	for(XElement node : lcf.selectAll("Extension")) {
	    		String name = node.getAttribute("Name");
	    		
	    		Bundle bundle = this.module.getLoader().getExtension(name);
	    		
	    		if (! (bundle instanceof ExtensionLoader))
	    			continue;

	    		IExtension ex = ((ExtensionLoader)bundle).getExtension();
	    		
	    		if (! (ex instanceof IWebExtension))
	    			continue;
	    		
	    		// only compute if this is a web extension
	    		/* TODO try to recreate this concept
	    		bundle.adler(ad);
	    		* /
	    		
	    		IWebExtension sm = (IWebExtension)ex;

	    		this.extensions.put(name, sm);
	    		
	    		if (this.defaultExtension == null)
	    			this.defaultExtension = name;
	    	}
				 */
		
		/* TODO try to recreate this concept
		this.version = Long.toHexString(ad.getValue());
		*/
		
		// ========================================================================
		

		/**
		 * - ./private/dcw/filetransferconsulting/www-preview/dcf/index.html
		 * - ./private/dcw/filetransferconsulting/www/dcf/index.html
		 * - ./public/dcw/filetransferconsulting/www-preview/dcf/index.html
		 * - ./public/dcw/filetransferconsulting/www/dcf/index.html
		 */			

		FuncCallback<FileStoreEvent> localfilestorecallback = new FuncCallback<FileStoreEvent>() {
			@Override
			public void callback() {
				this.resetCalledFlag();
				
				CommonPath p = this.getResult().getPath();
				
				//System.out.println(p);
				
				// only notify on www updates
				if (p.getNameCount() < 4) 
					return;
				
				// must be inside a domain or we don't care
				String mod = p.getName(0);
				String domain = p.getName(1);
				String section = p.getName(2);
				
				if (!"dcw".equals(mod) || (!"www".equals(section) && !"www-preview".equals(section) && !"feed".equals(section) && !"feed-preview".equals(section)))
					return;
				
				for (WebDomain wdomain : WebSiteManager.this.dsitemap.values()) {
					if (domain.equals(wdomain.getAlias())) {
						wdomain.dynNotify();
						
						// TODO after we merge DomainInfo and WebDomain features this will work better,
						// right now feed only gets imported if the domain has been loaded via HTTP(S) request
						/*
						if ("feed".equals(section) || "feed-preview".equals(section)) {
							Task task = new Task()
								.withWork(new IWork() {
									@Override
									public void run(TaskRun trun) {
										ImportWebsiteTool iutil = new ImportWebsiteTool();
										
										
										// TODO use domain path resolution
										iutil.importFeedFile(Paths.get("./public" + p), new OperationCallback() {
											@Override
											public void callback() {
												trun.complete();
											}
										});
									}
								})
								.withTitle("Importing feed " + p)
								.withBucket("ServicePool")		// only one at a time
								.withContext(new OperationContextBuilder()
									.withRootTaskTemplate()
									.withDomainId(wdomain.getId())
									.toOperationContext()
								);
							
							Hub.instance.getWorkPool().submit(task);
						}
						*/
						
						break;
					}
				}
			}
		};
		
		// register for file store events
		LocalFileStore pubfs = Hub.instance.getPublicFileStore();
		
		if (pubfs != null) 
			pubfs.register(localfilestorecallback);
		
		LocalFileStore privfs = Hub.instance.getPrivateFileStore();
		
		if (privfs != null) 
			privfs.register(localfilestorecallback);

		/**
		 * - ./packages/zCustomPublic/www/dcf/index.html
		 * - ./packages/dc/dcFilePublic/www/dcf/index.html
		 * - ./packages/dcWeb/www/dcf/index.html
		 */			

		FuncCallback<FileStoreEvent> localpackagecallback = new FuncCallback<FileStoreEvent>() {
			@Override
			public void callback() {
				for (WebDomain domain : WebSiteManager.this.dsitemap.values())
					domain.dynNotify();
				
				this.resetCalledFlag();
			}
		};
		
		// register for file store events
		LocalFileStore packfs = Hub.instance.getResources().getPackages().getPackageFileStore();
		
		if (packfs != null) 
			packfs.register(localpackagecallback);
		
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
		
		Hub.instance.subscribeToEvent(HubEvents.DomainConfigChanged, new IEventSubscriber() {			
			@Override
			public void eventFired(Object e) {
				DomainInfo di = (DomainInfo) e;
				
				for (WebDomain domain : WebSiteManager.this.dsitemap.values()) {
					if (di.getId().equals(domain.getId()))
						domain.settingsNotify();
				}
			}
		});
	}
	
	public WebDomain getDomain(String id) {
		DomainInfo di = Hub.instance.getDomainInfo(id);
		
		if (di != null) {
			WebDomain domain = this.dsitemap.get(di.getId());
			
			if (domain != null)
				return domain; 
			
			for (DomainInfo d : Hub.instance.getDomains().getDomains()) {
				if (d.getId().equals(id)) {
					domain = new WebDomain();
					domain.init(d, this);
					this.dsitemap.put(id, domain);
					
					return domain;
				}
			}
		}
		
		return null;
	}
	
	public void online() {
		this.dsitemap.clear();
	}	
	
	static public String resolveHost(Request req) {
		return WebSiteManager.resolveHost(req.getHeader("Host"));
	}
	
	static public String resolveHost(String dname) {
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
		return Hub.instance.getDomains().resolveDomainInfo(WebSiteManager.resolveHost(req));
	}
	
	public DomainInfo resolveDomainInfo(String dname) {
		return Hub.instance.getDomains().resolveDomainInfo(WebSiteManager.resolveHost(dname));
	}

	// only call this with normalized hostnames 
	public SslContextFactory findSslContextFactory(String hostname) {
		DomainInfo di = Hub.instance.getDomains().resolveDomainInfo(hostname);
		
		if (di != null) {
			WebDomain wd = this.getDomain(di.getId());
			
			if (wd != null) {
				SslContextFactory scf = wd.getSecureContextFactory(hostname);
				
				if (scf != null)
					return scf;
			}
		}
		
		for (SslContextFactory cf : this.tls)
			if (cf.keynameMatch(hostname))
				return cf;
		
		if (this.tls.size() > 0)
			return this.tls.get(0);
		
		return null;
	}

	public IWebMacro getMacro(String name) {
		if (this.vmacros.hasKey(name))
			return this.vmacros;
		
		return this.macros.get(name);
	}
	
	public IWebExtension getWebExtension() {
		return this.webExtension;
	}

	/*
	public IWebExtension getExtension(String name) {
		return this.extensions.get(name);
	}

	public String getDefaultName() {
		return this.defaultExtension;
	}
	*/
	
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
}
