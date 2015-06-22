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

public class Img extends Element implements ICodeTag {
    public Img() {
    	super();
	}
    
    public Img(Object... args) {
    	super(args);
	}

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("alt"))
			attrs.add("alt", xel.getRawAttribute("alt"));
		
		if (xel.hasAttribute("src"))
			attrs.add("src", xel.getRawAttribute("src"));
		
		if (xel.hasAttribute("srcset"))
			attrs.add("srcset", xel.getRawAttribute("srcset"));
		
		if (xel.hasAttribute("sizes"))
			attrs.add("sizes", xel.getRawAttribute("sizes"));
		
		if (xel.hasAttribute("height"))
			attrs.add("height", xel.getRawAttribute("height"));
		
		if (xel.hasAttribute("width"))
			attrs.add("width", xel.getRawAttribute("width"));
		
		if (xel.hasAttribute("align"))
			attrs.add("align", xel.getRawAttribute("align"));

        this.myArguments = new Object[] { attrs, ctx.getDomain().parseXml(ctx, xel) };
		
		nodes.add(this);
	}

    @Override
	public void build(WebContext ctx, Object... args) {
	    super.build(ctx, "img", new Attributes("border", "0", "alt", ""), args);
	}
}
