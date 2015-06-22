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

public class Body extends MixedElement implements ICodeTag {	
    public Body() {
    	super();
	}

    public Body(Object... args) {
    	super(args);
	}

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("onload"))
			attrs.add("onload", xel.getRawAttribute("onload"));
		
        this.myArguments = new Object[] { attrs, ctx.getDomain().parseXml(ctx, xel) };
		
		nodes.add(this);
	}
	
    @Override
	public void build(WebContext ctx, Object... args) {
	    super.build(ctx, "body", true, args);
	}
	
    /* TODO
	public void buildBodyAttrs() {
	    string onload = String.Empty;
	
	    foreach (string code in Root.Context.PageContext.OnLoadCode)
	    {
	        onload += code;
	    }
	
	    if (onload != String.Empty) base.Build(new Attributes("onload", onload));
	}
	*/
}
