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

import groovy.lang.GroovyObject;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.stream.ChunkedInput;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import divconq.cms.feed.FeedAdapter;
import divconq.hub.DomainInfo;
import divconq.hub.Hub;
import divconq.io.CacheFile;
import divconq.lang.op.OperationContext;
import divconq.lang.op.UserContext;
import divconq.locale.LocaleInfo;
import divconq.locale.LocaleUtil;
import divconq.struct.CompositeStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;
import divconq.web.dcui.GalleryImageConsumer;
import divconq.web.dcui.Node;
import divconq.xml.XElement;

public class WebContext {
	protected IInnerContext innerctx = null;
	protected LocaleInfo selectedlocale = null;
	protected String theme = null;
	protected boolean preview = false;
	// TODO restore if useful protected boolean completed = false;
	//protected Map<String, ContentPlaceholder> holders = new HashMap<String, ContentPlaceholder>();
	protected Map<String, String> innerparams = new HashMap<String, String>();
	
	protected GroovyObject serverScript = null;

	/*
	public ContentPlaceholder getHolder(String name) {
		synchronized (this.holders) {
			if (!this.holders.containsKey(name)) {
				ContentPlaceholder ph = new ContentPlaceholder();
				this.holders.put(name, ph);
				return ph;
			}
		}

		return this.holders.get(name);
	}
	*/
	
	public GroovyObject getServerScript() {
		return this.serverScript;
	}
	
	public void setServerScript(GroovyObject v) {
		this.serverScript = v;
	}
	
	public String getExternalParam(String name) {
		return this.getExternalParam(name, null);
	}

	public String getExternalParam(String name, String defaultvalue) {
		try {
			String ret = this.getRequest().getParameter(name);

			if (ret == null)
				ret = defaultvalue;

			return ret;
		} 
		catch (Exception x) {
		}

		return null;
	}
	
	public void putInternalParam(String name, String value) {
		this.innerparams.put(name, value);
	}
	
	public boolean hasInternalParam(String name) {
		return this.innerparams.containsKey(name);
	}
	
	public String getInternalParam(String name) {		
		return this.innerparams.get(name);
	}
	
	public IInnerContext getInnerContext() {
		return this.innerctx;
	}
	  
	  public String expandMacros(String value) {
		  if (StringUtil.isEmpty(value))
			  return null;
		  
		  boolean checkmatches = true;
		  
		  while (checkmatches) {
			  checkmatches = false;
			  Matcher m = Node.macropatten.matcher(value);
			  
			  while (m.find()) {
				  String grp = m.group();
				  
				  String macro = grp.substring(1, grp.length() - 1);
				  
				  String val = this.expandMacro(macro);
				  
				  // if any of these, then replace and check (expand) again 
				  if (val != null) {
					  value = value.replace(grp, val);
					  checkmatches = true;
				  }
			  }
		  }
		  
		  return value;
	  }
	  
	  public String expandMacro(String macro) {
		  String[] parts = macro.split("\\|");
		  
		  String val = null;
		  
		  // TODO check size of parts @val
		  
		  // params on this tree
		  if ("param".equals(parts[0])) {
			  val = this.getExternalParam(parts[1]);
			  
			  if (val == null)
				  val = "";
		  }
		  else if ("ctx".equals(parts[0])) {
			  val = this.getInternalParam(parts[1]);
			  
			  if (val == null)
				  val = "";
		  }
		  // definitions in the dictionary
		  else if ("tr".equals(parts[0])) {
			  val = this.getDomain().tr(this.getLocale(), parts[1]);		// TODO support tr params
			  
			  if (val == null)
				  val = "";
		  }
		  else if (this.innerctx != null) {
			  IWebMacro macroproc = this.innerctx.getMacro(parts[0]);
			  
			  if (macroproc != null) {
				  val = macroproc.process(this, parts);
				  
				  if (val == null)
					  val = "";
			  }
		  }
		  
		  return val;
	  }

	public LocaleInfo getLocale() {
		if (this.selectedlocale == null) {
			// TODO look at supported languages also, std http headers

			OperationContext tc = OperationContext.get();
			
			String locale = tc.getUserContext().getLocale();
			
			Cookie ck = this.getRequest().getCookie("dcmLocale");
			
			if (ck != null)
				locale = ck.value(); 
			
			// TODO if different
			//tc.getUserContext().setLocale(locale);	
	
			this.selectedlocale = LocaleUtil.getStrictLocaleInfo(locale);
			
			if (this.selectedlocale == null)
				this.selectedlocale = this.innerctx.getDomain().getDefaultLocaleInfo();
		}
		
		return this.selectedlocale;
	}
	
	public WebDomain getDomain() {
		return this.innerctx.getDomain();
	}
	
	public WebContext(IInnerContext httpctx) {
		this.innerctx = httpctx;
		
		Cookie ck = this.innerctx.getRequest().getCookie("dcmTheme");

		if (ck != null)
			this.theme = ck.value();

		ck = this.innerctx.getRequest().getCookie("dcmPreview");

		if (ck != null)
			this.preview = "true".equals(ck.value().toLowerCase());
		
		// TODO get default theme from WebExtension 
		
		// TODO add device override support too
		
		this.putInternalParam("dcmVersion", "0.9.6");
		
		UserContext uc = OperationContext.get().getUserContext();
		
		this.putInternalParam("dcUserId", uc.getUserId());
		this.putInternalParam("dcUsername", uc.getUsername());
		this.putInternalParam("dcUserFullname", uc.getFullName());
		this.putInternalParam("dcUserEmail", uc.getEmail());
		
		DomainInfo domain = Hub.instance.getDomainInfo(httpctx.getDomain().getId());
		
		XElement domconfig = domain.getSettings();
		
		if (domconfig!= null) {
			XElement web = domconfig.selectFirst("Web");
			
			if (web != null) {
				if (web.hasAttribute("SignInPath")) 
					this.putInternalParam("SignInPath", web.getAttribute("SignInPath"));
				
				if (web.hasAttribute("HomePath")) 
					this.putInternalParam("HomePath", web.getAttribute("HomePath"));
				
				if (web.hasAttribute("PortalPath")) 
					this.putInternalParam("PortalPath", web.getAttribute("PortalPath"));
				
				if (web.hasAttribute("SiteTitle")) 
					this.putInternalParam("SiteTitle", web.getRawAttribute("SiteTitle"));
				
				if (web.hasAttribute("SiteAuthor")) 
					this.putInternalParam("SiteAuthor", web.getRawAttribute("SiteAuthor"));
				
				if (web.hasAttribute("SiteCopyright")) 
					this.putInternalParam("SiteCopyright", web.getRawAttribute("SiteCopyright"));
			}
		}
	}

	/*
	public PrintStream getPrintStream() throws IOException {
		if (this.prntstream == null)
			this.prntstream = this.response.getPrintStream();

		return this.prntstream;
	}

	public OutputStream getRawStream() throws IOException {
		if (this.rawstream == null)
			this.rawstream = this.response.getOutputStream();

		return this.rawstream;
	}
	*/

	public String format(String str, Object... args) {
		LocaleInfo locale = this.getLocale();

		if (locale != null)
			return String.format(locale.getLocale(), str, args);

		return String.format(str, args);
	}

	public boolean isRightToLeft() {
		if (this.getLocale() != null)
			return this.getLocale().isRightToLeft();

		return false;
	}

	public String getLanguage() {
		if (this.getLocale() != null)
			return this.getLocale().getLanguage();

		return null;
	}

	public Response getResponse() {
		return this.innerctx.getResponse();
	}

	public Request getRequest() {
		return this.innerctx.getRequest();
	}
    
    public Map<String, List<String>> getParameters() {
    	return this.getRequest().getParameters();
    }
	
    /* TODO restore this if we can find a use
	public boolean isCompleted() {
		return this.completed;
	}
	*/

	public String getTheme() {
		return this.theme;
	}

	public void setTheme(String v) {
		this.theme = v;
	}
	
	public String getHost() {
		return WebSiteManager.resolveHost(this.innerctx.getRequest());
	}
	
	public Collection<String> getThemeSearch() {
		List<String> path = new ArrayList<String>();
		
		if (StringUtil.isNotEmpty(this.theme))
			path.add(this.theme);
	
		path.add("default");
		
		return path;
	}

	public void send() {
		this.innerctx.send();
	}

	public void sendStart(int contentLength) {
		this.innerctx.sendStart(contentLength);
	}

	public void send(ByteBuf content) {
		this.innerctx.send(content);
	}

	public void send(ChunkedInput<HttpContent> content) {
		this.innerctx.send(content);
	}

	public void sendEnd() {
		this.innerctx.sendEnd();
	}
	
	public void closeChannel() {
		this.innerctx.close();
	}

	public boolean isPreview() {
		return this.preview;
	}
	
	/*
	public Path findPath(String path) {
		return this.innerctx.getDomain().findFilePath(this.isPreview(), new CommonPath(path), null);
	}
	
	public Path findSectionPath(String section, String path) {
		return this.innerctx.getDomain().findSectionFile(this.isPreview(), section, path);
	}
	*/
	
	// string path is relative to dcw/[alias]/[path]
	public XElement getXmlResource(String section, String path) {
		CacheFile fpath = this.getSite().findSectionFile(section, path, this.isPreview());
		
		if (fpath == null)
			return null;
		
		return fpath.asXml();
	}
	
	// string path is relative to dcw/[alias]/[path]
	public CompositeStruct getJsonResource(String section, String path) {
		CacheFile fpath = this.getSite().findSectionFile(section, path, this.isPreview());
		
		if (fpath == null)
			return null;
		
		return fpath.asJson();
	}
	
	// string path is relative to dcw/[alias]/[path]
	public String getTextResource(String section, String path) {
		CacheFile fpath = this.getSite().findSectionFile(section, path, this.isPreview());
		
		if (fpath == null)
			return null;
		
		return fpath.asString();
	}
	
	public FeedAdapter getFeedAdapter(String alias, String path) {
		XElement feed = OperationContext.get().getDomain().getSettings().find("Feed");
		
		if (feed == null) 
			return null;
		
		// there are two special channels - Pages and Blocks
		for (XElement chan : feed.selectAll("Channel")) {
			String calias = chan.getAttribute("Alias");
			
			if (calias == null)
				calias = chan.getAttribute("Name");
			
			if (calias == null)
				continue;
			
			if (calias.equals(alias)) {
				path = chan.getAttribute("Path", chan.getAttribute("InnerPath", "")) + path + ".dcf.xml";
				
				CacheFile fpath = this.getSite().findSectionFile("feed", path, this.isPreview());
				
				if (fpath != null) {
					FeedAdapter adapt = new FeedAdapter();
					adapt.init(path, fpath.getFilePath());		// TODO not ideal, support FileCache object directly as FilePath is not our suggested usage
					return adapt; 
				}
				
				return null;
			}
		}
		
		return null;
	}
	
	public CompositeStruct getGalleryMeta(String path) {
		return this.getJsonResource("galleries", path + "/meta.json");
	}
	
	public void forEachGalleryShowImage(String path, String show, GalleryImageConsumer consumer) {
		RecordStruct gallery = (RecordStruct) this.getGalleryMeta(path);
		
		if ((gallery != null) && (gallery.hasField("Shows"))) {
			for (Struct s : gallery.getFieldAsList("Shows").getItems()) {
				RecordStruct showrec = (RecordStruct) s;
				
				if (!show.equals(showrec.getFieldAsString("Alias")))
					continue;
				
				for (Struct i : showrec.getFieldAsList("Images").getItems()) {
					consumer.accept(gallery, showrec, i);
				}
			}
		}
	}

	public WebSite getSite() {
		return this.innerctx.getSite();
	}
}
