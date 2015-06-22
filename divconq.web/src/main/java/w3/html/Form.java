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
import divconq.web.dcui.HtmlUtil;
import divconq.web.dcui.ICodeTag;
import divconq.web.dcui.MixedElement;
import divconq.web.dcui.Nodes;
import divconq.xml.XElement;

public class Form extends MixedElement implements ICodeTag {
    public Form() {
    	super();
	}
    
    public Form(Object... args) {
    	super(args);
	}

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("action"))
			attrs.add("action", xel.getRawAttribute("action"));
		
		if (xel.hasAttribute("autocomplete"))
			attrs.add("autocomplete", xel.getRawAttribute("autocomplete"));
		
		if (xel.hasAttribute("enctype"))
			attrs.add("enctype", xel.getRawAttribute("enctype"));
		
		if (xel.hasAttribute("method"))
			attrs.add("method", xel.getRawAttribute("method"));
		
		if (xel.hasAttribute("name"))
			attrs.add("name", xel.getRawAttribute("name"));
		
		if (xel.hasAttribute("target"))
			attrs.add("target", xel.getRawAttribute("target"));

        this.myArguments = new Object[] { attrs, ctx.getDomain().parseXml(ctx, xel) };
		
		nodes.add(this);
	}
	
    @Override
	public void build(WebContext ctx, Object... args) {
	    super.build(ctx, "form", new Attributes("method", "POST"), args);
	}
}
