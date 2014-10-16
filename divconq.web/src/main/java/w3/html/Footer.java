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

public class Footer extends MixedElement implements ICodeTag {
	protected String id = null;
	protected String cssclass = null;
	
	public Footer() {
		super();
	}
	
	public Footer(Object... args) {
		super(args);
	}
	
    public Footer(String id, String cssclass, Object... args) {
    	super(args);
    	this.id = id;
    	this.cssclass = cssclass;
	}

    @Override
    protected void doCopy(Node n) {
    	super.doCopy(n);
    	
    	Footer nn = (Footer)n;
    	nn.id = this.id;
    	nn.cssclass = this.cssclass;
    }
    
	@Override
	public Node deepCopy(Element parent) {
		Footer cp = new Footer();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);

        this.myArguments = new Object[] { attrs, view.getDomain().parseXml("HtmlOutput", view, xel) };
		
		nodes.add(this);
	}
	
    @Override
	public void build(Object... args) {
		Attributes extra = new Attributes();
		
		if (this.id != null) 
			extra.add("id", this.id);
		
		if (this.cssclass != null) 
			extra.add("class", this.cssclass);
    	
	    super.build("footer", true, extra, args);
	}
}
