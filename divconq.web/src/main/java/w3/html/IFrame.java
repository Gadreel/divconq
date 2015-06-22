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
package w3.html;

import divconq.web.WebContext;
import divconq.web.dcui.Attributes;
import divconq.web.dcui.Element;
import divconq.web.dcui.HtmlUtil;
import divconq.web.dcui.ICodeTag;
import divconq.web.dcui.Nodes;
import divconq.xml.XElement;

public class IFrame extends Element implements ICodeTag {
    public IFrame() {
    	super();
	}
    
    public IFrame(Object... args) {
    	super(args);
	}

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("scrolling"))
			attrs.add("scrolling", xel.getRawAttribute("scrolling"));
		
		if (xel.hasAttribute("src"))
			attrs.add("src", xel.getRawAttribute("src"));
		
		if (xel.hasAttribute("height"))
			attrs.add("height", xel.getRawAttribute("height"));
		
		if (xel.hasAttribute("width"))
			attrs.add("width", xel.getRawAttribute("width"));
		
		if (xel.hasAttribute("frameborder"))
			attrs.add("frameborder", xel.getRawAttribute("frameborder"));

        this.myArguments = new Object[] { attrs, ctx.getDomain().parseXml(ctx, xel) };
		
		nodes.add(this);
	}

    @Override
	public void build(WebContext ctx, Object... args) {
	    super.build(ctx, "iframe", true, new Attributes("frameborder", "0", "scrolling", "no"), args);
	}
}
