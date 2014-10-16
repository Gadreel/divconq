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
import divconq.view.Node;
import divconq.view.Nodes;
import divconq.view.html.HtmlUtil;
import divconq.web.ViewInfo;
import divconq.xml.XElement;

public class Meta extends Element implements ICodeTag {
    public Meta() {
    	super();
	}
    
    public Meta(Object... args) {
    	super(args);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		Meta cp = new Meta();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("charset"))
			attrs.add("charset", xel.getRawAttribute("charset"));
		
		if (xel.hasAttribute("name"))
			attrs.add("name", xel.getRawAttribute("name"));
		
		if (xel.hasAttribute("content"))
			attrs.add("content", xel.getRawAttribute("content"));

        this.myArguments = new Object[] { attrs, view.getDomain().parseXml("HtmlOutput", view, xel) };
		
		nodes.add(this);
	}
	
    @Override
	public void build(Object... args) {
	    super.build("meta", true, args);
	}
}
