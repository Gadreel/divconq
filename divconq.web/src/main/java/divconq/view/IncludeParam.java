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
package divconq.view;

import divconq.web.ViewInfo;
import divconq.xml.XElement;

public class IncludeParam extends FragmentProxy implements ICodeTag {
    protected String paramname = null;

    public IncludeParam() {
    	super();
    }
    
    public IncludeParam(String name) {
    	super();
        this.paramname = name;
    }
    
	@Override
	public Node deepCopy(Element parent) {
		IncludeParam cp = new IncludeParam(this.paramname);
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	protected void doCopy(Node n) {
		super.doCopy(n);
		((IncludeParam)n).paramname = this.paramname;
	}

	@Override
	public void doBuild() {
        super.build(this.getComplexParam(this.paramname));		// could deep copy for reuse?
	}
	
	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		this.paramname = xel.getRawAttribute("Name");	
		nodes.add(this);
	}
}
