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
import divconq.view.Node;

public class LtrDiv extends Div {
	public LtrDiv() {
		super();
	}
	
    public LtrDiv(String id, String cssclass, Object... args) {
    	super(id, cssclass, args);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		LtrDiv cp = new LtrDiv();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}
	
    @Override
	public void build(Object... args) {
        Attributes alignattr = new Attributes("align", 
        		(this.getContext().isRightToLeft()) ? "right" : "left");
        
        super.build(alignattr, args);
	}
}
