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
package divconq.view.xml;

import divconq.view.Element;
import divconq.view.MixedElement;
import divconq.view.Node;

public class XElement extends MixedElement {
	public XElement(Object... args) {
		
	}
	
    public XElement(String name, boolean block, Object... args) {
    	super(args);
    	this.name = name;
    	this.blockindent = block;
	}
    
	@Override
	public Node deepCopy(Element parent) {
		XElement cp = new XElement();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}
	
    @Override
	public void build(Object... args) {
	    super.build(this.name, this.blockindent, args);
	}
}
