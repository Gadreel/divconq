package divconq.cms.feed;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.UUID;

import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.util.IOUtil;
import divconq.util.StringUtil;
import divconq.web.WebContext;
import divconq.xml.XElement;
import divconq.xml.XmlReader;
import divconq.xml.XmlWriter;

public class FeedAdapter {
	protected String key = null;
	protected Path path = null;
	protected XElement xml = null;
	
	public void init(String key, Path path) {
		if (path == null)
			return;
		
		this.path = path;
		this.key = key;
		
		if (Files.notExists(path))
			return;
		
		FuncResult<XElement> res = XmlReader.loadFile(path, false);
		
		if (res.hasErrors())
			OperationContext.get().error("Bad feed file - " + this.key + " | " + path);
		
		this.xml = res.getResult();
	}
	
	public void validate() {
		if (this.xml == null)
			return;
		
		String uuid = this.xml.getAttribute("Uuid");
		boolean needwrite = false;
		
		if (StringUtil.isEmpty(uuid)) {
			uuid = this.xml.getAttribute("UniqueId");
			needwrite = true;
		}
		
		if (StringUtil.isEmpty(uuid)) {
			uuid = UUID.randomUUID().toString();
			needwrite = true;
		}
	
		if (needwrite) {
			OperationContext.get().info("Adding Uuid: " + this.key + " | " + path);
			
			xml.setAttribute("Uuid", uuid);
			xml.removeAttribute("UniqueId");
			
			XmlWriter.writeToFile(xml, path);
		}
		
		String locale = this.getAttribute("Locale");
		
		if (StringUtil.isEmpty(locale))
			OperationContext.get().error("Missing Locale - " + this.key + " | " + path);
		
		String title = this.getDefaultField("Title");
		
		if (StringUtil.isEmpty(title))
			OperationContext.get().error("Missing Title - " + this.key + " | " + path);
		
		String desc = this.getDefaultField("Description");
		
		if (StringUtil.isEmpty(desc))
			OperationContext.get().warn("Missing Description - " + this.key + " | " + path);
		
		String key = this.getDefaultField("Keywords");
		
		if (StringUtil.isEmpty(key))
			OperationContext.get().warn("Missing Keywords - " + this.key + " | " + path);
		
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
		String deflocale = this.xml.getAttribute("Locale", OperationContext.get().getLocale());
		
		for (XElement fel : this.xml.selectAll("Field")) {
			if ((deflocale.equals(fel.getAttribute("Locale")) || !fel.hasAttribute("Locale")) && name.equals(fel.getAttribute("Name")))
				return fel.getValue();
		}
		
		return null;
	}
	
	public String getField(String name) {
		return this.getField(name, OperationContext.get().getLocale());
	}
	
	public String getFirstField(String... names) {
		String locale = OperationContext.get().getLocale();
		
		for (String n : names) {
			String v = this.getField(n, locale);
			
			if (v != null)
				return v;
		}
		
		return null;
	}
	
	public String getField(String name, String locale) {
		if ((this.xml == null) || StringUtil.isEmpty(name) || StringUtil.isEmpty(locale))
			return null;
		
		XElement ael = this.getAlternate(locale);
		
		if (ael != null) {
			for (XElement fel : ael.selectAll("Field")) {
				if (name.equals(fel.getAttribute("Name")))
					return fel.getValue();
			}
		}
		
		for (XElement fel : this.xml.selectAll("Field")) {
			if (locale.equals(fel.getAttribute("Locale")) && name.equals(fel.getAttribute("Name")))
				return fel.getValue();
		}
		
		// if we cannot match the locale requested we can provide the default
		String deflocale = this.xml.getAttribute("Locale", OperationContext.get().getLocale());
		
		for (XElement fel : this.xml.selectAll("Field")) {
			if ((deflocale.equals(fel.getAttribute("Locale")) || !fel.hasAttribute("Locale")) && name.equals(fel.getAttribute("Name")))
				return fel.getValue();
		}
		
		return null;
	}
	
	public String getPart(String name, boolean isPreview) {
		return this.getPart(name, OperationContext.get().getLocale(), isPreview);
	}
	
	public String getPart(String name, String locale, boolean isPreview) {
		if ((this.xml == null) || StringUtil.isEmpty(name) || StringUtil.isEmpty(locale))
			return null;
		
		XElement ael = this.getAlternate(locale);
		
		if (ael != null) {
			for (XElement fel : ael.selectAll("PagePart")) {
				if (name.equals(fel.getAttribute("For")))
					return this.getPartValue(locale, fel, isPreview);
			}
		}
		
		for (XElement fel : this.xml.selectAll("PagePart")) {
			if (name.equals(fel.getAttribute("For")))
				return this.getPartValue(locale, fel, isPreview);
		}
		
		return null;
	}
	
	public void buildHtmlPage(WebContext ctx, XElement frag, boolean isPreview) {
		this.buildHtmlPage(ctx, frag, OperationContext.get().getLocale(), isPreview);
	}
	
	public void buildHtmlPage(WebContext ctx, XElement frag, String locale, boolean isPreview) {
		OperationResult or = new OperationResult();
		
		String title = this.getField("Title");
				
		if (title != null) 
			frag.setAttribute("Title", title);

		String desc = this.getField("Description");

		if (desc != null) 
			frag.add(new XElement("Description").withText(desc));

		String keywords = this.getField("Keywords");
		
		if (keywords != null) 
			frag.add(new XElement("Keywords").withText(keywords));
		
		for (XElement pdef : frag.selectAll("PagePartDef")) {
			String bid = pdef.getAttribute("BuildId", pdef.getAttribute("For"));
			
			if (StringUtil.isEmpty(bid)) {
				or.error("Unable to build page element: " + pdef);
				continue;
			}
			
			XElement bparent = frag.findId(bid); 
			
			if (bparent == null) {
				or.error("Missing parent to build page element: " + pdef);
				continue;
			}
			
			String bop = pdef.getAttribute("BuildOp", "Append");
			
			if ("Append".equals(bop))
				bparent.add(-1, this.buildHtml(pdef.getAttribute("For"), pdef.getAttribute("BuildClass"), locale, isPreview));
			else if ("Prepend".equals(bop))
				bparent.add(0, this.buildHtml(pdef.getAttribute("For"), pdef.getAttribute("BuildClass"), locale, isPreview));
		}
	}
	
	public XElement buildHtml(String id, String clss, boolean isPreview) {
		return this.buildHtml(id, clss, OperationContext.get().getLocale(), isPreview);
	}
	
	public XElement buildHtml(String id, String clss, String locale, boolean isPreview) {
		if ((this.xml == null) || StringUtil.isEmpty(id) || StringUtil.isEmpty(locale))
			return null;
		
		XElement ael = this.getAlternate(locale);
		
		if (ael != null) {
			for (XElement fel : ael.selectAll("PagePart")) {
				if (id.equals(fel.getAttribute("For"))) 
					return this.buildHtmlPart(fel, id, clss, ael.getAttribute("Locale"), isPreview);
			}
		}
		
		for (XElement fel : this.xml.selectAll("PagePart")) {
			if (id.equals(fel.getAttribute("For"))) 
				return this.buildHtmlPart(fel, id, clss, xml.getAttribute("Locale", OperationContext.get().getLocale()), isPreview);
		}
		
		return null;
	}
	
	public XElement buildHtmlPart(XElement src, String id, String clss, String parentlocale, boolean isPreview) {
		String locale = src.getAttribute("Locale", parentlocale);
		String lang = locale;
		
		if (lang.indexOf("_") > -1) 
			lang = lang.substring(0, lang.indexOf("_"));
		
		String fmt = src.getAttribute("Format", "md");
		
		if ("image".equals(fmt)) {
			XElement pel = new XElement("img");
			pel.setAttribute("lang", lang);
			pel.setAttribute("src", "/galleries" + this.getPartValue(locale, src, isPreview));
			
			// copy all attributes 
			for (Entry<String, String> attr : src.getAttributes().entrySet()) 
				pel.setAttribute(attr.getKey(), attr.getValue());
			
			return pel;
		}
		else {
			XElement div = new XElement("div");
			div.setAttribute("lang", lang);
			div.setAttribute("data-dcui-mode", "enhance");
			
			if (id != null)
				div.setAttribute("id", id);
			
			if (clss != null)
				div.setAttribute("class", clss);
			
			XElement pel = new XElement("AdvText");
			pel.setAttribute("Format", src.getAttribute("Format", "md"));
			pel.withCData(this.getPartValue(locale, src, isPreview));
			
			// copy attributes to both, the correct attributes will be used with the correct element automatically (typically)
			for (Entry<String, String> attr : src.getAttributes().entrySet()) {
				div.setAttribute(attr.getKey(), attr.getValue());
				pel.setAttribute(attr.getKey(), attr.getValue());
			}
			
			div.add(pel);
			
			return div;
		}
	}
	
	public String getPartValue(String locale, XElement part, boolean isPreview) {
		if (part == null)
			return null;
		
		String ex = part.getAttribute("External", "False");
		
		if (part.hasAttribute("Locale"))
			locale = part.getAttribute("Locale");	// use the override locale if present
		
		if (StringUtil.isNotEmpty(ex) && "true".equals(ex.toLowerCase())) {
			int pos = this.key.indexOf('.');
			String spath = (pos != -1) ? this.key.substring(0, pos) : this.key;
			
			spath = spath + "." + part.getAttribute("For") + "." + locale + "." + part.getAttribute("Format");
			
			Path fpath = null;
			
			if (isPreview) {
				fpath = OperationContext.get().getDomain().resolvePath("/feed-preview" + spath);
				
				if (Files.notExists(fpath))
					fpath = OperationContext.get().getDomain().resolvePath("/feed" + spath);
			}
			else {
				fpath = OperationContext.get().getDomain().resolvePath("/feed" + spath);
			}
			
			//Path fpath = OperationContext.get().getDomain().findSectionFile(this.isPreview(), "feed", spath);
			
			//FuncResult<CharSequence> mres = IOUtil.readEntireFile(this.path.resolveSibling(sname));

			if (Files.exists(fpath)) {
				FuncResult<CharSequence> mres = IOUtil.readEntireFile(fpath);
				
				if (mres.isNotEmptyResult()) 
					return mres.getResult().toString();
			}
		}
		
		return part.getValue();
	}
	
	public XElement getAlternate(String locale) {
		if ((this.xml == null) || StringUtil.isEmpty(locale))
			return null;
		
		for (XElement afel : this.xml.selectAll("Alternate")) {
			if (locale.equals(afel.getAttribute("Locale")))
				return afel;
		}
		
		if (locale.contains("_")) 
			return this.getAlternate(locale.split("_")[0]);
		
		return null;
	}

	public XElement getXml() {
		return this.xml;
	}
}
