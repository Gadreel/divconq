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

public class Table extends Element implements ICodeTag {
    public Table() {
    	super();
	}
    
    public Table(Object... args) {
    	super(args);
	}

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("align"))
			attrs.add("align", xel.getRawAttribute("align"));
		
		if (xel.hasAttribute("cellspacing"))
			attrs.add("cellspacing", xel.getRawAttribute("cellspacing"));
		
		if (xel.hasAttribute("cellpadding"))
			attrs.add("cellpadding", xel.getRawAttribute("cellpadding"));
		
		if (xel.hasAttribute("width"))
			attrs.add("width", xel.getRawAttribute("width"));

        this.myArguments = new Object[] { attrs, ctx.getDomain().parseXml(ctx, xel) };
		
		nodes.add(this);
	}

    @Override
	public void build(WebContext ctx, Object... args) {
	    super.build(ctx, "table",
	        true,
	        new Attributes(
	            "cellspacing", "0",
	            "cellpadding", "2",
	            "border", "0",
	            "width", "100%"
	        ),
	        args
	    );
	}
}
