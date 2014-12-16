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
import divconq.web.dcui.Node;
import divconq.web.dcui.Nodes;
import divconq.web.dcui.ViewOutputAdapter;
import divconq.xml.XElement;

public class Table extends Element implements ICodeTag {
    public Table() {
    	super();
	}
    
    public Table(Object... args) {
    	super(args);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		Table cp = new Table();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("align"))
			attrs.add("align", xel.getRawAttribute("align"));
		
		if (xel.hasAttribute("cellspacing"))
			attrs.add("cellspacing", xel.getRawAttribute("cellspacing"));
		
		if (xel.hasAttribute("cellpadding"))
			attrs.add("cellpadding", xel.getRawAttribute("cellpadding"));
		
		if (xel.hasAttribute("width"))
			attrs.add("width", xel.getRawAttribute("width"));

        this.myArguments = new Object[] { attrs, view.getDomain().parseXml(view, xel) };
		
		nodes.add(this);
	}

    @Override
	public void build(Object... args) {
	    super.build("table",
	        true,
	        new Attributes(
	            "cellspacing", "0",
	            "cellpadding", "2",
	            "border", "0",
	            "width", "100%"
	        ),
	        args
	    );
	}
}
