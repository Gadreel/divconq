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
import divconq.xml.XElement;

public class Html extends Element implements ICodeTag {
    public Html() {
    	super();
	}
    
    public Html(Object... args) {
    	super(args);
	}

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);

        this.myArguments = new Object[] { attrs, ctx.getDomain().parseXml(ctx, xel) };
		
		nodes.add(this);
	}
	
    @Override
	public void build(WebContext ctx, Object... args) {
	    super.build(ctx, "html", 
	    		true, 
	    		new Attributes("lang", ctx.getLanguage()), 
	    		new Attributes("dir", ctx.isRightToLeft() ? "rtl" : "ltr"), 
	    		args);
	}
	
	static public Node Nbsp(int num) {
	    StringBuilder sb = new StringBuilder();
	
	    for (int i = 0; i < num; i++)
	    {
	        sb.append("&nbsp;");
	    }
	
	    return new LiteralText(sb.toString());
	}

	static public String escapeHtml(String str) {
        return org.apache.commons.lang3.StringEscapeUtils.escapeHtml4(str);
    }
}
