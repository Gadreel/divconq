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
import divconq.web.dcui.LiteralText;
import divconq.web.dcui.Node;
import divconq.web.dcui.Nodes;
import divconq.web.dcui.UnescapedText;
import divconq.xml.XElement;

public class Title extends Element implements ICodeTag {
    protected Node title = null;

    public Title() {
    	super();    	
    }
    
    public Title(String title) {
    	super();
        this.title = new UnescapedText(false, title);
    }

    public Title(UnescapedText title) {
    	super();
        this.title = title;
    }

    public Title(LiteralText title) {
    	super();
        this.title = title;
    }

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);

        this.myArguments = new Object[] { attrs, ctx.getDomain().parseXml(ctx, xel) };
		
		nodes.add(this);
	}

    @Override
    public void build(WebContext ctx, Object... args) {
        super.build(ctx, "title", this.title, args);
    }
}
