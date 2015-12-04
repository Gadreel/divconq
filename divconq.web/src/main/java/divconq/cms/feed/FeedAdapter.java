package divconq.cms.feed;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map.Entry;

import divconq.io.CacheFile;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.locale.ITranslationAdapter;
import divconq.locale.LocaleDefinition;
import divconq.log.Logger;
import divconq.util.IOUtil;
import divconq.util.StringUtil;
import divconq.web.WebContext;
import divconq.web.md.Processor;
import divconq.xml.XElement;
import divconq.xml.XNode;
import divconq.xml.XmlReader;

public class FeedAdapter {
	protected String channel = null;
	protected Path path = null;
	protected XElement xml = null;
	
	// best not to use CacheFile always, not if from feed save/feed delete
	public void init(String channel, Path path) {
		if (path == null)
			return;
		
		this.path = path;
		this.channel = channel;
		
		if (Files.notExists(path))
			return;
		
		FuncResult<XElement> res = XmlReader.loadFile(path, false);
		
		if (res.hasErrors())
			OperationContext.get().error("Bad feed file - " + this.channel + " | " + path);
		
		this.xml = res.getResult();
	}
	
	public void init(String channel, CacheFile file) {
		if (file == null)
			return;
		
		this.path = file.getFilePath();
		this.channel = channel;
		
		this.xml = file.asXml();
	}
	
	public void validate() {
		if (this.xml == null)
			return;
		
		String locale = this.getAttribute("Locale");
		
		if (StringUtil.isEmpty(locale))
			OperationContext.get().error("Missing Locale - " + this.channel + " | " + path);
		
		String title = this.getDefaultField("Title");
		
		if (StringUtil.isEmpty(title))
			OperationContext.get().error("Missing Title - " + this.channel + " | " + path);
		
		String desc = this.getDefaultField("Description");
		
		if (StringUtil.isEmpty(desc))
			OperationContext.get().warn("Missing Description - " + this.channel + " | " + path);
		
		String key = this.getDefaultField("Keywords");
		
		if (StringUtil.isEmpty(key))
			OperationContext.get().warn("Missing Keywords - " + this.channel + " | " + path);
		
		//String img = this.getField("Image");
		
		//if (StringUtil.isEmpty(img))
		//	OperationContext.get().warn("Missing Image - " + this.key + " | " + path);
	}

	public String getAttribute(String name) {
		if ((this.xml == null) || StringUtil.isEmpty(name))
			return null;
		
		return this.xml.getAttribute(name);
	}
	
	public String getDefaultField(String name) {
		if ((this.xml == null) || StringUtil.isEmpty(name))
			return null;
		
		// provide the value for the `default` locale of the feed 
		String deflocale = this.xml.getAttribute("Locale");
		
		for (XElement fel : this.xml.selectAll("Field")) {
			if (name.equals(fel.getAttribute("Name"))) {
				if (!fel.hasAttribute("Locale"))
					return fel.getValue();
					
				if ((deflocale != null) && deflocale.equals(fel.getAttribute("Locale")))
					return fel.getValue();
			}
		}
		
		return null;
	}
	
	public MatchResult bestMatch(String tag, String attr,  String name) {
		return this.bestMatch(OperationContext.get(), tag, attr, name);
	}
	
	public MatchResult bestMatch(ITranslationAdapter ctx, String tag, String attr, String match) {
		if ((this.xml == null) || StringUtil.isEmpty(tag) || StringUtil.isEmpty(attr) || StringUtil.isEmpty(match))
			return null;

		int highest = Integer.MAX_VALUE;
		MatchResult best = new MatchResult();
		
		for (XElement afel : this.xml.selectAll("Alternate")) {
			String alocale = afel.getAttribute("Locale");
			
			int arate = ctx.rateLocale(alocale);
			
			if ((arate >= highest) || (arate == -1))
				continue;
			
			for (XElement fel : afel.selectAll(tag)) {
				if (match.equals(fel.getAttribute(attr))) {
					best.el = fel;
					best.localename = alocale;
					highest = arate;
					break;
				}
			}
		}
		
		for (XElement fel : this.xml.selectAll(tag)) {
			if (fel.hasAttribute("Locale") && match.equals(fel.getAttribute(attr))) {
				String flocale = fel.getAttribute("Locale");
				
				int arate = ctx.rateLocale(flocale);
				
				if ((arate >= highest) || (arate == -1))
					continue;
				
				best.el = fel;
				best.localename = flocale;
				highest = arate;
			}
		}

		String deflocale = this.xml.getAttribute("Locale");
		
		if (StringUtil.isNotEmpty(deflocale)) {
			int arate = ctx.rateLocale(deflocale);
			
			if ((arate != -1) && (arate < highest)) {
				for (XElement fel : this.xml.selectAll(tag)) {
					if (!fel.hasAttribute("Locale") && match.equals(fel.getAttribute(attr))) {
						best.el = fel;
						best.localename = deflocale;
						highest = arate;
						break;
					}
				}
			}
		}
		
		if (highest == Integer.MAX_VALUE)
			return null;
		
		best.locale = ctx.getLocaleDefinition(best.localename);
		
		return best;
	}
	
	public class MatchResult {
		public XElement el = null;
		public LocaleDefinition locale = null;
		public String localename = null;
	}
	
	public String getFirstField(String... names) {
		return this.getFirstField(OperationContext.get(), names);
	}
	
	public String getFirstField(ITranslationAdapter ctx, String... names) {
		for (String n : names) {
			String v = this.getField(n);
			
			if (v != null)
				return v;
		}
		
		return null;
	}
	
	public String getField(String name) {
		return this.getField(OperationContext.get(), name);
	}
	
	public String getField(ITranslationAdapter ctx, String name) {
		if ((this.xml == null) || StringUtil.isEmpty(name))
			return null;
		
		MatchResult mr = this.bestMatch(ctx, "Field", "Name", name);
		
		if (mr != null)
			return mr.el.getValue();
		
		return null;
	}
	
	public String getPart(WebContext wctx, String name) {
		return this.getPart(wctx, OperationContext.get(), name);
	}
	
	public String getPart(WebContext wctx, ITranslationAdapter ctx, String name) {
		if ((this.xml == null) || StringUtil.isEmpty(name))
			return null;
		
		MatchResult mr = this.bestMatch(ctx, "PagePart", "For", name);
		
		if (mr == null)
			return null;

		return this.getPartValue(wctx, ctx, mr);
	}
	
	public void buildHtmlPage(WebContext wctx, XElement frag) {
		this.buildHtmlPage(wctx, OperationContext.get(), frag);
	}
	
	public void buildHtmlPage(WebContext wctx, ITranslationAdapter ctx, XElement frag) {
		OperationResult or = new OperationResult();
		
		String title = this.getField(ctx, "Title");
				
		if (title != null) 
			frag.setAttribute("Title", title);

		String desc = this.getField(ctx, "Description");

		if (desc != null) 
			frag.add(new XElement("Description").withText(desc));

		String keywords = this.getField(ctx, "Keywords");
		
		if (keywords != null) 
			frag.add(new XElement("Keywords").withText(keywords));
		
		for (XElement pdef : frag.selectAll("PagePartDef")) {
			String bid = pdef.getAttribute("BuildId", pdef.getAttribute("For"));
			
			if (StringUtil.isEmpty(bid)) {
				or.error("Unable to build page element: " + pdef);
				continue;
			}
			
			XElement bbparent = frag.findParentOfId(bid);
			XElement bparent = bbparent.findId(bid); 
			
			if (bparent == null) {
				or.error("Missing parent to build page element: " + pdef);
				continue;
			}
			
			XElement content = this.buildHtml(wctx, ctx, pdef.getAttribute("For"), pdef.getAttribute("BuildClass"), frag);
			
			if (content == null)
				continue;
			
			String bop = pdef.getAttribute("BuildOp", "Append");
			
			if ("Append".equals(bop)) {
				bparent.add(-1, content);
			}
			else if ("Prepend".equals(bop)) {
				bparent.add(0, content);
			}
			else if ("Before".equals(bop)) {
				int ccnt = bbparent.getChildCount();
				int cpos = 0;
				
				for (int i = 0; i < ccnt; i++) 
					if (bbparent.getChild(i) == bparent) {
						cpos = i;
						break;
					}
						
				bbparent.add(cpos, content);
			}
			else if ("After".equals(bop)) {
				int ccnt = bbparent.getChildCount();
				int cpos = 0;
				
				for (int i = 0; i < ccnt; i++) 
					if (bbparent.getChild(i) == bparent) {
						cpos = i;
						break;
					}
						
				bbparent.add(cpos + 1, content);
			}
		}
	}
	
	public XElement buildHtml(WebContext wctx, String id, String clss, XElement altsrc) {
		return this.buildHtml(wctx, OperationContext.get(), id, clss, altsrc);
	}
	
	public XElement buildHtml(WebContext wctx, ITranslationAdapter ctx, String id, String clss, XElement altsrc) {
		if ((this.xml == null) || StringUtil.isEmpty(id))
			return null;
		
		MatchResult mr = this.bestMatch(ctx, "PagePart", "For", id);
		
		if (mr == null) {
			if (altsrc != null) {
				for (XElement fel : altsrc.selectAll("PagePart")) {
					if (id.equals(fel.getAttribute("For"))) {
						mr = new MatchResult();
						mr.el = fel;
						mr.localename = this.xml.getAttribute("Locale", "en");
						mr.locale = ctx.getLocaleDefinition(mr.localename);
					}
				}
			}			
		}
		
		if (mr == null)
			return null;

		return this.buildHtmlPart(wctx, ctx, mr, id, clss);
	}
	
	public XElement buildHtmlPart(WebContext wctx, ITranslationAdapter ctx, MatchResult mr, String id, String clss) {
		String lang = mr.locale.getLanguage();
		
		String fmt = mr.el.getAttribute("Format", "md");
		
		XElement pel = new XElement("div");
		pel.setAttribute("lang", lang);
		
		if (id != null)
			pel.setAttribute("id", id);
		
		if (clss != null)
			pel.setAttribute("class", clss);
		
		// copy all attributes 
		for (Entry<String, String> attr : mr.el.getAttributes().entrySet()) 
			pel.setAttribute(attr.getKey(), attr.getValue());
		
		
		if ("image".equals(fmt)) {
			pel.setName("img");
			pel.setAttribute("src", "/galleries" + this.getPartValue(wctx, ctx, mr));
		}
		else if ("html".equals(fmt)) {
			pel.setAttribute("data-dcui-mode", "enhance");
			
			XElement html = this.getPartXml(wctx, ctx, mr);
			
			// copy all children
			for (XNode n : html.getChildren())
				pel.add(n);
		}
		else {
			pel.setAttribute("data-dcui-mode", "enhance");

	        try {
				// TODO support safe mode?
				XElement html = Processor.parse(wctx.getMarkdownContext(), this.getPartValue(wctx, ctx, mr));
				
				// copy all children
				for (XNode n : html.getChildren())
					pel.add(n);
	        }
	        catch (Exception x) {
	        	Logger.error("Error adding copy box" + x);
	        }
		}
		
		return pel;
	}
	
	public String getPartValue(WebContext wctx, ITranslationAdapter ctx, MatchResult mr) {
		if ((ctx == null) || (mr == null))
			return null;
		
		String ex = mr.el.getAttribute("External", "False");
		
		String locale = mr.locale.getName();
		
		if (StringUtil.isNotEmpty(ex) && "true".equals(ex.toLowerCase())) {
			int pos = this.channel.indexOf('.');
			String spath = (pos != -1) ? this.channel.substring(0, pos) : this.channel;
			
			spath = spath + "." + mr.el.getAttribute("For") + "." + locale + "." + mr.el.getAttribute("Format");
			
			// TODO connect to file caching system - but make sure the import from FeedIndexer will still work correctly
			//Path fpath = OperationContext.get().getDomain().findSectionFile(this.isPreview(), "feed", spath);
			
			Path fpath = null;
			
			if (wctx.isPreview()) {
				fpath = OperationContext.get().getDomain().resolvePath("/feed-preview" + spath);
				
				if (Files.notExists(fpath))
					fpath = OperationContext.get().getDomain().resolvePath("/feed" + spath);
			}
			else {
				fpath = OperationContext.get().getDomain().resolvePath("/feed" + spath);
			}

			if (Files.exists(fpath)) {
				FuncResult<CharSequence> mres = IOUtil.readEntireFile(fpath);
				
				if (mres.isNotEmptyResult()) 
					return mres.getResult().toString();
			}
		}
		
		return mr.el.getValue();
	}
	
	// element returned is ignored - children and attributes are copied into a div
	public XElement getPartXml(WebContext wctx, ITranslationAdapter ctx, MatchResult mr) {
		if ((ctx == null) || (mr == null))
			return null;
		
		String ex = mr.el.getAttribute("External", "False");
		
		String locale = mr.locale.getName();
		
		if (StringUtil.isNotEmpty(ex) && "true".equals(ex.toLowerCase())) {
			int pos = this.channel.indexOf('.');
			String spath = (pos != -1) ? this.channel.substring(0, pos) : this.channel;
			
			spath = spath + "." + mr.el.getAttribute("For") + "." + locale + "." + mr.el.getAttribute("Format");

			// TODO connect to file caching system - but make sure the import from FeedIndexer will still work correctly
			//Path fpath = OperationContext.get().getDomain().findSectionFile(this.isPreview(), "feed", spath);
			
			Path fpath = null;
			
			if (wctx.isPreview()) {
				fpath = OperationContext.get().getDomain().resolvePath("/feed-preview" + spath);
				
				if (Files.notExists(fpath))
					fpath = OperationContext.get().getDomain().resolvePath("/feed" + spath);
			}
			else {
				fpath = OperationContext.get().getDomain().resolvePath("/feed" + spath);
			}

			if (Files.exists(fpath)) {
				FuncResult<CharSequence> mres = IOUtil.readEntireFile(fpath);
				
				if (mres.isNotEmptyResult()) {
					FuncResult<XElement> xres = XmlReader.parse("<div>" + mres.getResult() + "</div>", false);
					
					if (xres.isNotEmptyResult()) 
						return xres.getResult();
				}
			}
		}
		
		return mr.el;
	}

	public XElement getXml() {
		return this.xml;
	}
	
	public String getPartValue(String locale, XElement part, boolean isPreview) {
		if (part == null)
			return null;
		
		String ex = part.getAttribute("External", "False");
		
		if (part.hasAttribute("Locale"))
			locale = part.getAttribute("Locale");	// use the override locale if present
		
		if (StringUtil.isNotEmpty(ex) && "true".equals(ex.toLowerCase())) {
			int pos = this.channel.indexOf('.');
			String spath = (pos != -1) ? this.channel.substring(0, pos) : this.channel;
			
			spath = spath + "." + part.getAttribute("For") + "." + locale + "." + part.getAttribute("Format");

			// TODO connect to file caching system - but make sure the import from FeedIndexer will still work correctly
			//Path fpath = OperationContext.get().getDomain().findSectionFile(this.isPreview(), "feed", spath);
			
			Path fpath = null;
			
			if (isPreview) {
				fpath = OperationContext.get().getDomain().resolvePath("/feed-preview" + spath);
				
				if (Files.notExists(fpath))
					fpath = OperationContext.get().getDomain().resolvePath("/feed" + spath);
			}
			else {
				fpath = OperationContext.get().getDomain().resolvePath("/feed" + spath);
			}

			if (Files.exists(fpath)) {
				FuncResult<CharSequence> mres = IOUtil.readEntireFile(fpath);
				
				if (mres.isNotEmptyResult()) 
					return mres.getResult().toString();
			}
		}
		
		return part.getValue();
	}
}
