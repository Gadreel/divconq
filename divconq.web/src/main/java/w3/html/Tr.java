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

import divconq.util.ArrayUtil;
import divconq.web.dcui.Attributes;
import divconq.web.dcui.Element;
import divconq.web.dcui.HtmlUtil;
import divconq.web.dcui.ICodeTag;
import divconq.web.dcui.Node;
import divconq.web.dcui.Nodes;
import divconq.web.dcui.ViewOutputAdapter;
import divconq.xml.XElement;


public class Tr extends Element implements ICodeTag {
    protected boolean LTRAdaptable = true;

    public Tr() {
    	super();
    }

    public Tr(Object... args) {
    	super(args);
    	
        if ((args.length > 0) && (args[0] instanceof Boolean)) 
        	this.LTRAdaptable = (Boolean)args[0];
    }
    
	@Override
	public Node deepCopy(Element parent) {
		Tr cp = new Tr();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	protected void doCopy(Node n) {
		super.doCopy(n);
		((Tr)n).LTRAdaptable = this.LTRAdaptable;
	}

	@Override
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);

        this.myArguments = new Object[] { attrs, view.getDomain().parseXml(view, xel) };
		
		nodes.add(this);
	}

    @Override
    public void build(Object... args) {
        if (this.LTRAdaptable && this.getContext().isRightToLeft()) 
        	ArrayUtil.reverse(args);

        super.build("tr", true, args);
    }
}
