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

public class Td extends MixedElement implements ICodeTag {
    protected boolean LTRAdaptable = true;

    public Td() {
    	super();
    }

    public Td(Object... args) {
    	super(args);
    	
        if ((args.length > 0) && (args[0] instanceof Boolean)) 
        	this.LTRAdaptable = (Boolean)args[0];
    }
    
	@Override
	public Node deepCopy(Element parent) {
		Td cp = new Td();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	protected void doCopy(Node n) {
		super.doCopy(n);
		((Td)n).LTRAdaptable = this.LTRAdaptable;
	}

	@Override
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("colspan"))
			attrs.add("colspan", xel.getRawAttribute("colspan"));
		
		if (xel.hasAttribute("rowspan"))
			attrs.add("rowspan", xel.getRawAttribute("rowspan"));
		
		if (xel.hasAttribute("align"))
			attrs.add("align", xel.getRawAttribute("align"));
		
		if (xel.hasAttribute("width"))
			attrs.add("width", xel.getRawAttribute("width"));

        this.myArguments = new Object[] { attrs, view.getDomain().parseXml(view, xel) };
		
		nodes.add(this);
	}
	
    @Override
    public void build(Object... args) {
        String align = (this.LTRAdaptable && this.getContext().isRightToLeft()) 
        	? "right" : "left";

        super.build("td", true, new Attributes("valign", "top", "align", align), args);
    }
}
