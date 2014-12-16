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

import divconq.web.dcui.Attributes;
import divconq.web.dcui.Element;
import divconq.web.dcui.HtmlUtil;
import divconq.web.dcui.ICodeTag;
import divconq.web.dcui.MixedElement;
import divconq.web.dcui.Node;
import divconq.web.dcui.Nodes;
import divconq.web.dcui.ViewOutputAdapter;
import divconq.xml.XElement;

public class Input extends MixedElement implements ICodeTag {
    public Input() {
    	super();
	}
    
    public Input(Object... args) {
    	super(args);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		Input cp = new Input();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("alt"))
			attrs.add("alt", xel.getRawAttribute("alt"));
		
		if (xel.hasAttribute("checked"))
			attrs.add("checked", xel.getRawAttribute("checked"));
		
		if (xel.hasAttribute("disabled"))
			attrs.add("disabled", xel.getRawAttribute("disabled"));
		
		if (xel.hasAttribute("placeholder"))
			attrs.add("placeholder", xel.getRawAttribute("placeholder"));
		
		if (xel.hasAttribute("maxlength"))
			attrs.add("maxlength", xel.getRawAttribute("maxlength"));
		
		if (xel.hasAttribute("multiple"))
			attrs.add("multiple", xel.getRawAttribute("multiple"));
		
		if (xel.hasAttribute("name"))
			attrs.add("name", xel.getRawAttribute("name"));
		
		if (xel.hasAttribute("readonly"))
			attrs.add("readonly", xel.getRawAttribute("readonly"));
		
		if (xel.hasAttribute("size"))
			attrs.add("size", xel.getRawAttribute("size"));
		
		if (xel.hasAttribute("src"))
			attrs.add("src", xel.getRawAttribute("src"));
		
		if (xel.hasAttribute("type"))
			attrs.add("type", xel.getRawAttribute("type"));
		
		if (xel.hasAttribute("value"))
			attrs.add("value", xel.getRawAttribute("value"));

        this.myArguments = new Object[] { attrs, view.getDomain().parseXml(view, xel) };
		
		nodes.add(this);
	}
	
    @Override
	public void build(Object... args) {
        if (this.getContext().isRightToLeft())
            super.build("input", new Attributes("dir", "rtl"), args);
        else
        	super.build("input", args);
	}
}
