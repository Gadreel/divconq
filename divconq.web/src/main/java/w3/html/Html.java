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

import divconq.view.Attributes;
import divconq.view.Element;
import divconq.view.ICodeTag;
import divconq.view.LiteralText;
import divconq.view.Node;
import divconq.view.Nodes;
import divconq.view.html.HtmlUtil;
import divconq.web.ViewInfo;
import divconq.web.WebContext;
import divconq.xml.XElement;

public class Html extends Element implements ICodeTag {
    public Html() {
    	super();
	}
    
    public Html(Object... args) {
    	super(args);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		Html cp = new Html();		// no view
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);

        this.myArguments = new Object[] { attrs, view.getDomain().parseXml("HtmlOutput", view, xel) };
		
		nodes.add(this);
	}
	
    @Override
	public void build(Object... args) {
    	WebContext ctx = this.getContext();
    	
	    super.build("html", 
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
