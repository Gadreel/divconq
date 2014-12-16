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
package divconq.web.dcui;

import divconq.web.WebContext;
import divconq.xml.XElement;

public class IncludeHolder extends FragmentProxy implements ICodeTag {
    protected String holdername = null;

    public IncludeHolder() {
    	super();
    }
    
    public IncludeHolder(String holder) {
    	super();
        this.holdername = holder;
    }
    
	@Override
	public Node deepCopy(Element parent) {
		IncludeHolder cp = new IncludeHolder();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	protected void doCopy(Node n) {
		super.doCopy(n);
		((IncludeHolder)n).holdername = this.holdername;
	}

	@Override
	public void doBuild() {
		WebContext ctx = this.getContext();
		
		ContentPlaceholder ph = ctx.getHolder(this.holdername);
		
		super.build(ph);
	}
	
	@Override
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
		this.holdername = xel.getRawAttribute("Name");	
		nodes.add(this);
	}
}
