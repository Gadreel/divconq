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
package divconq.view.html;

import io.netty.handler.codec.http.Cookie;
import divconq.util.StringUtil;
import divconq.view.Attributes;
import divconq.view.Element;
import divconq.view.ICodeTag;
import divconq.view.IncludeHolder;
import divconq.view.Node;
import divconq.view.Nodes;
import divconq.web.ViewInfo;
import divconq.xml.XElement;
import w3.html.Head;
import w3.html.Link;
import w3.html.Meta;
import w3.html.Script;
import w3.html.Style;
import w3.html.Title;

/*
 * TODO these libs are not in this project - fix that
 * 
 * <Html5Head Title="NNN" Public="[T]/F" Mobile="[T]/F" Icon="/dcm/Asset/icon" Include="dcCore,dcManager,jQueryMobile">
 * 		<Keywords></Keywords>
 * 		<Description></Description>
 * </Html5Head>
 * 
 * see also http://wiki.whatwg.org/wiki/MetaExtensions
 * 		rating
 * 		
 * 
 */
public class Html5Head extends Head implements ICodeTag {
	protected divconq.xml.XElement source = null;
	
    public Html5Head() {
    	super();
	}
	
    public Html5Head(divconq.xml.XElement source, Object... args) {
    	super(args);
    	this.source = source;
	}
    
	@Override
	public Node deepCopy(Element parent) {
		Html5Head cp = new Html5Head(this.source);
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		this.source = xel;
        this.myArguments = new Object[] { attrs, view.getDomain().parseXml("HtmlOutput", view, xel) };
		
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
				// already in HTTP headers
				//new Meta(new Attributes("http-equiv", "X-UA-Compatible", "content", "chrome=1"))
		);
		
		if ("False".equals(this.source.getAttribute("Public")))
			earlyadditional.add(
					new Meta(new Attributes("name", "robots", "content", "noindex,nofollow"))
			);
		else
			earlyadditional.add(
					new Meta(new Attributes("name", "robots", "content", "index,follow"))
			);
		
		if ("Ture".equals(this.source.getAttribute("Mobile")))
			earlyadditional.add(
					new Meta(new Attributes("name", "viewport", "content", "width=device-width, initial-scale=1"))
			);
		
		String title = this.source.hasAttribute("Title") 
				? this.source.getRawAttribute("Title")
				: this.hasParam("dcmTitle") ? "@dcmTitle@" : null;
		
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
		
		String desc = this.hasParam("dcmDescription") ? "@dcmDescription@" : null;
		divconq.xml.XElement del = this.source.find("Description");
		
		if (del != null)
			desc = del.getText();
		
		if (StringUtil.isNotEmpty(desc))
			earlyadditional.add(
					new Meta(new Attributes("name", "description", "content", desc))
			);
		
		String keywords = this.hasParam("dcmKeywords") ? "@dcmKeywords@" : null;
		divconq.xml.XElement kel = this.source.find("Keywords");
		
		if (kel != null)
			keywords = kel.getText();
		
		if (StringUtil.isNotEmpty(keywords))
			earlyadditional.add(
					new Meta(new Attributes("name", "keywords", "content", keywords))
			);
		
		String inc = this.source.getAttribute("Include");
		
		if (StringUtil.isNotEmpty(inc) &&  inc.contains("Require")) { 
			earlyadditional.add(
					new Script("/lib/require.js")
			);
		}
		
		if (StringUtil.isNotEmpty(inc) &&  inc.contains("dcCore")) { 
			if (inc.contains("dcInlineCms"))			
				earlyadditional.add(new Script("/lib/vendor/jquery-1.7.2.js"));
			else if (inc.contains("jq82"))			
				earlyadditional.add(new Script("/lib/vendor/jquery-1.8.2.js"));
			else if (inc.contains("jq83"))			
				earlyadditional.add(new Script("/lib/vendor/jquery-1.8.3.min.js"));
			else
				earlyadditional.add(new Script("/lib/vendor/jquery-1.9.1.min.js"));
			
			earlyadditional.add(
					new Script("/lib/user/detect_timezone.js"),
					new Script("/lib/user/aes.js"),
					new Script("/lib/user/dcCore.js") 
			);
		}
		
		if (StringUtil.isNotEmpty(inc) &&  inc.contains("dcManager")) { 
			earlyadditional.add(
					new Script("/lib/user/dcManager.js")
			);
		}
		
		// css/support js for mobile
		if (StringUtil.isNotEmpty(inc) &&  inc.contains("jQueryMobile")) {
			earlyadditional.add(
					new Style("/css/jquery.mobile-1.3.1.min.css"),
					new Style("/css/jqm-icon-pack-2.1.2-fa.css"),
					new Script("/lib/user/jquery.dcforms.js"),
					new Script("/lib/vendor/jquery-validate/jquery.validate.js"),
					new Script("/lib/vendor/jquery-validate/additional-methods.js")
			);
		}
		
		// additional scripts/styles
		
		lateadditional.add(new IncludeHolder("Styles"));
		
		lateadditional.add(new IncludeHolder("Scripts"));
		
	    // js for mobile
		if (StringUtil.isNotEmpty(inc) &&  inc.contains("jQueryMobile")) {
			lateadditional.add(
					new Script("/lib/vendor/jquery.mobile-1.3.1.min.js")
			);
		}
		
		if (StringUtil.isNotEmpty(inc) &&  inc.contains("dcInlineCms")) {
			Cookie pm = this.getContext().getRequest().getCookie("dcmReveal");
			
			if ((pm != null) && "1".equals(pm.getValue())) {
				lateadditional.add(
					new Style("/css/aloha.css"),
					new Style("/css/dcmInline.css"),
					new Style("/css/jquery-ui-1.9.0.custom.min.css"),
					new Script("/lib/user/aloha-config.js"),
					new Script("/lib/aloha.js", 
							new Attributes("data-aloha-plugins", "common/ui,common/format,common/table,common/paste,common/highlighteditables,common/link,common/dom-to-xhtml,common/characterpicker,common/image,extra/imagebrowser,user/dcm")
					)
				);
			}			
		}		
		
	    this.build(earlyadditional, this.myArguments, lateadditional);
	}
}
