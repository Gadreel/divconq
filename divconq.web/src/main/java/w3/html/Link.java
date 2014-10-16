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
import divconq.view.MixedElement;
import divconq.view.Node;
import divconq.view.Nodes;
import divconq.view.html.HtmlUtil;
import divconq.web.ViewInfo;
import divconq.xml.XElement;

public class Link extends MixedElement implements ICodeTag {
    public Link() {
    	super();
	}
    
    public Link(Object... args) {
    	super(args);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		Link cp = new Link();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("rel"))
			attrs.add("rel", xel.getRawAttribute("rel"));
		
		if (xel.hasAttribute("href"))
			attrs.add("href", xel.getRawAttribute("href"));
		
		if (xel.hasAttribute("sizes"))
			attrs.add("sizes", xel.getRawAttribute("sizes"));
		
		if (xel.hasAttribute("type"))
			attrs.add("type", xel.getRawAttribute("type"));
		
		if (xel.hasAttribute("media"))
			attrs.add("media", xel.getRawAttribute("media"));

        this.myArguments = new Object[] { attrs, view.getDomain().parseXml("HtmlOutput", view, xel) };
		
		nodes.add(this);
	}
	
    @Override
	public void build(Object... args) {
	    super.build("link", true, args);
	}
}
