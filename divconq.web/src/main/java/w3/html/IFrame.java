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

public class IFrame extends Element implements ICodeTag {
    public IFrame() {
    	super();
	}
    
    public IFrame(Object... args) {
    	super(args);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		IFrame cp = new IFrame();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("scrolling"))
			attrs.add("scrolling", xel.getRawAttribute("scrolling"));
		
		if (xel.hasAttribute("src"))
			attrs.add("src", xel.getRawAttribute("src"));
		
		if (xel.hasAttribute("height"))
			attrs.add("height", xel.getRawAttribute("height"));
		
		if (xel.hasAttribute("width"))
			attrs.add("width", xel.getRawAttribute("width"));
		
		if (xel.hasAttribute("frameborder"))
			attrs.add("frameborder", xel.getRawAttribute("frameborder"));

        this.myArguments = new Object[] { attrs, view.getDomain().parseXml("HtmlOutput", view, xel) };
		
		nodes.add(this);
	}

    @Override
	public void build(Object... args) {
	    super.build("iframe", true, new Attributes("frameborder", "0", "scrolling", "no"), args);
	}
}
