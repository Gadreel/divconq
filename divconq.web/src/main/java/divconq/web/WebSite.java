package divconq.web;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import divconq.filestore.CommonPath;
import divconq.hub.Hub;
import divconq.io.CacheFile;
import divconq.io.LocalFileStore;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationContextBuilder;
import divconq.lang.op.UserContext;
import divconq.locale.Dictionary;
import divconq.locale.ILocaleResource;
import divconq.locale.LocaleDefinition;
import divconq.log.Logger;
import divconq.util.MimeUtil;
import divconq.util.StringUtil;
import divconq.web.ui.adapter.GasOutputAdapter;
import divconq.web.ui.adapter.SsiOutputAdapter;
import divconq.web.ui.adapter.StaticOutputAdapter;
import divconq.web.ui.adapter.DcuiOutputAdapter;
import divconq.xml.XElement;

public class WebSite implements ILocaleResource {
	protected String alias = null;
	protected WebDomain domain = null;
	
	// these are only meaningful for sub sites
	protected SiteIntegration integration = SiteIntegration.Files;
	protected String locale = null;
	protected LocaleDefinition localedef = null;
	protected Dictionary dictionary = null;
	
	protected Map<String, LocaleDefinition> locales = new HashMap<>();
	protected Map<String, LocaleDefinition> domainlocales = new HashMap<>();
	
	protected Map<String, DcuiOutputAdapter> dyncache = new HashMap<>();
	protected Map<String, DcuiOutputAdapter> dynpreviewcache = new HashMap<>();
	
	public WebDomain getDomain() {
		return this.domain;
	}

	public String getAlias() {
		return this.alias;
	}
	
	public SiteIntegration getIntegration() {
		return this.integration;
	}
	
	@Override
	public Dictionary getDictionary() {
		if (this.dictionary != null)
			return this.dictionary;
		
		return this.getParentLocaleResource().getDictionary();
	}
	
	@Override
	public ILocaleResource getParentLocaleResource() {
		return this.domain.getDomainInfo();
	}
	
	@Override
	public String getDefaultLocale() {
		if (this.locale != null)
			return this.locale;
		
		return this.getParentLocaleResource().getDefaultLocale();
	}

	@Override
	public LocaleDefinition getLocaleDefinition(String name) {
		// TODO lookup definitions
		
		return new LocaleDefinition(name);
	}
	
	@Override
	public LocaleDefinition getDefaultLocaleDefinition() {
		if (this.localedef != null)
			return this.localedef;
		
		return this.getParentLocaleResource().getDefaultLocaleDefinition();
	}
	
	// 0 is best, higher the number the worse, -1 for not supported
	@Override
	public int rateLocale(String locale) {
		if ((this.localedef != null) && this.localedef.match(locale))
			return 0;
		
		int r = this.getParentLocaleResource().rateLocale(locale);
		
		if (r < 0)
			return -1;
		
		return r + 1;
	}

	public WebSite(String alias, WebDomain domain) {
		this.alias = alias;
		this.domain = domain;
	}
	
	public void init(XElement config) {
		this.dictionary = null;
		this.locales.clear();
		
		// for a sub-site, may have additional dict to load
		if (this != this.domain.getRootSite()) {
			LocalFileStore pubfs = Hub.instance.getPublicFileStore();
			
			Path cpath = pubfs.resolvePath("/dcw/" + this.domain.getAlias() + "/sites/" + this.alias + "/config");
			
			if ((cpath != null) && Files.exists(cpath)) {
				// dictionary
				
				Path dicpath = cpath.resolve("dictionary.xml");
		
				if (Files.exists(dicpath)) {
					this.dictionary = new Dictionary();
					this.dictionary.setParent(this.domain.getDomainInfo().getDictionary());
					this.dictionary.load(dicpath);
				}		
			}
		}
		
		if (config != null) {
			// this settings are only valid for sub sites
			if (this != this.domain.getRootSite()) {
				if (config.hasAttribute("Integration")) {
					try {
						this.integration = SiteIntegration.valueOf(config.getAttribute("Integration", "Files"));
					}
					catch (Exception x) {
						this.integration = SiteIntegration.Files;
					}
				}
			}			
			
			if (config.hasAttribute("Locale")) {
				this.locale = config.getAttribute("Locale");
				
				this.localedef = this.getLocaleDefinition(this.locale);
				
				// add the list of locales supported for this site
				this.locales.put(this.locale, this.localedef);
			}
			
			// these settings are valid for root and sub sites
			
			for (XElement pel : config.selectAll("Locale")) {
				String lname = pel.getAttribute("Name");
				
				if (StringUtil.isEmpty(lname))
					continue;

				LocaleDefinition def = this.getLocaleDefinition(lname);
				
				this.locales.put(lname, def);
				
				for (XElement del : pel.selectAll("Domain")) {
					String dname = del.getAttribute("Name");					
					
					if (StringUtil.isEmpty(lname))
						continue;
					
					this.domainlocales.put(dname, def);
				}
			}
		}
	}

	public Cookie resolveLocale(HttpContext context, UserContext usr, OperationContextBuilder ctxb) {
		if (this.locales.size() == 0) {
			// make sure we have at least 1 locale listed for the site
			String lvalue = this.getDefaultLocale();
			
			// add the list of locales supported for this site
			this.locales.put(lvalue, this.getLocaleDefinition(lvalue));
		}

		LocaleDefinition locale = null;

		// see if the path indicates a language
		CommonPath path = context.getRequest().getPath();
		
		if (path.getNameCount() > 0)  {
			String lvalue = path.getName(0);
			
			locale = this.locales.get(lvalue);
			
			// extract the language from the path
			if (locale != null)
				context.getRequest().setPath(path.subpath(1));
		}

		// but respect the cookie if it matches something though
		Cookie langcookie = context.getRequest().getCookie("dcLang");
		
		if (locale == null) {
			if (langcookie != null) {
				String lvalue = langcookie.value();
				
				// if everything checks out set the op locale and done
				if (this.locales.containsKey(lvalue)) {
					ctxb.withOperatingLocale(lvalue);
					return null;
				}
				
				locale = this.getLocaleDefinition(lvalue);
				
				// use language if variant - still ok and done
				if (locale.hasVariant()) {
					if (this.locales.containsKey(locale.getLanguage())) {
						ctxb.withOperatingLocale(lvalue);		// keep the variant part, it may be used in places on site - supporting a lang implicitly allows all variants
						return null;
					}
				}
				
				// otherwise ignore the cookie, will replace it
			}
		}
		
		// see if the domain is set for a specific language
		if (locale == null) {
			String domain = context.getRequest().getHeader("Host");
			
			if (domain.indexOf(':') > -1)
				domain = domain.substring(0, domain.indexOf(':'));
			
			locale = this.domainlocales.get(domain);
		}
		
		// see if the user has a preference
		if (locale == null) {
			String lvalue = usr.getLocale();
			
			if (StringUtil.isNotEmpty(lvalue)) 
				locale = this.locales.get(lvalue);
		}
		
		// if we find any locale at all then to see if it is the default
		// if not use it, else use the default
		if ((locale != null) && !locale.equals(this.getDefaultLocaleDefinition())) {
			ctxb.withOperatingLocale(locale.getName());
			return new DefaultCookie("dcLang", locale.getName());
		}
		
		// clear the cookie if we are to use default locale
		if (langcookie != null) 
			return new DefaultCookie("dcLang", "");
		
		// we are using default locale, nothing more to do
		return null;
	}
	
	public boolean isSharedSection(String section) {
		if (this.integration == SiteIntegration.None)
			return false;
		
		if (this.integration == SiteIntegration.Full)
			return true;

		if ("files".equals(section) || "galleries".equals(section))
			return true;
		
		return false;
	}
	
	// something changed in the www folder
	// force compiled content to reload from file system 
	public void dynNotify() {
		this.dyncache.clear();				
		this.dynpreviewcache.clear();
	}

	/*
	public IOutputAdapter findFile(CommonPath path, boolean isPreview) {
		return this.domain.findFile(this, path, isPreview);
	}
	*/

	/**
	 * File paths come in as /dcf/index.html but really they are in -  
	 * 
	 * Domain Path Map:
	 * 		"/dcf/index.html"
	 * 
	 * Domain Private Phantom Files:							(draft/preview mode files)
	 * 		./private/dcw/[domain id]/www-preview/dcf/index.html
	 * 
	 * Domain Private Override Files:
	 * 		./private/dcw/[domain id]/www/dcf/index.html
	 * 
	 * Domain Phantom Files:                           			(draft/preview mode files)
	 * 		./public/dcw/[domain id]/www-preview/dcf/index.html
	 * 
	 * Domain Override Files:
	 * 		./public/dcw/[domain id]/www/dcf/index.html
	 * 
	 * Package Files:
	 * 		./packages/[package id]/www/dcf/index.html
	 * 
	 * Example:
	 * - ./private/dcw/filetransferconsulting/www-preview/dcf/index.html
	 * - ./private/dcw/filetransferconsulting/www/dcf/index.html
	 * - ./public/dcw/filetransferconsulting/www-preview/dcf/index.html
	 * - ./public/dcw/filetransferconsulting/www/dcf/index.html
	 * - ./packages/zCustomPublic/www/dcf/index.html
	 * - ./packages/dc/dcFilePublic/www/dcf/index.html
	 * - ./packages/dcWeb/www/dcf/index.html
	 * 
	 * 
	 * @param ctx
	 * @return an adapter that can execute to generate web response
	 */	
	
	public IOutputAdapter findFile(WebContext ctx) {		
		return this.findFile(ctx.getRequest().getPath(), ctx.isPreview());
	}
	
	public IOutputAdapter findFile(CommonPath path, boolean isPreview) {
		// =====================================================
		//  if request has an extension do specific file lookup
		// =====================================================
		
		if (Logger.isDebug())
			Logger.debug("find file before ext check: " + path + " - " + isPreview);
		
		// if we have an extension then we don't have to do the search below
		// never go up a level past a file (or folder) with an extension
		if (path.hasFileExtension()) {
			CacheFile wpath = this.findFilePath(path, isPreview);
			
			if (wpath != null) 
				return this.pathToAdapter(isPreview, path, wpath);
			
			// TODO not found file!!
			OperationContext.get().errorTr(150007);		
			return null;
		}
		
		// =====================================================
		//  if request does not have an extension look for files
		//  that might match this path or one of its parents
		//  using the special extensions
		// =====================================================
		
		if (Logger.isDebug())
			Logger.debug("find file before dyn check: " + path + " - " + isPreview);
		
		// we get here if we have no extension - thus we need to look for path match with specials
		int pdepth = path.getNameCount();
		
		// check path maps first - hopefully the request has been mapped at one of the levels
		while (pdepth > 0) {
			CommonPath ppath = path.subpath(0, pdepth);

			if (isPreview) {
				IOutputAdapter ioa = this.dynpreviewcache.get(ppath.toString());
				
				if (ioa != null)
					return ioa;
			}
			
			IOutputAdapter ioa = this.dyncache.get(ppath.toString());
			
			if (ioa != null)
				return ioa;
			
			pdepth--;
		}
		
		if (Logger.isDebug())
			Logger.debug("find file not cached: " + path + " - " + isPreview);
		
		// if not in dyncache then look on file system
		CacheFile wpath = this.findFilePath(path, isPreview);
		
		if (wpath == null) {
			OperationContext.get().errorTr(150007);		
			return null;
		}			
		
		if (Logger.isDebug())
			Logger.debug("find file path: " + wpath + " - " + path + " - " + isPreview);
		
		return this.pathToAdapter(isPreview, path, wpath);
	}

	public IOutputAdapter pathToAdapter(boolean isPreview, CommonPath path, CacheFile cfile) {
		IOutputAdapter ioa = null;
		
		String filename = cfile.getPath();
		
		HtmlMode hmode = this.domain.getHtmlMode();
		
		if (filename.endsWith(".dcui.xml") || filename.endsWith(".dcuis.xml")) {
			DcuiOutputAdapter voa = new DcuiOutputAdapter();
			
			(isPreview ? this.dynpreviewcache : this.dyncache).put(path.toString(), voa);
			
			ioa = voa;
		}
		else if (filename.endsWith(".shtml") || ((hmode == HtmlMode.Ssi) && filename.endsWith(".html"))) {
			ioa = new SsiOutputAdapter();
		}		
		else if (filename.endsWith(".gas")) {
			ioa = new GasOutputAdapter();		
		}
		// certain resource types cannot be delivered
		else if (filename.endsWith(".class")) {
			OperationContext.get().errorTr(150001);
			return null;
		}
		else {
			ioa = new StaticOutputAdapter();
		}
		
		ioa.init(this, cfile, path, isPreview);
		
		return ioa;
	}
	
	public CacheFile findFilePath(CommonPath path, boolean isPreview) {
		// figure out which section we are looking in
		String sect = "www";
		
		if ("files".equals(path.getName(0)) || "galleries".equals(path.getName(0))) {
			sect = path.getName(0);
			path = path.subpath(1);
		}
		
		if (Logger.isDebug())
			Logger.debug("find file path: " + path + " in " + sect);
		
		// =====================================================
		//  if request has an extension do specific file lookup
		// =====================================================
		
		// if we have an extension then we don't have to do the search below
		// never go up a level past a file (or folder) with an extension
		if (path.hasFileExtension()) 
			return this.findSectionFile(sect, path.toString(), isPreview);
		
		// =====================================================
		//  if request does not have an extension look for files
		//  that might match this path or one of its parents
		//  using the special extensions
		// =====================================================
		
		if (Logger.isDebug())
			Logger.debug("find file path dyn: " + path + " in " + sect);
		
		// we get here if we have no extension - thus we need to look for path match with specials
		int pdepth = path.getNameCount();
		
		// check file system
		while (pdepth > 0) {
			CommonPath ppath = path.subpath(0, pdepth);
			
			for (String ext : this.domain.getSpecialExtensions()) {
				CacheFile cfile = this.findSectionFile(sect, ppath.toString() + ext, isPreview);
				
				if (cfile != null)
					return cfile;
			}
			
			pdepth--;
		}
		
		OperationContext.get().errorTr(150007);		
		return null;
	}

	/*
	public CacheFile findSectionFile(WebContext ctx, String section, String path) {		
		return this.findSectionFile(ctx.getSite(), section, path, ctx.isPreview());
	}
	*/
	
	public CacheFile findSectionFile(String section, String path, boolean isPreview) {
		if (Logger.isDebug())
			Logger.debug("find section file: " + path + " in " + section);
		
		LocalFileStore pubfs = Hub.instance.getPublicFileStore();
		LocalFileStore prifs = Hub.instance.getPrivateFileStore();
		
		// for a sub-site, check first in the site folder
		
		if (this != this.domain.getRootSite()) {
			if (Logger.isDebug())
				Logger.debug("find section file, check site: " + path + " in " + section);
			
			if (prifs != null) {
				if (isPreview) {
					CacheFile cfile = prifs.cacheResolvePath("/dcw/" + this.domain.getAlias() + "/sites/" + this.alias + "/" + section + "-preview" + path);

					if (cfile != null) 
						return cfile;
				}
				
				CacheFile cfile = prifs.cacheResolvePath("/dcw/" + this.domain.getAlias() + "/sites/" + this.alias + "/" + section + path);
				
				if (cfile != null) 
					return cfile;
			}
			
			if (pubfs != null) {
				if (isPreview) {
					CacheFile cfile = pubfs.cacheResolvePath("/dcw/" + this.domain.getAlias() + "/sites/" + this.alias + "/" + section + "-preview" + path);

					if (cfile != null) 
						return cfile;
				}
				
				CacheFile cfile = pubfs.cacheResolvePath("/dcw/" + this.domain.getAlias() + "/sites/" + this.alias + "/" + section + path);
				
				if (cfile != null) 
					return cfile;
			}
			
			// if not shared then jump right to modules for resource
			if (!this.isSharedSection(section)) 
				return Hub.instance.getResources().getPackages().cacheLookupPath(this.domain.getPackagelist(), "/" + section + path);
		}
		
		// now check the root site folders
		
		// TODO Each site's special files (dcui, dcf, html, shtml, gas) are completely separate - supplemental files like js, css, imgs, etc may be integrated depending on site settings.

		if (Logger.isDebug())
			Logger.debug("find section file, check root: " + path + " in " + section);
		
		if (prifs != null) {
			if (isPreview) {
				CacheFile cfile = prifs.cacheResolvePath("/dcw/" + this.domain.getAlias() + "/" + section + "-preview" + path);

				if (cfile != null) 
					return cfile;
			}
			
			CacheFile cfile = prifs.cacheResolvePath("/dcw/" + this.domain.getAlias() + "/" + section + path);
			
			if (cfile != null) 
				return cfile;
		}
		
		if (pubfs != null) {
			if (isPreview) {
				CacheFile cfile = pubfs.cacheResolvePath("/dcw/" + this.domain.getAlias() + "/" + section + "-preview" + path);

				if (cfile != null) 
					return cfile;
			}
			
			CacheFile cfile = pubfs.cacheResolvePath("/dcw/" + this.domain.getAlias() + "/" + section + path);
			
			if (cfile != null) 
				return cfile;
		}
		
		if (Logger.isDebug())
			Logger.debug("find section check packages: " + path + " in " + section);
		
		return Hub.instance.getResources().getPackages().cacheLookupPath(this.domain.getPackagelist(), "/" + section + path);
	}

	public String getMimeType(String ext) {
		// TODO check settings Site before system - no move to domain, domain is where settings / data go
		
		return MimeUtil.getMimeType(ext);
	}
	
	public boolean getMimeCompress(String mime) {
		// TODO check settings Site before system - no move to domain, domain is where settings / data go
		
		return MimeUtil.getMimeCompress(mime);
	}
}
