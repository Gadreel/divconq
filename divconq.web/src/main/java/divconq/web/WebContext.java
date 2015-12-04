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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import divconq.cms.feed.FeedAdapter;
import divconq.hub.DomainInfo;
import divconq.io.CacheFile;
import divconq.lang.op.OperationContext;
import divconq.locale.Tr;
import divconq.struct.CompositeStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;
import divconq.web.dcui.GalleryImageConsumer;
import divconq.web.dcui.Node;
import divconq.web.md.Configuration;
import divconq.web.md.ProcessContext;
import divconq.web.md.plugin.PairedMediaSection;
import divconq.web.md.plugin.StandardSection;
import divconq.xml.XElement;

public class WebContext {
	protected IInnerContext innerctx = null;
	protected boolean preview = false;

	protected Map<String, String> innerparams = new HashMap<String, String>();
	
	protected GroovyObject serverScript = null;
	
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
		  
		  // params on this tree
		  if ("param".equals(parts[0]) && (parts.length > 1)) {
			  val = this.getExternalParam(parts[1]);
		  }
		  else if ("ctx".equals(parts[0]) && (parts.length > 1)) {
			  String vname = parts[1];
			  
			  val = this.getInternalParam(vname);
			  
			  // TODO review and remove paths, excessive
			  if ((val == null) && (vname.equals("SignInPath") || vname.equals("HomePath") || vname.equals("PortalPath")
					  || vname.equals("SiteTitle") || vname.equals("SiteAuthor") || vname.equals("SiteCopyright")))
			  {
				DomainInfo domain = this.getDomain().getDomainInfo();
				
				XElement domconfig = domain.getSettings();
				
				if (domconfig != null) {
					XElement web = domconfig.selectFirst("Web");
					
					if ((web != null) && (web.hasAttribute(vname))) 
						val = web.getRawAttribute(vname);
				}
			  }
			  
			  // if not a web setting, perhaps a user field?
			  if ((val == null) && (vname.equals("dcUserFullname"))) {
					val = OperationContext.get().getUserContext().getFullName();
			  }
		  }
		  // definitions in the dictionary
		  else if ("tr".equals(parts[0])) {
			if ((parts.length > 1) && (StringUtil.isDataInteger(parts[1]))) 
				parts[1] = "_code_" + parts[1];
			  
			if (parts.length > 2) {
				String[] params = Arrays.copyOfRange(parts, 2, parts.length - 2);
				val = Tr.tr(parts[1], (Object) params);		// TODO test this
			}
			else if (parts.length > 1) {
				val = Tr.tr(parts[1]);		
			}
		  }
		  else if (this.innerctx != null) {
			  IWebMacro macroproc = this.innerctx.getMacro(parts[0]);
			  
			  if (macroproc != null) 
				  val = macroproc.process(this, parts);
		  }
		  
		  if (val == null)
			  return "";
		  
		  return val;
	  }

	public WebDomain getDomain() {
		return this.innerctx.getDomain();
	}
	
	public WebContext(IInnerContext httpctx) {
		this.innerctx = httpctx;

		Cookie ck = this.innerctx.getRequest().getCookie("dcPreview");

		if (ck != null)
			this.preview = "true".equals(ck.value().toLowerCase());
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
	
	public String getHost() {
		return WebSiteManager.resolveHost(this.innerctx.getRequest());
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
					adapt.init(path, fpath);		
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

	// TODO enhance how plugins are loaded
	public ProcessContext getMarkdownContext() {
		Configuration cfg = new Configuration()
			.setSafeMode(false)
			.registerPlugins(new PairedMediaSection(), new StandardSection());
		
		return new ProcessContext(cfg, this);
	}
	
	public ProcessContext getSafeMarkdownContext() {
		Configuration cfg = new Configuration();
		
		return new ProcessContext(cfg, this);
	}
}
