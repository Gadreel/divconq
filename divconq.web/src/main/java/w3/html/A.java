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

public class A extends MixedElement implements ICodeTag {
    public A() {
    	super();
    }
    
    public A(Object... args) {
    	super(args);
	}
	
    @Override
	public void build(Object... args) {
	    super.build("a", args);
	}

	@Override
	public Node deepCopy(Element parent) {
		A cp = new A();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("href"))
			attrs.add("href", xel.getRawAttribute("href"));
		
		if (xel.hasAttribute("rel"))
			attrs.add("rel", xel.getRawAttribute("rel"));
		
		if (xel.hasAttribute("target"))
			attrs.add("target", xel.getRawAttribute("target"));

        this.myArguments = new Object[] { attrs, view.getDomain().parseXml("HtmlOutput", view, xel) };
		
		nodes.add(this);
	}
}
