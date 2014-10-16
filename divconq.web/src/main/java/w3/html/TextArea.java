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

public class TextArea extends MixedElement implements ICodeTag {
    public TextArea() {
    	super();
	}
    
    public TextArea(Object... args) {
    	super(args);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		TextArea cp = new TextArea();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("name"))
			attrs.add("name", xel.getRawAttribute("name"));
		
		if (xel.hasAttribute("cols"))
			attrs.add("cols", xel.getRawAttribute("cols"));
		
		if (xel.hasAttribute("rows"))
			attrs.add("rows", xel.getRawAttribute("rows"));
		
		if (xel.hasAttribute("disabled"))
			attrs.add("disabled", xel.getRawAttribute("disabled"));
		
		if (xel.hasAttribute("placeholder"))
			attrs.add("placeholder", xel.getRawAttribute("placeholder"));
		
		if (xel.hasAttribute("maxlength"))
			attrs.add("maxlength", xel.getRawAttribute("maxlength"));
		
		if (xel.hasAttribute("readonly"))
			attrs.add("readonly", xel.getRawAttribute("readonly"));

        this.myArguments = new Object[] { attrs, view.getDomain().parseXml("HtmlOutput", view, xel) };
		
		nodes.add(this);
	}
	
    @Override
	public void build(Object... args) {
	    super.build("textarea", true, args);
	}
}
