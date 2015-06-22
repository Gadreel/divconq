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

import divconq.util.ArrayUtil;
import divconq.web.WebContext;
import divconq.web.dcui.Attributes;
import divconq.web.dcui.Element;
import divconq.web.dcui.HtmlUtil;
import divconq.web.dcui.ICodeTag;
import divconq.web.dcui.Nodes;
import divconq.xml.XElement;


public class Tr extends Element implements ICodeTag {
    protected boolean LTRAdaptable = true;

    public Tr() {
    	super();
    }

    public Tr(Object... args) {
    	super(args);
    	
        if ((args.length > 0) && (args[0] instanceof Boolean)) 
        	this.LTRAdaptable = (Boolean)args[0];
    }

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);

        this.myArguments = new Object[] { attrs, ctx.getDomain().parseXml(ctx, xel) };
		
		nodes.add(this);
	}

    @Override
    public void build(WebContext ctx, Object... args) {
        if (this.LTRAdaptable && ctx.isRightToLeft()) 
        	ArrayUtil.reverse(args);

        super.build(ctx, "tr", true, args);
    }
}
