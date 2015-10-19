package divconq.web;

import java.util.HashMap;
import java.util.Map;

import divconq.filestore.CommonPath;
import divconq.hub.Hub;
import divconq.io.CacheFile;
import divconq.io.LocalFileStore;
import divconq.lang.op.OperationContext;
import divconq.log.Logger;
import divconq.util.MimeUtil;
import divconq.web.ui.adapter.GasOutputAdapter;
import divconq.web.ui.adapter.SsiOutputAdapter;
import divconq.web.ui.adapter.StaticOutputAdapter;
import divconq.web.ui.adapter.DcuiOutputAdapter;
import divconq.xml.XElement;

public class WebSite {
	protected String alias = null;
	protected WebDomain domain = null;
	
	protected Map<String, DcuiOutputAdapter> dyncache = new HashMap<>();
	protected Map<String, DcuiOutputAdapter> dynpreviewcache = new HashMap<>();
	
	public WebDomain getDomain() {
		return this.domain;
	}

	public String getAlias() {
		return this.alias;
	}

	public WebSite(String alias, WebDomain domain) {
		this.alias = alias;
		this.domain = domain;
	}
	
	public void init(XElement config) {
	}
	
	public boolean isSharedSection(String section) {
		// TODO 
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
		
		/*
		System.out.println("x: " + Hub.instance.getResources());
		System.out.println("y: " + Hub.instance.getResources().getPackages());
		System.out.println("z: " + this.domain);
		System.out.println("w: " + this.domain.getPackagelist());
		*/
		
		return Hub.instance.getResources().getPackages().cacheLookupPath(this.domain.getPackagelist(), "/" + section + path);
	}

	public String getMimeType(String ext) {
		// TODO check settings Site before system
		
		return MimeUtil.getMimeType(ext);
	}
}
