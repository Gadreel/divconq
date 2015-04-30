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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.TrustManager;

import w3.html.A;
import w3.html.Article;
import w3.html.Aside;
import w3.html.B;
import w3.html.BlockQuote;
import w3.html.Body;
import w3.html.Br;
import w3.html.BrLf;
import w3.html.Button;
import w3.html.Div;
import w3.html.Em;
import w3.html.FieldSet;
import w3.html.Footer;
import w3.html.Form;
import w3.html.H1;
import w3.html.H2;
import w3.html.H3;
import w3.html.H4;
import w3.html.H5;
import w3.html.H6;
import w3.html.Head;
import w3.html.Header;
import w3.html.Hr;
import w3.html.Html;
import w3.html.I;
import w3.html.IFrame;
import w3.html.Img;
import w3.html.Input;
import w3.html.Label;
import w3.html.Legend;
import w3.html.Li;
import w3.html.Link;
import w3.html.Meta;
import w3.html.Nav;
import w3.html.NbSp;
import w3.html.Ol;
import w3.html.OptGroup;
import w3.html.Option;
import w3.html.P;
import w3.html.Pre;
import w3.html.Script;
import w3.html.Section;
import w3.html.Select;
import w3.html.Span;
import w3.html.Style;
import w3.html.TBody;
import w3.html.THead;
import w3.html.Table;
import w3.html.Td;
import w3.html.TextArea;
import w3.html.Th;
import w3.html.Title;
import w3.html.Tr;
import w3.html.U;
import w3.html.Ul;
import divconq.filestore.CommonPath;
import divconq.hub.DomainInfo;
import divconq.hub.DomainNameMapping;
import divconq.hub.Hub;
import divconq.io.LocalFileStore;
import divconq.lang.op.OperationContext;
import divconq.locale.LocaleInfo;
import divconq.locale.LocaleUtil;
import divconq.locale.Localization;
import divconq.net.ssl.SslHandler;
import divconq.struct.Struct;
import divconq.util.StringUtil;
import divconq.web.asset.AssetOutputAdapter;
import divconq.web.dcui.AdvElement;
import divconq.web.dcui.AdvForm;
import divconq.web.dcui.AssetImage;
import divconq.web.dcui.ButtonLink;
import divconq.web.dcui.Document;
import divconq.web.dcui.FormButton;
import divconq.web.dcui.FormInstruction;
import divconq.web.dcui.ServerScript;
import divconq.web.dcui.Html5AppHead;
import divconq.web.dcui.HyperLink;
import divconq.web.dcui.ICodeTag;
import divconq.web.dcui.IncludeParam;
import divconq.web.dcui.IncludePart;
import divconq.web.dcui.LiteralText;
import divconq.web.dcui.Nodes;
import divconq.web.dcui.PagePart;
import divconq.web.dcui.TitledSection;
import divconq.web.dcui.ViewOutputAdapter;
import divconq.web.dcui.ViewTemplateAdapter;
import divconq.web.http.SslContextFactory;
import divconq.web.http.WebTrustManager;
import divconq.xml.XElement;
import divconq.xml.XNode;
import divconq.xml.XText;

public class WebDomain {
	protected String id = null;
	protected String alias = null;
	protected String locale = null;		// domain level locale
	protected CommonPath homepath = null;
	protected WebSiteManager man = null;
	
	protected XElement webconfig = null;
	
	protected TrustManager[] trustManagers = new TrustManager[1];
	protected DomainNameMapping<SslContextFactory> certs = null;
	
	protected Map<String, IOutputAdapter> paths = new HashMap<>();
	protected Map<String, IOutputAdapter> previewpaths = new HashMap<>();
	
	protected Localization dictionary = null;
	
	protected Map<String, Class<? extends ICodeTag>> codetags = new HashMap<String, Class<? extends ICodeTag>>();
	
	protected String[] specialExtensions = new String[] { ".dcui.xml", ".gas", ".pui.xml", ".html" };	
	
	protected boolean appFramework = false;
	
	public String getId() {
		return this.id;
	}
	
	public String getAlias() {
		return this.alias;
	}
	
	public CommonPath getHomePath() {
		return this.homepath;
	}
	
	public boolean isAppFramework() {
		return this.appFramework;
	}
	
	public void init(DomainInfo domain, WebSiteManager man) {
		this.id = domain.getId();
		this.alias = domain.getAlias();
		this.man = man;
		
		this.initialTags();

		this.reloadSettings();
	}

	public void reloadSettings() {
		this.homepath = new CommonPath("/index.html");
		this.locale = LocaleUtil.getDefaultLocale();
		this.appFramework = false;
		
		DomainInfo domain = Hub.instance.getDomainInfo(this.id);
		
		if (domain == null)
			return;
		
		XElement config = domain.getSettings();
		
		if (config == null)
			return;
		
		this.webconfig = config.selectFirst("Web");
		
		if (this.webconfig == null) 
			return;
		
		// UI = app or customer uses builder
		// UI = basic is just 'index.html' approach
		this.appFramework = (this.webconfig.hasAttribute("UI") && 
				("custom".equals(this.webconfig.getAttribute("UI").toLowerCase())));

		if (this.webconfig.hasAttribute("HomePath")) 
			this.homepath = new CommonPath(this.webconfig.getAttribute("HomePath"));
		else if (this.appFramework) 
			this.homepath = new CommonPath("/Home");		
	
		if (this.webconfig.hasAttribute("Locale")) 
			this.locale = this.webconfig.getAttribute("Locale");
		
		WebTrustManager trustman = new WebTrustManager();
		trustman.init(this.webconfig);
		
		this.trustManagers[0] = trustman;
		
		LocalFileStore fs = Hub.instance.getPublicFileStore();
		
		if (fs == null)
			return;
		
		Path cpath = fs.getFilePath().resolve("dcw/" + this.getAlias() + "/config/certs");

		if (Files.notExists(cpath))
			return;
		
		this.certs = new DomainNameMapping<>();
		
		for (XElement cel : this.webconfig.selectAll("Certificate")) {
			SslContextFactory ssl = new SslContextFactory();
			ssl.init(cel, cpath.toString() + "/", trustManagers);
			this.certs.add(cel.getAttribute("Name"), ssl);
		}
	}

	// matchname might be a wildcard match
	public SslContextFactory getSecureContextFactory(String matchname) {
		if (this.certs != null)
			return this.certs.get(matchname);
		
		return null;
	}
	
	public void addCodeTag(String tag, Class<? extends ICodeTag> classdef) {
		this.codetags.put(tag, classdef);
	}
	
	// something changed in the config folder	
	public void settingsNotify() {
		this.reloadSettings();
		
		this.siteNotify();
	}
	
	// something changed in the www folder
	// force compiled content to reload from file system 
	public void siteNotify() {
		this.paths.clear();				
		this.previewpaths.clear();
	}

	public String getLocale() {
		 return this.locale;
	}

	public LocaleInfo getDefaultLocaleInfo() {
		return new LocaleInfo(this.locale);
	}
	
	public String tr(LocaleInfo locale, String token, Object... params) {
		Localization dict = this.dictionary;
		
		if (dict != null) {
			String res = dict.tr(locale.getName(), token, params);
			
			if (res != null)
				return res;
		}
		
		return LocaleUtil.tr(locale.getName(), token, params);
	}

	public void translatePath(WebContext ctx) {
		CommonPath path = ctx.getRequest().getPath();
		
		if (path.isRoot()) 
			ctx.getRequest().setPath(this.homepath);
	}
	
	public CommonPath getNotFound() {
		if (this.homepath != null)
			return this.homepath;

		return new CommonPath("/dcw/notfound.html");
	}
	
	public void execute(WebContext ctx) {
		this.translatePath(ctx);
		
		CommonPath path = ctx.getRequest().getPath();
	
		// translate above should take us home for root 
		if (path.isRoot()) { 
			OperationContext.get().errorTr(150001);
			return;
		}
		
		IOutputAdapter output = this.findFile(ctx);

		if (OperationContext.get().hasErrors() || (output == null)) {
			OperationContext.get().errorTr(150001);			
			return;
		}
		
		try {
			ctx.setAdapter(output);
			
			output.execute(ctx);
		} 
		catch (Exception x) {
			System.out.println("Unable to process web file: " + x);
			x.printStackTrace();
		}
		
	}

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
		return this.findFile(ctx.isPreview(), ctx.getRequest().getPath(), ctx.getExtension());
	}
	
	public IOutputAdapter findFile(boolean isPreview, CommonPath path, IWebExtension ext) {
		// =====================================================
		//  if request has an extension do specific file lookup
		// =====================================================
		
		// if we have an extension then we don't have to do the search below
		// never go up a level past a file (or folder) with an extension
		if (path.hasFileExtension()) {
			// check path map first
			IOutputAdapter ioa = isPreview ? this.previewpaths.get(path.toString()) :  this.paths.get(path.toString());
			
			if (ioa != null)
				return ioa;
			
			Path wpath = this.findFilePath(isPreview, path, ext);
			
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
		
		// we get here if we have no extension - thus we need to look for path match with specials
		int pdepth = path.getNameCount();
		
		// check path maps first - hopefully the request has been mapped at one of the levels
		while (pdepth > 0) {
			CommonPath ppath = path.subpath(0, pdepth);

			if (isPreview) {
				IOutputAdapter ioa = this.paths.get(ppath.toString());
				
				if (ioa != null)
					return ioa;
			}
			
			IOutputAdapter ioa = this.paths.get(ppath.toString());
			
			if (ioa != null)
				return ioa;
			
			pdepth--;
		}
		
		Path wpath = this.findFilePath(isPreview, path, ext);
		
		if (wpath != null) 
			return this.pathToAdapter(isPreview, path, wpath);
		
		OperationContext.get().errorTr(150007);		
		return null;
	}
	
	public Path findFilePath(boolean isPreview, CommonPath path, IWebExtension ext) {
		LocalFileStore pubfs = Hub.instance.getPublicFileStore();
		LocalFileStore pacfs = Hub.instance.getPackageFileStore();
		LocalFileStore prifs = Hub.instance.getPrivateFileStore();
		
		// =====================================================
		//  if request has an extension do specific file lookup
		// =====================================================
		
		// if we have an extension then we don't have to do the search below
		// never go up a level past a file (or folder) with an extension
		if (path.hasFileExtension()) {
			if (prifs != null) {
				// look in the domain's www-preview file system
				if (isPreview) {
					Path wpath = this.getWebFile(prifs, "/dcw/" + this.alias + "/www-preview", path);
					
					if (wpath != null) 
						return wpath;
				}
				
				// look in the domain's static file system
				Path wpath = this.getWebFile(prifs, "/dcw/" + this.alias + "/www", path);
				
				if ("galleries".equals(path.getName(0)) || "files".equals(path.getName(0)))
						wpath = this.getWebFile(prifs, "/dcw/" + this.alias + "/", path);
				
				if (wpath != null) 
					return wpath;
			}
			
			if (pubfs != null) {
				// look in the domain's www-preview file system
				if (isPreview) {
					Path wpath = this.getWebFile(pubfs, "/dcw/" + this.alias + "/www-preview", path);
					
					if (wpath != null) 
						return wpath;
				}
				
				// look in the domain's static file system
				Path wpath = this.getWebFile(pubfs, "/dcw/" + this.alias + "/www", path);
				
				if ("galleries".equals(path.getName(0)) || "files".equals(path.getName(0)))
						wpath = this.getWebFile(pubfs, "/dcw/" + this.alias + "/", path);
				
				if (wpath != null) 
					return wpath;
			}
			
			if ((pacfs != null) && (this.webconfig != null)) {
				// if not in the domain, then go look in the packages 
				for (XElement pel :  this.webconfig.selectAll("Package")) {
					Path wpath = this.getWebFile(pacfs, "/" + pel.getAttribute("Name") + "/www", path);
					
					if (wpath != null) 
						return wpath;
				}
			}
			
			if ((pacfs != null) && (ext != null) && (ext.getLoader().getConfig() != null)) {
				// if not in the domain, then go look in the packages (older config used Id not Name)
				for (XElement pel :  ext.getLoader().getConfig().selectAll("Package")) {
					Path wpath = this.getWebFile(pacfs, "/" + pel.getAttribute("Id") + "/www", path);
					
					if (wpath != null) 
						return wpath;
				}
			}
			
			// TODO not found file!!
			OperationContext.get().errorTr(150007);		
			return null;
		}
		
		// =====================================================
		//  if request does not have an extension look for files
		//  that might match this path or one of its parents
		//  using the special extensions
		// =====================================================
		
		// we get here if we have no extension - thus we need to look for path match with specials
		int pdepth = path.getNameCount();
		
		// check file system
		while (pdepth > 0) {
			CommonPath ppath = path.subpath(0, pdepth);
			
			if (prifs != null) {
				if (isPreview) {
					// look in the domain's www-preview file system
					Path wpath = this.getWebFile(prifs, "/dcw/" + this.alias + "/www-preview", ppath);
					
					if (wpath != null) 
						return wpath;
				}
				
				// look in the domain's static file system
				Path wpath = this.getWebFile(prifs, "/dcw/" + this.alias + "/www", ppath);
				
				if (wpath != null) 
					return wpath;
			}
			
			if (pubfs != null) {
				if (isPreview) {
					// look in the domain's www-preview file system
					Path wpath = this.getWebFile(pubfs, "/dcw/" + this.alias + "/www-preview", ppath);
					
					if (wpath != null) 
						return wpath;
				}
				
				// look in the domain's static file system
				Path wpath = this.getWebFile(pubfs, "/dcw/" + this.alias + "/www", ppath);
				
				if (wpath != null) 
					return wpath;
			}
			
			if ((pacfs != null) && (this.webconfig != null)) {
				// if not in the domain, then go look in the packages 
				for (XElement pel :  this.webconfig.selectAll("Package")) {
					Path wpath = this.getWebFile(pacfs, "/" + pel.getAttribute("Name") + "/www", ppath);
					
					if (wpath != null) 
						return wpath;
				}
			}
			
			if ((pacfs != null) && (ext != null) && (ext.getLoader().getConfig() != null)) {
				// if not in the domain, then go look in the packages (older config used Id not Name)
				for (XElement pel :  ext.getLoader().getConfig().selectAll("Package")) {
					Path wpath = this.getWebFile(pacfs, "/" + pel.getAttribute("Id") + "/www", path);
					
					if (wpath != null) 
						return wpath;
				}
			}
			
			pdepth--;
		}
		
		OperationContext.get().errorTr(150007);		
		return null;
	}
	
	public Path getWebFile(LocalFileStore lfs, String prefix, CommonPath path) {		// TODO support ZIP packages
		if (path.isRoot())
			return null;
		
		Path fl = Paths.get(lfs.getPath() + prefix + path);		// must be absolute path
		
		try {
			if (path.hasFileExtension()) {
				if (Files.exists(fl) && !Files.isDirectory(fl) || !Files.isHidden(fl) && Files.isReadable(fl)) 
					return fl;
				
				return null;
			}
			
			Path fld = fl.getParent();
				
			if (Files.notExists(fld)) 
				return null;
			
			for (String ext : this.specialExtensions) {
				String sppath = path.getFileName() + ext;
				
				fl = fld.resolve(sppath);
				
				if (Files.exists(fl)) {
					try {
						if (!Files.isDirectory(fl) && !Files.isHidden(fl) && Files.isReadable(fl))
							return fl;
					}
					catch (Exception x) {
					}
					
					return null;
				}
			}
		}
		catch (Exception x) {
		}
			
		return null;
	}

	public IOutputAdapter pathToAdapter(boolean isPreview, CommonPath path, Path filepath) {
		String wpathname = filepath.getFileName().toString();

		IOutputAdapter ioa = null;
		
		if (wpathname.endsWith(".dcui.xml"))
			ioa = new ViewOutputAdapter(this, path, filepath, isPreview);
		else if (wpathname.endsWith(".dcuis.xml"))
			ioa = new ViewTemplateAdapter(path, filepath);
		else
			ioa = new AssetOutputAdapter(path, filepath);
		
		if (isPreview)
			this.previewpaths.put(path.toString(), ioa);
		else
			this.paths.put(path.toString(), ioa);
		
		return ioa;
	}

	public String route(Request req, SslHandler ssl) {
		DomainInfo domain = Hub.instance.getDomainInfo(this.id);
		
		if (domain == null)
			return null;
		
		XElement config = domain.getSettings();
		
		if (config == null)
			return null;
		
		String host = req.getHeader("Host");
		String port = "";
		
		if (host.contains(":")) {
			int pos = host.indexOf(':');
			port = host.substring(pos);
			host = host.substring(0, pos);
		}
		
		XElement web = config.selectFirst("Web");
		
		if (web == null)
			return null;
		
		String defPort = this.man.getDefaultTlsPort();
		
		for (XElement route : web.selectAll("Route")) {
			if (host.equals(route.getAttribute("Name"))) {
				if (route.hasAttribute("RedirectPath"))
					return route.getAttribute("RedirectPath");
				
				if (!route.hasAttribute("ForceTls") && !route.hasAttribute("RedirectName"))
					continue;
				
				boolean tlsForce = Struct.objectToBoolean(route.getAttribute("ForceTls", "False"));				
				String rname = route.getAttribute("RedirectName");
				
				boolean changeTls = ((ssl == null) && tlsForce);
				
				if (StringUtil.isNotEmpty(rname) || changeTls) {
					String path = ((ssl != null) || tlsForce) ? "https://" : "http://";
					
					path += StringUtil.isNotEmpty(rname) ? rname : host;
					
					// if forcing a switch, use another port
					path += changeTls ? ":" + route.getAttribute("TlsPort", defPort) : port;
					
					return path + req.getOriginalPath(); 
				}
				
				// TODO support alternate home paths - HomePath
			}
		}
		
		if ((ssl == null) && Struct.objectToBoolean(web.getAttribute("ForceTls", "False"))) 
			return "https://" + host + ":" + web.getAttribute("TlsPort", defPort) + req.getOriginalPath(); 
		
		return null;
	}
	
	// Html, Qx, Xml parsing
	
    public Nodes parseXml(ViewOutputAdapter view, XElement container) {
    	Nodes nodes = new Nodes();
    	
    	for (XNode xnode : container.getChildren()) {
    		if (xnode instanceof XElement) {
    			this.parseElement(view, nodes, (XElement)xnode);
    		}
    		else if (xnode instanceof XText) {
    			String content = ((XText)xnode).getRawValue();
    			
    			if (!StringUtil.isEmpty(content))
    				nodes.add(new LiteralText(content));
    		}
    	}
    	
    	return nodes;
    }
    
    // parses the children of container
    public Nodes parseElement(ViewOutputAdapter view, XElement xel) {
    	Nodes nodes = new Nodes();
    	this.parseElement(view, nodes, xel);
    	return nodes;
    }
    
    // parses the children of container
    public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
		if (xel == null)
			return;
		
		Class<? extends ICodeTag> tag = this.codetags.get(xel.getName());
		
		if (tag != null)
			try {
				tag.newInstance().parseElement(view, nodes, xel);
			} 
			catch (Exception x) {
				// TODO Auto-generated catch block
				System.out.println("Site parse error: " + x);
			}
    }	
	
	protected void initialTags() {
		this.codetags.put("a", A.class);
		this.codetags.put("article", Article.class);
		this.codetags.put("aside", Aside.class);
		
		this.codetags.put("b", B.class);
		this.codetags.put("blockquote", BlockQuote.class);
		this.codetags.put("body", Body.class);
		this.codetags.put("button", Button.class);
		this.codetags.put("br", Br.class);
		this.codetags.put("brlf", BrLf.class);		
		
		this.codetags.put("canvas", AdvElement.class);
		
		this.codetags.put("div", Div.class);
		
		this.codetags.put("em", Em.class);
		
		this.codetags.put("fieldset", FieldSet.class);
		this.codetags.put("footer", Footer.class);
		this.codetags.put("form", Form.class);
		
		this.codetags.put("h1", H1.class);
		this.codetags.put("h2", H2.class);
		this.codetags.put("h3", H3.class);
		this.codetags.put("h4", H4.class);
		this.codetags.put("h5", H5.class);
		this.codetags.put("h6", H6.class);		
		this.codetags.put("head", Head.class);
		this.codetags.put("header", Header.class);
		this.codetags.put("html", Html.class);
		this.codetags.put("hr", Hr.class);
		
		this.codetags.put("i", I.class);
		this.codetags.put("iframe", IFrame.class);
		this.codetags.put("img", Img.class);
		this.codetags.put("input", Input.class);
		
		this.codetags.put("label", Label.class);
		this.codetags.put("legend", Legend.class);
		this.codetags.put("li", Li.class);		
		this.codetags.put("link", Link.class);
		
		this.codetags.put("meta", Meta.class);
		
		this.codetags.put("nbsp", NbSp.class);
		this.codetags.put("nav", Nav.class);
		
		this.codetags.put("ol", Ol.class);		
		this.codetags.put("optgroup", OptGroup.class);
		this.codetags.put("option", Option.class);
		
		this.codetags.put("p", P.class);
		this.codetags.put("pre", Pre.class);
		
		this.codetags.put("section", Section.class);		
		this.codetags.put("select", Select.class);
		this.codetags.put("script", Script.class);
		this.codetags.put("style", Style.class);		
		this.codetags.put("strong", B.class);
		this.codetags.put("span", Span.class);
		
		this.codetags.put("table", Table.class);
		this.codetags.put("tbody", TBody.class);		
		this.codetags.put("td", Td.class);		
		this.codetags.put("th", Th.class);
		this.codetags.put("thead", THead.class);		
		this.codetags.put("tr", Tr.class);
		this.codetags.put("textarea", TextArea.class);
		this.codetags.put("title", Title.class);

		this.codetags.put("u", U.class);
		this.codetags.put("ul", Ul.class);
		
		// ==============================================================
		// above this point are std HTML tags, below are our enhanced tags
		// ==============================================================

		this.codetags.put("dcui", Document.class);		
		
		this.codetags.put("AssetImage", AssetImage.class);
		this.codetags.put("Button", ButtonLink.class);
		this.codetags.put("WideButton", ButtonLink.class);
		this.codetags.put("SubmitButton", FormButton.class);
		this.codetags.put("Form", AdvForm.class);
		this.codetags.put("Html5Head", Html5AppHead.class);		
		this.codetags.put("IncludePart", IncludePart.class);
		//this.codetags.put("IncludeHolder", IncludeHolder.class);
		this.codetags.put("IncludeParam", IncludeParam.class);
		this.codetags.put("Link", HyperLink.class);
		this.codetags.put("LiteralText", LiteralText.class);
		this.codetags.put("Style", Style.class);
		this.codetags.put("Script", Script.class);
		this.codetags.put("PagePart", PagePart.class);
		this.codetags.put("ServerScript", ServerScript.class);
		this.codetags.put("TitledSection", TitledSection.class);

		// TODO these should eventually be migrated so they can be shown in html mode too
		// though they wouldn't work correctly, it would just be for show (unless we do a lot more)
		this.codetags.put("FieldContainer", AdvElement.class);
		this.codetags.put("TextInput", AdvElement.class);
		this.codetags.put("PasswordInput", AdvElement.class);
		this.codetags.put("YesNo", AdvElement.class);
		this.codetags.put("HorizRadioGroup", AdvElement.class);
		this.codetags.put("RadioGroup", AdvElement.class);
		this.codetags.put("RadioButton", AdvElement.class);
		this.codetags.put("RadioCheck", AdvElement.class);
		this.codetags.put("RadioSelect", AdvElement.class);
		this.codetags.put("Range", AdvElement.class);
		this.codetags.put("Select", AdvElement.class);
		this.codetags.put("TextArea", AdvElement.class);
		this.codetags.put("HiddenInput", AdvElement.class);
		this.codetags.put("FormInstruction", FormInstruction.class);
	}
}
