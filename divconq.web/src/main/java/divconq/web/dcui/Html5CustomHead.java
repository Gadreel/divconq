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
package divconq.web.dcui;

import divconq.util.StringUtil;
import divconq.xml.XElement;
import w3.html.Head;
import w3.html.Link;
import w3.html.Meta;
import w3.html.Script;
import w3.html.Style;
import w3.html.Title;

/*
 * 
 */
public class Html5CustomHead extends Head implements ICodeTag {
	protected divconq.xml.XElement source = null;
	protected divconq.xml.XElement domconfig = null;
	
    public Html5CustomHead() {
    	super();
	}
	
    public Html5CustomHead(divconq.xml.XElement source, XElement domconfig, Object... args) {
    	super(args);
    	this.source = source;
    	this.domconfig = domconfig;
	}
    
	@Override
	public Node deepCopy(Element parent) {
		Html5CustomHead cp = new Html5CustomHead(this.source, this.domconfig);
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		this.source = xel;
        this.myArguments = new Object[] { attrs, view.getDomain().parseXml(view, xel) };
		
		nodes.add(this);
	}
	
	@Override
	public void doBuild() {
		Nodes earlyadditional = new Nodes();
		Nodes lateadditional = new Nodes();
		
		// TODO some of this - except for cookie - can move up into parse area
		
		earlyadditional.add(
				new Meta(new Attributes("chartset", "utf-8")),
				new Meta(new Attributes("name", "format-detection", "content", "telephone=no"))
		);
		
		if ("False".equals(this.source.getAttribute("Public")))
			earlyadditional.add(
					new Meta(new Attributes("name", "robots", "content", "noindex,nofollow"))
			);
		else
			earlyadditional.add(
					new Meta(new Attributes("name", "robots", "content", "index,follow"))
			);
		
		earlyadditional.add(
				new Meta(new Attributes("name", "viewport", "content", "width=device-width, initial-scale=1, maximum-scale=1.0, user-scalable=no"))
		);
		
		String title = "@val|PageTitle@ - @val|SiteTitle@";
		
		if (StringUtil.isNotEmpty(title))
			earlyadditional.add(
					new Title(title)
			);
		
		String icon = this.source.getRawAttribute("Icon");
		
		if (StringUtil.isEmpty(icon))
			icon = this.source.getRawAttribute("Icon16");
		
		if (StringUtil.isNotEmpty(icon)) { 
			// if full name then use as the 16x16 version
			if (icon.endsWith(".png")) {
				earlyadditional.add(
						new Link(new Attributes("type", "image/png", "rel", "shortcut icon", "href", icon)),
						new Link(new Attributes("sizes", "16x16", "rel", "icon", "href", icon))
				);
			}
			else {
				earlyadditional.add(
						new Link(new Attributes("type", "image/png", "rel", "shortcut icon", "href", icon + "16.png")),
						new Link(new Attributes("sizes", "16x16", "rel", "icon", "href", icon + "16.png")),
						new Link(new Attributes("sizes", "32x32", "rel", "icon", "href", icon + "32.png")),
						new Link(new Attributes("sizes", "48x48", "rel", "icon", "href", icon + "48.png")),
						new Link(new Attributes("sizes", "64x64", "rel", "icon", "href", icon + "64.png")),
						new Link(new Attributes("sizes", "128x128", "rel", "icon", "href", icon + "128.png"))
				);
			}
		}
		
		icon = this.source.getRawAttribute("Icon32");
		
		if (StringUtil.isNotEmpty(icon)) { 
			earlyadditional.add(
					new Link(new Attributes("sizes", "32x32", "rel", "icon", "href", icon))
			);
		}
		
		icon = this.source.getRawAttribute("Icon64");
		
		if (StringUtil.isNotEmpty(icon)) { 
			earlyadditional.add(
					new Link(new Attributes("sizes", "64x64", "rel", "icon", "href", icon))
			);
		}
		
		divconq.xml.XElement del = this.source.find("Description");
		String desc = (del != null) ? del.getText() : "@val|SiteDescription@";
		
		if (StringUtil.isNotEmpty(desc))
			earlyadditional.add(
					new Meta(new Attributes("name", "description", "content", desc))
			);
		
		divconq.xml.XElement kel = this.source.find("Keywords");
		String keywords = (kel != null) ? kel.getText() : "@val|SiteKeywords@";
		
		if (StringUtil.isNotEmpty(keywords))
			earlyadditional.add(
					new Meta(new Attributes("name", "keywords", "content", keywords))
			);
		
		XElement web = this.domconfig.selectFirst("Web");
		
		if (web != null) {
			for (XElement gel : web.selectAll("Global")) {
				if (gel.hasAttribute("Style"))
					earlyadditional.add(new Style(gel.getAttribute("Style")));
				else
					earlyadditional.add(new Script(gel.getAttribute("Script")));
			}
		}
		
		// additional scripts/styles
		
		lateadditional.add(new IncludeHolder("Styles"));
		
		lateadditional.add(new IncludeHolder("Scripts"));
		
	    this.build(earlyadditional, this.myArguments, lateadditional);
	}
}
