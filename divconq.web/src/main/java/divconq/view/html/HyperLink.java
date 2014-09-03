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
package divconq.view.html;

import divconq.view.Element;
import divconq.view.Node;
import divconq.view.Nodes;
import divconq.web.ViewInfo;
import divconq.xml.XElement;
import w3.html.A;


public class HyperLink extends A {
    protected String to = null;

    public HyperLink() {
    	super();
    }
    
    public HyperLink(String to, Object... args) {
    	super(args);
        this.to = to;
    }
    
	@Override
	public Node deepCopy(Element parent) {
		HyperLink cp = new HyperLink();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	protected void doCopy(Node n) {
		super.doCopy(n);
		((HyperLink)n).to = this.to;
	}
	
	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		super.parseElement(view, nodes, xel);
		this.to = xel.getRawAttribute("To");
	}

    @Override
    public void build(Object... args) {
    	// TODO
        // super.build(new Attributes("href", this.getContext().buildViewPath(this.to)), args);
    }
}
