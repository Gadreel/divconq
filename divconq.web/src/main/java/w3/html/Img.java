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

public class Img extends Element implements ICodeTag {
    public Img() {
    	super();
	}
    
    public Img(Object... args) {
    	super(args);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		Img cp = new Img();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("alt"))
			attrs.add("alt", xel.getRawAttribute("alt"));
		
		if (xel.hasAttribute("src"))
			attrs.add("src", xel.getRawAttribute("src"));
		
		if (xel.hasAttribute("height"))
			attrs.add("height", xel.getRawAttribute("height"));
		
		if (xel.hasAttribute("width"))
			attrs.add("width", xel.getRawAttribute("width"));
		
		if (xel.hasAttribute("align"))
			attrs.add("align", xel.getRawAttribute("align"));

        this.myArguments = new Object[] { attrs, view.getDomain().parseXml("HtmlOutput", view, xel) };
		
		nodes.add(this);
	}

    @Override
	public void build(Object... args) {
	    super.build("img", new Attributes("border", "0", "alt", ""), args);
	}
}
