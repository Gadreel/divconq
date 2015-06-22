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

public class BlockQuote extends MixedElement implements ICodeTag {
	protected String id = null;
	protected String cssclass = null;
	
	public BlockQuote() {
		super();
	}
	
	public BlockQuote(Object... args) {
		super(args);
	}
	
    public BlockQuote(String id, String cssclass, Object... args) {
    	super(args);
    	this.id = id;
    	this.cssclass = cssclass;
	}

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);

        this.myArguments = new Object[] { attrs, ctx.getDomain().parseXml(ctx, xel) };
		
		nodes.add(this);
	}
	
    @Override
	public void build(WebContext ctx, Object... args) {
		Attributes extra = new Attributes();
		
		if (this.id != null) 
			extra.add("id", this.id);
		
		if (this.cssclass != null) 
			extra.add("class", this.cssclass);
    	
	    super.build(ctx, "blockquote", true, extra, args);
	}
}
