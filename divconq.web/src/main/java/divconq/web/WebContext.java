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

import io.netty.handler.codec.http.Cookie;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import divconq.hub.DomainInfo;
import divconq.lang.OperationContext;
import divconq.lang.UserContext;
import divconq.locale.LocaleInfo;
import divconq.locale.LocaleUtil;
import divconq.session.Session;
import divconq.util.StringUtil;
import divconq.view.ContentPlaceholder;
import divconq.view.Node;

public class WebContext {
	protected HttpContext innerctx = null;
	protected IWebExtension extension = null; // originating extension
	protected IWebDomain domain = null;  // originating domain
	
	protected LocaleInfo selectedlocale = null;
	protected String theme = null;
	protected boolean completed = false;
	protected Map<String, ContentPlaceholder> holders = new HashMap<String, ContentPlaceholder>();
	protected Map<String, String> innerparams = new HashMap<String, String>();

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

	public String getExternalParam(String name) {
		return this.getExternalParam(name, null);
	}

	public String getExternalParam(String name, String defaultvalue) {
		try {
			String ret = this.innerctx.getRequest().getParameter(name);

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
		if ("dcmVersion".equals(name))
			return true;
		
		UserContext uc = this.innerctx.getSession().getUser();
		
		if (uc != null) {
			if ("dcUserId".equals(name))
				return true;
			
			if ("dcUsername".equals(name))
				return true;
			
			if ("dcUserFullname".equals(name))
				return true;
			
			if ("dcUserEmail".equals(name))
				return true;
		}
		
		return this.innerparams.containsKey(name);
	}
	
	public String getInternalParam(String name) {		
		if ("dcmVersion".equals(name))
			return "0.7.1";
		
		UserContext uc = this.innerctx.getSession().getUser();
		
		if (uc != null) {
			if ("dcUserId".equals(name))
				return uc.getUserId();
			
			if ("dcUsername".equals(name))
				return uc.getUserName();
			
			if ("dcUserFullname".equals(name))
				return uc.getFullName();
			
			if ("dcUserEmail".equals(name))
				return uc.getEmail();
		}
		
		return this.innerparams.get(name);
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
				  
				  String val = null;
				  
				  // params from external
				  if (val == null)
					  val = this.getExternalParam(macro);
				  
				  // params from internal
				  if (val == null)
					  val = this.getInternalParam(macro);
				  
				  // definitions in the dictionary
				  if (val == null) 
					  val = OperationContext.get().tr(macro);		// TODO what about web domain dictionaries...
				  
				  if (val == null) {
					  String[] mparts = macro.split("\\|");
					  
					  IWebMacro macroproc = WebSiteManager.instance.getMacro(mparts[0]);
					  
					  if (macroproc != null)
						  val = macroproc.process(this, mparts); 
				  }
				  
				  // if any of these, then replace and check (expand) again 
				  if (val != null) {
					  value = value.replace(grp, val);
					  checkmatches = true;
				  }
			  }
		  }
		  
		  return value;
	  }

	public LocaleInfo getLocale() {
		return this.selectedlocale;
	}

	public IWebExtension getExtension() {
		return this.extension;
	}
	
	public IWebDomain getDomain() {
		return this.domain;
	}
	
	public WebContext(HttpContext httpctx, IWebExtension ext) {
		this.innerctx = httpctx;
		this.extension = ext;		
		
		OperationContext tc = OperationContext.get();
		
		// TODO look at supported languages also, std http headers

		String locale = tc.getUserContext().getLocale();
		
		Cookie ck = this.innerctx.getRequest().getCookie("dcmLocale");
		
		if (ck != null)
			locale = ck.getValue(); 
		
		// TODO if different
		//tc.getUserContext().setLocale(locale);	

		this.selectedlocale = LocaleUtil.getLocaleInfo(locale);
		
		if (this.selectedlocale == null)
			this.selectedlocale = LocaleUtil.getDefaultLocaleInfo();

		ck = this.innerctx.getRequest().getCookie("dcmTheme");

		if (ck != null)
			this.theme = ck.getValue();
		
		// TODO get default theme from WebExtension 
		
		// TODO add device override support too
		
		DomainInfo domaininfo = WebSiteManager.instance.resolveDomainInfo(this.innerctx.getRequest());
		
		if (domaininfo != null)
			this.domain = ext.getDomain(domaininfo.getId());
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
		if (this.selectedlocale != null)
			return this.selectedlocale.isRightToLeft();

		return false;
	}

	public String getLanguage() {
		if (this.selectedlocale != null)
			return this.selectedlocale.getLanguage();

		return null;
	}

	public Response getResponse() {
		return this.innerctx.getResponse();
	}

	public Request getRequest() {
		return this.innerctx.getRequest();
	}

	public Session getSession() {
		return this.innerctx.getSession();
	}
    
    public Map<String, List<String>> getParameters() {
    	return this.innerctx.getRequest().getParameters();
    }
	
	public boolean isCompleted() {
		return this.completed;
	}

	public String getTheme() {
		return this.theme;
	}

	public void setTheme(String v) {
		this.theme = v;
	}
	
	public String getHost() {
		return WebSiteManager.instance.resolveHost(this.innerctx.getRequest());
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
	
	public void closeChannel() {
		this.innerctx.close();
	}
}
