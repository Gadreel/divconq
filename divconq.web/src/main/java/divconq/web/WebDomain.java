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

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import w3.html.A;
import w3.html.Article;
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
import w3.html.Table;
import w3.html.Td;
import w3.html.TextArea;
import w3.html.Th;
import w3.html.Title;
import w3.html.Tr;
import w3.html.U;
import w3.html.Ul;
import divconq.hub.Hub;
import divconq.interchange.CommonPath;
import divconq.io.ByteBufWriter;
import divconq.io.FileStoreEvent;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationResult;
import divconq.locale.LocaleInfo;
import divconq.locale.LocaleUtil;
import divconq.locale.Localization;
import divconq.log.Logger;
import divconq.net.NetUtil;
import divconq.util.StringUtil;
import divconq.view.ICodeTag;
import divconq.view.IncludeHolder;
import divconq.view.IncludeParam;
import divconq.view.IncludePart;
import divconq.view.LiteralText;
import divconq.view.Nodes;
import divconq.view.html.AssetImage;
import divconq.view.html.Html5Head;
import divconq.view.html.HyperLink;
import divconq.xml.XElement;
import divconq.xml.XNode;
import divconq.xml.XText;
import divconq.xml.XmlReader;

public class WebDomain implements IWebDomain {
	protected String id = null;
	protected IWebExtension extension = null;
	protected Map<String, ViewInfo> views = new HashMap<String, ViewInfo>();	
	
	protected Localization dictionary = null;
	
	protected Map<String,Class<? extends IViewBuilder>> builderClasses = new HashMap<String,Class<? extends IViewBuilder>>();
	protected Map<String,Class<? extends IContentInfo>> contentClasses = new HashMap<String,Class<? extends IContentInfo>>();
	//protected Map<String,IViewParser> formatParsers = new HashMap<String,IViewParser>();
	
	protected Map<String, Map<String, Class<? extends ICodeTag>>> codetags = new HashMap<String, Map<String,Class<? extends ICodeTag>>>();
	
	@Override
	public String getId() {
		return this.id;
	}
	
	public Class<? extends ICodeTag> getCodeTag(String format, String name) {
		Map<String, Class<? extends ICodeTag>> fmt = this.codetags.get(format);
		
		if (fmt != null) 
			return fmt.get(name);
					
		return null;
	}
	
	@Override
	public IWebExtension getExtension() {
		return this.extension;
	}

	@Override
	public void init(IWebExtension ext, String id) {
		this.id = id;
		this.extension = ext;
		
		this.initialTags();

		XElement settings = this.extension.getLoader().getSettings();
		
		if (settings != null) {		
			for (XElement builder : settings.selectAll("Format")) {
				String fmt = builder.getAttribute("Name");
				
				if (StringUtil.isEmpty(fmt))
					continue;
				
				String bname = builder.getAttribute("BuilderClass");
				
				if (StringUtil.isNotEmpty(bname)) {
					Class<?> cls = this.extension.getLoader().getClass(bname);
					
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
					Class<?> cls = this.extension.getLoader().getClass(cname);
					
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
					Class<?> cls = this.extension.getLoader().getClass(pname);
					
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
		}		
		
		// load the site as provided in the file store
		/* TODO restore domain level files
		File fsview = new File("./files/" + this.extension.getAppName() + "/" + id + "/View");
		
		if (fsview.exists() && fsview.isDirectory()) {
			Collection<File> files = FileUtils.listFiles(fsview, new String[] { "xml" }, true);
			
			String fsname = fsview.getPath();
			
			for (File f : files) {
				String fname = f.getPath(); 
				
				if (fname.endsWith(".view.xml") || fname.endsWith(".part.xml"))
					this.compileLocalView(fname.substring(fsname.length() - 5).replace('\\', '/'), f.toPath());
			}
		}
		*/
		
		// load as though a site
		
		this.siteNotify();		
	}

	public void addCodeTag(String kind, String tag, Class<? extends ICodeTag> classdef) {
		Map<String, Class<? extends ICodeTag>> tagmap = this.codetags.get(kind);
		
		if (tagmap == null) {
			tagmap = new HashMap<String, Class<? extends ICodeTag>>();
			this.codetags.put(kind, tagmap);
		}
		
		tagmap.put(tag, classdef);
	}

	public void compileLocalView(String path, Path view) {
		try {
			FuncResult<XElement> xres = XmlReader.loadFile(view.toFile(), true);
			
			if (xres.hasErrors()) {
				System.out.println("Error compiling local view: " + xres.getMessages());
				return;
			}
			
			this.compileView(path, xres.getResult()); 
		}
		catch (Exception x) {
			// TODO log
			System.out.println("Error on View file (" + path + "): " + x);
		}
	}
	
	public void compileView(String path, XElement doc) {
		if (doc == null)
			doc = new XElement("HtmlOutput",
					new XElement("Content", 
							new XElement("html", 
									new XElement("body", "Parse Error!!")
							)
					)
			);
		
		try {			
			ViewInfo info = new ViewInfo(this);
			
			if (!info.load(doc)) {
				doc = new XElement("HtmlOutput",
						new XElement("Content", 
								new XElement("html", 
										new XElement("body", "Compile Error!!")
								)
						)
				);
				
				info.load(doc);
			}
			
			this.views.put(path, info);
		}
		catch (Exception x) {
			// TODO log
			System.out.println("Compile error on View (" + path + "): " + x);
		}
	}

	/*
	public void load(RecordStruct site) {
		// no default behavior
	}
	*/

	@Override
	public void fileNotify(FileStoreEvent result) {
		CommonPath p = result.getPath();
		
		// check for views only, we don't cache others yet
		if ((p.getNameCount() < 6) || !p.getFileName().endsWith(".view.xml")) 
			return;

		if (!"View".equals(p.getName(1)))
			return;
		
		if (result.isDeleted()) {
			System.out.println("Forget: " + p);
			this.views.remove(p.getName(1));
		}
		
		else {
			System.out.println("Update: " + p);
			
			this.compileLocalView(p.subpath(1).toString(), result.getFile());
		}
	}

	// load web site from local files
	
	@Override
	public void siteNotify() {
		/*
		 * 
		 *  TODO
		try {
			File site = new File("./files/" + this.extension.getAppName() + "/Site_" + this.id + ".xml");
			
			if (site.exists()) {
				InputStream dstrm = new FileInputStream(site);
				RecordStruct res = (RecordStruct) CompositeParser.parseXml(dstrm);
				dstrm.close();
				
				if (res != null)
					this.load(res);
			}
		}
		catch (Exception x) {
			// TODO
		}
					*/
	}
	
	public Class<? extends IViewBuilder> getBuilder(String format) {
		if (this.builderClasses.containsKey(format))
			return this.builderClasses.get(format);
		
		return WebSiteManager.instance.getBuilder(format);
	}
	
	public Class<? extends IContentInfo> getContentLoader(String fmt) {
		if (this.contentClasses.containsKey(fmt))
			return this.contentClasses.get(fmt);
		
		return WebSiteManager.instance.getContentLoader(fmt);
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
		
		if (path.getNameCount() < 2) 
			ctx.getRequest().setPath(new CommonPath("/dcw/index.html"));
	}
	
	public CommonPath getNotFound() {
		return new CommonPath("/dcw/notfound.html");
	}
	
	@Override
	public OperationResult execute(WebContext ctx) {
		OperationResult res = new OperationResult();
		
		this.translatePath(ctx);
		
		CommonPath path = ctx.getRequest().getPath();
		
		//System.out.println("h3: ");
	
		if (path.getNameCount() < 2) {
			res.errorTr(150001);			
		}
		else {
			/* phase out Asset/View concept
			String source = path.getName(1);
			
			//System.out.println("h4: ");
			
			if (source.startsWith("View")) 
				res.copyMessages(this.executeView(ctx));
			else if (source.startsWith("Asset")) 
				res.copyMessages(this.executeAsset(ctx));
			else
			*/
			
			this.executeFile(ctx);
		}
		
		return res;
	}

	public OperationResult executeView(WebContext ctx) {
		OperationResult res = new OperationResult();
		
		FuncResult<ViewInfo> infores = this.getView(ctx, ctx.getRequest().getPath(), "view");

		if (infores.hasErrors()) {
			res.errorTr(150001);	
			
			infores = this.getView(ctx, this.getNotFound(), "view");
			
			if (infores.hasErrors()) 
				return res;
		}

		try {
			infores.getResult().getBuilder().execute(ctx);
		} 
		catch (Exception x) {
			res.errorTr(150006, x);
		}
		
		return res;
	}
	
	protected ViewInfo findView(String path, WebContext ctx) {
		return this.views.get(path);
	}

	// type = only "view" or "part" allowed
	@Override
	public FuncResult<ViewInfo> getView(WebContext ctx, CommonPath path, String type) {
		FuncResult<ViewInfo> res = new FuncResult<ViewInfo>();

		int pdepth = path.getNameCount() - 1;
		
		while (pdepth > 0) {
			String fpath = path.subpath(1, pdepth) + "." + type + ".xml";
			
			ViewInfo info = this.findView(fpath, ctx);
			
			if (info != null)  {
				res.setResult(info);
				return res;
			}
							
			// if not in the domain, then go look in the JAR
			
			try {
				FuncResult<XElement> xmlres = this.extension.getBundle().getResourceAsXml(fpath, true);
				
				if (xmlres.hasErrors()) {
					// don't log 133, just means not found
					if (xmlres.getCode() != 133) {
						System.out.println("Parse error loading: " + fpath + "\nMessage: " + xmlres.getMessage());		// TODO logging
						res.errorTr(150009, fpath);
					}
				}
				else {
					info = new ViewInfo(this);
	
					// TODO switch to FuncResult model
					if (!info.load(xmlres.getResult())) {
						System.out.println("Compile error loading: " + fpath);  // TODO logging
						res.errorTr(150009, fpath);
					}
					else {
						this.views.put(fpath, info);
						res.setResult(info);
						return res;
					}
				}
			}
			catch (Exception x) {
				System.out.println("Compile error loading: " + fpath + "\nMessage: " + x.getMessage());  // TODO logging
				res.errorTr(150009, x);
			}
			
			pdepth--;
		}
		
		res.errorTr(150008);		
		return res;
	}

	/*
	public OperationResult executeAsset(WebContext ctx) {
		OperationResult res = new OperationResult();
		
		FuncResult<AssetInfo> assres = this.getAsset(ctx);

		if (assres.hasErrors()) {
			res.errorTr(150001);			
			return res;
		}
		
		res.copyMessages(this.writeFile(ctx, assres.getResult()));
		
		return res;
	}
	
	protected AssetInfo findAsset(String path, WebContext ctx) {
		return null;
	}

	public FuncResult<AssetInfo> getAsset(WebContext ctx) {
		FuncResult<AssetInfo> res = new FuncResult<AssetInfo>();
		
		CommonPath path = ctx.getRequest().getPath();		
		int pdepth = path.getNameCount();
		
		while (pdepth > 0) {
			CommonPath fpath = path.subpathAbs(1, pdepth);
			
			String sfpath = fpath.toString();
			
			// look at in memory assets
			
			AssetInfo info = this.findAsset(sfpath, ctx);
			
			if (info != null)  {
				res.setResult(info);
				return res;
			}
			
			// TODO consider compressing files
			
			// look in the domain's file system
			
			Path fass = Paths.get("./files/" + this.extension.getAppName() + "/" + this.id + sfpath);
			
			if (Files.exists(fass))
				try {
					AssetInfo ai = new AssetInfo(ctx, path, fass, Files.getLastModifiedTime(fass).toMillis());
					this.embellishDiskAsset(ai);
					res.setResult(ai);
					return res;
				} 
				catch (IOException x) {
				}

			// if not in the domain, then go look in the JAR 

			if (this.extension.getBundle().hasFileEntry(sfpath)) {
				byte[] mem = this.extension.getBundle().findFileEntry(sfpath);
				AssetInfo ai = new AssetInfo(fpath, mem, WebSiteManager.instance.getModule().startTime());
				this.embellishDiskAsset(ai);
				res.setResult(ai);
				return res;
			}
			
			pdepth--;
		}
		
		res.errorTr(150005);		
		return res;
	}
	*/

	protected void embellishDiskAsset(AssetInfo ai) {
	}

	public OperationResult executeFile(WebContext ctx) {
		OperationResult res = new OperationResult();
		
		FuncResult<AssetInfo> assres = this.getFile(ctx);

		if (assres.hasErrors()) {
			res.errorTr(150001);			
			return res;
		}
		
		this.writeFile(ctx, assres.getResult());
		
		return res;
	}

	/**
	 * File paths come in as /ncc/demo but really they are in -  
	 * 
	 * Domain Cache:
	 * 		-
	 * 
	 * Domain:
	 * 		./domain/[domain id]/www/ncc/demo.html				- addition of /ncc reduces conflict between file names in different extensions 
	 * 
	 * Cache
	 * 		-
	 * 
	 * Package:
	 * 		./[package id]/www/ncc/demo.html          (or .jss or .css or .js or .php, etc)
	 * 
	 * JAR:
	 * 		./[jar file - extension lib]/www/ncc/demo.html
	 * 
	 * @param ctx
	 * @return
	 */
	
	// TODO consider compressing files
	public FuncResult<AssetInfo> getFile(WebContext ctx) {		
		FuncResult<AssetInfo> res = new FuncResult<AssetInfo>();
		
		CommonPath path = ctx.getRequest().getPath();
		//CommonPath fpath = path.subpathAbs(1);
		
		/*
		// loop in the domain's file system
		// TODO review loading from domain
		
		String sfpath = fpath.toString();
		Path fass = Paths.get("./files/" + this.extension.getAppName() + "/" + this.id + sfpath);
		
		if (Files.exists(fass))
			try {
				res.setResult(new AssetInfo(ctx, fpath, fass, Files.getLastModifiedTime(fass).toMillis()));
			} 
			catch (IOException x) {
			}
		*/
		
		
		// if not in the domain, then go look in the packages 

		if (res.getResult() == null) {
			for (XElement pel :  this.extension.getLoader().getConfig().selectAll("Package")) {
				Path wpath = Hub.instance.getResources().getPackageWebFile(pel.getAttribute("Id"), path);
				
				if (wpath != null) {
					try {
						String wpathname = wpath.getFileName().toString();
						
						if (wpathname.endsWith(".pui.xml"))
							res.setResult(new PuiAssetInfo(ctx, path, wpath, Files.getLastModifiedTime(wpath).toMillis()));
						else if (wpathname.endsWith(".gas")) {
							GroovyClassLoader loader = new GroovyClassLoader();
							Class<?> groovyClass = loader.parseClass(wpath.toFile());
							Method runmeth = null;
							
							for (Method m : groovyClass.getMethods()) {
								if (!m.getName().startsWith("run"))
									continue;
								
								runmeth = m;
								break;
							}
							
							if (runmeth == null) {
								res.error("Unable to execute script!");
							}
							else {
								ByteBufWriter mem = ByteBufWriter.createLargeHeap();
								AssetInfo info = new AssetInfo(path, mem, System.currentTimeMillis());
								info.setMime("text/html");
								
								GroovyObject groovyObject = (GroovyObject) groovyClass.newInstance();
								Object[] args2 = { ctx, mem, info };
								
								groovyObject.invokeMethod("run", args2);
								
								res.setResult(info);
							}
							
							loader.close();
						}
						else {
							res.setResult(new AssetInfo(ctx, path, wpath, Files.getLastModifiedTime(wpath).toMillis()));
						}
						
						// stop on first match
						break;
					} 
					catch (Exception x) {
						Logger.error("Error loading resource: " + path + " - error: " + x);
					}
				}
			}
		}		

		/*
		// if not in the packges, then go look in the JAR 
		// TODO review loading from JARs

		if (res.getResult() == null) {
			if (this.extension.getBundle().hasFileEntry(sfpath)) {
				byte[] mem = this.extension.getBundle().findFileEntry(sfpath);
				res.setResult(new AssetInfo(fpath, mem, WebSiteManager.instance.getModule().startTime()));
			}
		}
		*/
		
		if (res.getResult() == null) 		
			res.errorTr(150007);
		
		return res;
	}
	
	public OperationResult writeFile(WebContext ctx, AssetInfo asset) {
		OperationResult res = new OperationResult();
		
		String fpath = asset.getPath().toString();
		
		if ((fpath == null) || (asset.getSize() == -1)) {
			res.errorTr(150001);
			return res;
		}

		// certain resource types cannot be delivered
		if ((fpath.endsWith(".class") || fpath.endsWith(".view.xml") || fpath.endsWith(".part.xml"))){
			res.errorTr(150001);
			return res;
		}

		Response resp = ctx.getResponse(); 
		
		resp.setHeader("Content-Type", asset.getMime());
		resp.setDateHeader("Date", System.currentTimeMillis());
		resp.setDateHeader("Last-Modified", asset.getWhen());
		resp.setHeader("X-UA-Compatible", "IE=Edge,chrome=1");
		
		if (ctx.getRequest().hasHeader("If-Modified-Since")) {
			long dd = asset.getWhen() - ctx.getRequest().getDateHeader("If-Modified-Since");  

			// getDate does not return consistent results because milliseconds
			// are not cleared correctly see:
			// https://sourceforge.net/tracker/index.php?func=detail&aid=3162870&group_id=62369&atid=500353
			// so ignore differences of less than 1000, they are false positives
			if (dd < 1000) {
				resp.setStatus(HttpResponseStatus.NOT_MODIFIED);
				ctx.send();
				return res;
			}
		}
		
		if (asset.getCompressed())
			resp.setHeader("Content-Encoding", "gzip");
		
		String attach = asset.getAttachmentName();
		
		if (StringUtil.isNotEmpty(attach))
			resp.setHeader("Content-Disposition", "attachment; filename=\"" + NetUtil.urlEncodeUTF8(attach) + "\"");
		
		// TODO send HttpResponse with content length...then push the file directly from here instead of setting body first and using memory 
		//ctx.getResponse().setBody(content);
		
		//System.out.println("Sending: " + fpath + " as: " + asset.getSize());
		ctx.sendStart(asset.getSize());
		
		// TODO
		if (asset.isRegion()) {
			//System.out.println("Sending: " + fpath + " as chunks ");
			ctx.send(asset.getChunks());
		}
		else {
			//System.out.println("Sending: " + fpath + " as buffer ");
			ctx.send(asset.getBuffer().getByteBuf());
		}
		
		//System.out.println("Sending: " + fpath + " as end");
		ctx.sendEnd();
		
		return res;
		
		/*
		byte[] bis = this.extension.getBundle().findFileEntry(fpath);
		
		// TODO make sure there is some way to flush responses in Simple					
		if (bis != null) 
			this.response.getOutputStream().write(bis);
		else
			notfound = true;
			*/
		
	}

	// Html, Qx, Xml parsing
	
	@Override
    public Nodes parseXml(String format, ViewInfo view, XElement container) {
    	Nodes nodes = new Nodes();
    	
    	for (XNode xnode : container.getChildren()) {
    		if (xnode instanceof XElement) {
    			this.parseElement(format, view, nodes, (XElement)xnode);
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
	@Override
    public Nodes parseElement(String format, ViewInfo view, XElement xel) {
    	Nodes nodes = new Nodes();
    	this.parseElement(format, view, nodes, xel);
    	return nodes;
    }
    
    // parses the children of container
	@Override
    public void parseElement(String format, ViewInfo view, Nodes nodes, XElement xel) {
		if (xel == null)
			return;
		
		Class<? extends ICodeTag> tag = this.getCodeTag(format, xel.getName());
		
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
		Map<String, Class<? extends ICodeTag>> html = new HashMap<String, Class<? extends ICodeTag>>();
		
		this.codetags.put("HtmlOutput", html);
		
		html.put("a", A.class);
		html.put("article", Article.class);
		
		html.put("b", B.class);
		html.put("blockquote", BlockQuote.class);
		html.put("body", Body.class);
		html.put("button", Button.class);
		html.put("br", Br.class);
		html.put("brlf", BrLf.class);		
		
		html.put("div", Div.class);
		
		html.put("em", Em.class);
		
		html.put("fieldset", FieldSet.class);
		html.put("footer", Footer.class);
		html.put("form", Form.class);
		
		html.put("h1", H1.class);
		html.put("h2", H2.class);
		html.put("h3", H3.class);
		html.put("h4", H4.class);
		html.put("h5", H5.class);
		html.put("h6", H6.class);		
		html.put("head", Head.class);
		html.put("header", Header.class);
		html.put("html", Html.class);
		html.put("hr", Hr.class);
		
		html.put("i", I.class);
		html.put("iframe", IFrame.class);
		html.put("img", Img.class);
		html.put("input", Input.class);
		
		html.put("label", Label.class);
		html.put("legend", Legend.class);
		html.put("li", Li.class);		
		html.put("link", Link.class);
		
		html.put("meta", Meta.class);
		
		html.put("nbsp", NbSp.class);
		html.put("nav", Nav.class);
		
		html.put("ol", Ol.class);		
		html.put("optgroup", OptGroup.class);
		html.put("option", Option.class);
		
		html.put("p", P.class);
		html.put("pre", Pre.class);
		
		html.put("section", Section.class);		
		html.put("select", Select.class);
		html.put("script", Script.class);
		html.put("style", Style.class);		
		html.put("strong", B.class);
		html.put("span", Span.class);
		
		html.put("table", Table.class);
		html.put("tbody", TBody.class);		
		html.put("td", Td.class);		
		html.put("th", Th.class);
		html.put("tr", Tr.class);
		html.put("textarea", TextArea.class);
		html.put("title", Title.class);

		html.put("u", U.class);
		html.put("ul", Ul.class);
		
		// ==============================================================
		// above this point are std HTML tags, below are our enhanced tags
		// ==============================================================

		html.put("HyperLink", HyperLink.class);
		html.put("AssetImage", AssetImage.class);
		html.put("LiteralText", LiteralText.class);
		html.put("Html5Head", Html5Head.class);		
		html.put("IncludePart", IncludePart.class);
		html.put("IncludeHolder", IncludeHolder.class);
		html.put("IncludeParam", IncludeParam.class);
		html.put("Style", Style.class);
		html.put("Script", Script.class);
		
		html.put("jqmDocument", jqm.Document.class);
		html.put("jqmPage", jqm.Page.class);
		html.put("jqmForm", jqm.form.Form.class);
		html.put("jqmTextField", jqm.form.TextField.class);
		html.put("jqmTextArea", jqm.form.TextArea.class);
		html.put("jqmSelect", jqm.form.Select.class);
		html.put("jqmYesNoField", jqm.form.YesNo.class);
		html.put("jqmRadioSelect", jqm.form.RadioSelect.class);
		html.put("jqmFileField", jqm.form.FileField.class);
		html.put("jqmFormButtons", jqm.form.FormButtons.class);
	}
}
