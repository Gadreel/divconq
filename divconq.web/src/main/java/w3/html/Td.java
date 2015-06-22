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

public class Td extends MixedElement implements ICodeTag {
    protected boolean LTRAdaptable = true;

    public Td() {
    	super();
    }

    public Td(Object... args) {
    	super(args);
    	
        if ((args.length > 0) && (args[0] instanceof Boolean)) 
        	this.LTRAdaptable = (Boolean)args[0];
    }

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("colspan"))
			attrs.add("colspan", xel.getRawAttribute("colspan"));
		
		if (xel.hasAttribute("rowspan"))
			attrs.add("rowspan", xel.getRawAttribute("rowspan"));
		
		if (xel.hasAttribute("align"))
			attrs.add("align", xel.getRawAttribute("align"));
		
		if (xel.hasAttribute("width"))
			attrs.add("width", xel.getRawAttribute("width"));

        this.myArguments = new Object[] { attrs, ctx.getDomain().parseXml(ctx, xel) };
		
		nodes.add(this);
	}
	
    @Override
    public void build(WebContext ctx, Object... args) {
        String align = (this.LTRAdaptable && ctx.isRightToLeft()) 
        	? "right" : "left";

        super.build(ctx, "td", true, new Attributes("valign", "top", "align", align), args);
    }
}
