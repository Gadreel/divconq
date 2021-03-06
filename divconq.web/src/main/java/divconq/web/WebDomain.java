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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import w3.html.Code;
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
import w3.html.Q;
import w3.html.S;
import w3.html.Script;
import w3.html.Section;
import w3.html.Select;
import w3.html.Span;
import w3.html.Style;
import w3.html.Sub;
import w3.html.Sup;
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
import w3.html.Wbr;
import divconq.filestore.CommonPath;
import divconq.hub.DomainInfo;
import divconq.hub.DomainNameMapping;
import divconq.hub.Hub;
import divconq.hub.HubPackage;
import divconq.io.LocalFileStore;
import divconq.lang.op.OperationContext;
import divconq.log.Logger;
import divconq.net.ssl.SslHandler;
import divconq.struct.Struct;
import divconq.util.StringUtil;
import divconq.web.dcui.AdvElement;
import divconq.web.dcui.AdvForm;
import divconq.web.dcui.AdvText;
import divconq.web.dcui.CaptionedImage;
import divconq.web.dcui.ButtonLink;
import divconq.web.dcui.Document;
import divconq.web.dcui.FormButton;
import divconq.web.dcui.FormInstruction;
import divconq.web.dcui.Html5AppHead;
import divconq.web.dcui.HyperLink;
import divconq.web.dcui.ICodeTag;
import divconq.web.dcui.IncludeParam;
import divconq.web.dcui.IncludePart;
import divconq.web.dcui.LiteralText;
import divconq.web.dcui.Nodes;
import divconq.web.dcui.PagePart;
import divconq.web.dcui.TextPart;
import divconq.web.dcui.TitledSection;
import divconq.web.http.SslContextFactory;
import divconq.web.http.WebTrustManager;
import divconq.xml.XElement;
import divconq.xml.XNode;
import divconq.xml.XText;

public class WebDomain {
	protected String id = null;
	protected String alias = null;
	protected CommonPath homepath = null;
	protected WebSiteManager man = null;
	protected List<HubPackage> packagelist = null;
	
	protected XElement webconfig = null;
	
	protected TrustManager[] trustManagers = new TrustManager[1];
	protected DomainNameMapping<SslContextFactory> certs = null;
	
	protected Map<String, Class<? extends ICodeTag>> codetags = new HashMap<String, Class<? extends ICodeTag>>();
	
	protected String[] specialExtensions = new String[] { ".dcui.xml", ".html", ".gas" };	
	
	protected boolean appFramework = false;
	protected HtmlMode htmlmode = HtmlMode.Static;
	
	protected WebSite rootsite = null;
	protected Map<String, WebSite> sites = new HashMap<>();
	protected Map<String, WebSite> domainsites = new HashMap<>();

	public XElement getWebConfig() {
		return this.webconfig;
	}
	
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
	
	public HtmlMode getHtmlMode() {
		return this.htmlmode;
	}
	
	public WebSite getRootSite() {
		return this.rootsite;
	}
	
	public List<HubPackage> getPackagelist() {
		return this.packagelist;
	}
	
	public String[] getSpecialExtensions() {
		return this.specialExtensions;
	}
	
	public void init(DomainInfo domain, WebSiteManager man) {
		this.id = domain.getId();
		this.alias = domain.getAlias();
		this.man = man;
		
		this.rootsite = new WebSite("root", this);
		
		this.initialTags();

		this.reloadSettings();
	}

	public void reloadSettings() {
		this.homepath = new CommonPath("/index.html");
		this.appFramework = false;
		this.sites.clear();
		this.domainsites.clear();
		
		XElement settings = this.man.getModule().getLoader().getSettings();
		
		if (Logger.isDebug())
			Logger.debug("Reloading web domain settings: " + this.alias + " - " + settings);
		
		if (settings != null) {
			// UI = app or customer uses builder
			// UI = basic is just 'index.html' approach
			this.appFramework = (settings.hasAttribute("UI") && 
					("custom".equals(settings.getAttribute("UI").toLowerCase())));

			if (settings.hasAttribute("HomePath")) 
				this.homepath = new CommonPath(settings.getAttribute("HomePath"));
			else if (this.appFramework) 
				this.homepath = new CommonPath("/Home");		
			
			if (settings.hasAttribute("HtmlMode")) 
				try {
					this.htmlmode = HtmlMode.valueOf(settings.getAttribute("HtmlMode"));
				}
				catch(Exception x) {
					OperationContext.get().error("Unknown HTML Mode: " + settings.getAttribute("HtmlMode"));
				}
			
			for (XElement pel :  settings.selectAll("Site")) {
				String sname = pel.getAttribute("Name");
				
				if (StringUtil.isEmpty(sname))
					continue;
				
				if ("root".equals(sname)) {
					this.rootsite.init(pel);
				}
				else {
					WebSite site = new WebSite(sname, this);
					site.init(pel);
					this.sites.put(sname, site);
					
					for (XElement del : pel.selectAll("Domain")) {
						String dname = del.getAttribute("Name");					
						this.domainsites.put(dname, site);
					}
				}
			}
		}
		
		DomainInfo domain = Hub.instance.getDomainInfo(this.id);
		
		if (domain == null)
			return;
		
		XElement config = domain.getSettings();
		
		if (Logger.isDebug())
			Logger.debug("Checking web domain settings: " + this.alias + " - " + config);
		
		if (config == null) {
			// TODO improve so this works with domain settings - with or without Web
			
			// collect a list of the packages names enabled for this domain
			HashSet<String> packagenames = new HashSet<>();
			
			XElement webextconfig = this.man.getWebExtension().getLoader().getConfig();
			
			// add to the package name list all the packages turned on for entire web service
			if (webextconfig != null) 
				for (XElement pel :  webextconfig.selectAll("Package"))
					packagenames.add(pel.hasAttribute("Id") ? pel.getAttribute("Id") :pel.getAttribute("Name"));
			
			this.packagelist = Hub.instance.getResources().getPackages().buildLookupList(packagenames);
			
			if (Logger.isDebug())
				Logger.debug("Package list: " + this.alias + " - " + this.packagelist.size());
			
			return;
		}
		
		this.webconfig = config.selectFirst("Web");
		
		if (Logger.isDebug())
			Logger.debug("Checking web domain web settings: " + this.alias + " - " + this.webconfig);
		
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
		
		if (this.webconfig.hasAttribute("HtmlMode")) 
			try {
				this.htmlmode = HtmlMode.valueOf(this.webconfig.getAttribute("HtmlMode"));
			}
			catch(Exception x) {
				OperationContext.get().error("Unknown HTML Mode: " + this.webconfig.getAttribute("HtmlMode"));
			}
		
		for (XElement pel :  this.webconfig.selectAll("Site")) {
			String sname = pel.getAttribute("Name");
			
			if (StringUtil.isEmpty(sname))
				continue;
			
			if ("root".equals(sname)) {
				this.rootsite.init(pel);
			}
			else {
				WebSite site = new WebSite(sname, this);
				site.init(pel);
				this.sites.put(sname, site);
				
				for (XElement del : pel.selectAll("Domain")) {
					String dname = del.getAttribute("Name");					
					this.domainsites.put(dname, site);
				}
			}
		}
		
		// ------
		
		// collect a list of the packages names enabled for this domain
		HashSet<String> packagenames = new HashSet<>();
		
		// if not in the domain, then go look in the packages 
		for (XElement pel :  this.webconfig.selectAll("Package")) 
			packagenames.add(pel.getAttribute("Name"));
	
		XElement webextconfig = this.man.getWebExtension().getLoader().getConfig();
		
		// add to the package name list all the packages turned on for entire web service
		if (webextconfig != null) 
			for (XElement pel :  webextconfig.selectAll("Package"))
				packagenames.add(pel.hasAttribute("Id") ? pel.getAttribute("Id") :pel.getAttribute("Name"));
		
		this.packagelist = Hub.instance.getResources().getPackages().buildLookupList(packagenames);
		
		// ------
		
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

	public WebSite site(Request req) {
		String domain = req.getHeader("Host");
		
		if (domain.indexOf(':') > -1)
			domain = domain.substring(0, domain.indexOf(':'));
		
		WebSite site = this.domainsites.get(domain);
		
		return (site != null) ? site : this.rootsite;
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
		
		this.dynNotify();
	}
	
	// something changed in the www folder
	// force compiled content to reload from file system 
	public void dynNotify() {
		this.rootsite.dynNotify();
		
		if (this.sites != null)
			for (WebSite site : this.sites.values())
				site.dynNotify();
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
		if (Logger.isDebug())
			Logger.debug("Translating path: " + ctx.getRequest().getPath());
		
		this.translatePath(ctx);
		
		CommonPath path = ctx.getRequest().getPath();
	
		if (Logger.isDebug())
			Logger.debug("Process path: " + path);
		
		// translate above should take us home for root 
		if (path.isRoot()) { 
			OperationContext.get().errorTr(150001);
			return;
		}
		
		WebSite site = ctx.getSite();
		
		if (Logger.isDebug())
			Logger.debug("Site: " + (site != null ? site.getAlias() : "[missing]"));
		
		IOutputAdapter output = site.findFile(ctx);

		if (OperationContext.get().hasErrors() || (output == null)) {
			OperationContext.get().errorTr(150001);			
			return;
		}
		
		if (Logger.isDebug())
			Logger.debug("Executing adapter: " + output.getClass().getName());
		
		try {
			output.execute(ctx);
		} 
		catch (Exception x) {
			Logger.error("Unable to process web file: " + x);
			
			x.printStackTrace();
		}
	}

	public DomainInfo getDomainInfo() {
		return Hub.instance.getDomainInfo(this.id);
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
		
		String orguri = req.getOriginalPathUri();
		
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
			}
			
			if (orguri.equals(route.getAttribute("Path"))) {
				if (route.hasAttribute("RedirectPath"))
					return route.getAttribute("RedirectPath");
			}
		}
		
		if ((ssl == null) && Struct.objectToBoolean(web.getAttribute("ForceTls", "False"))) 
			return "https://" + host + ":" + web.getAttribute("TlsPort", defPort) + req.getOriginalPath(); 
		
		return null;
	}
	
	// Html, Qx, Xml parsing
	
    public Nodes parseXml(WebContext ctx, XElement container) {
    	Nodes nodes = new Nodes();
    	
    	for (XNode xnode : container.getChildren()) {
    		if (xnode instanceof XElement) {
    			this.parseElement(ctx, nodes, (XElement)xnode);
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
    public Nodes parseElement(WebContext ctx, XElement xel) {
    	Nodes nodes = new Nodes();
    	this.parseElement(ctx, nodes, xel);
    	return nodes;
    }
    
    // parses the children of container
    public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		if (xel == null)
			return;
		
		Class<? extends ICodeTag> tag = this.codetags.get(xel.getName());
		
		if (tag != null)
			try {
				tag.newInstance().parseElement(ctx, nodes, xel);
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
		this.codetags.put("code", Code.class);
		
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
		
		this.codetags.put("q", Q.class);
		
		this.codetags.put("section", Section.class);		
		this.codetags.put("select", Select.class);
		this.codetags.put("script", Script.class);
		this.codetags.put("s", S.class);
		this.codetags.put("strong", B.class);
		this.codetags.put("style", Style.class);		
		this.codetags.put("sub", Sub.class);
		this.codetags.put("sup", Sup.class);
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
		
		this.codetags.put("wbr", Wbr.class);
		
		// ==============================================================
		// above this point are std HTML tags, below are our enhanced tags
		// ==============================================================

		this.codetags.put("dcui", Document.class);		
		this.codetags.put("dcem", divconq.mail.Document.class);		
		
		this.codetags.put("Image", CaptionedImage.class);
		this.codetags.put("CaptionedImage", CaptionedImage.class);
		this.codetags.put("AdvText", AdvText.class);
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
		this.codetags.put("MD", AdvText.class);
		this.codetags.put("Style", Style.class);
		this.codetags.put("Script", Script.class);
		this.codetags.put("PagePart", PagePart.class);
		this.codetags.put("TextPart", TextPart.class);
		//this.codetags.put("ServerScript", ServerScript.class);
		this.codetags.put("TitledSection", TitledSection.class);

		// TODO these should eventually be migrated so they can be shown in html mode too
		// though they wouldn't work correctly, it would just be for show (unless we do a lot more)
		this.codetags.put("FieldContainer", AdvElement.class);
		this.codetags.put("TextInput", AdvElement.class);
		this.codetags.put("PasswordInput", AdvElement.class);
		this.codetags.put("YesNo", AdvElement.class);
		this.codetags.put("HorizRadioGroup", AdvElement.class);
		this.codetags.put("RadioGroup", AdvElement.class);
		this.codetags.put("CheckGroup", AdvElement.class);
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
