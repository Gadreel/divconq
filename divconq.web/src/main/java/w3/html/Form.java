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

public class Form extends MixedElement implements ICodeTag {
    public Form() {
    	super();
	}
    
    public Form(Object... args) {
    	super(args);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		Form cp = new Form();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("action"))
			attrs.add("action", xel.getRawAttribute("action"));
		
		if (xel.hasAttribute("autocomplete"))
			attrs.add("autocomplete", xel.getRawAttribute("autocomplete"));
		
		if (xel.hasAttribute("enctype"))
			attrs.add("enctype", xel.getRawAttribute("enctype"));
		
		if (xel.hasAttribute("method"))
			attrs.add("method", xel.getRawAttribute("method"));
		
		if (xel.hasAttribute("name"))
			attrs.add("name", xel.getRawAttribute("name"));
		
		if (xel.hasAttribute("target"))
			attrs.add("target", xel.getRawAttribute("target"));

        this.myArguments = new Object[] { attrs, view.getDomain().parseXml("HtmlOutput", view, xel) };
		
		nodes.add(this);
	}
	
    @Override
	public void build(Object... args) {
	    super.build("form", new Attributes("method", "POST"), args);
	}
}
