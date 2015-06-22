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
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		this.paramname = xel.getRawAttribute("Name");	
		nodes.add(this);
	}

	@Override
	public void doBuild(WebContext ctx) {
        this.myArguments = new Object[] { this.getComplexParam(this.paramname) };	// TODO make this part of the compile phase
        super.doBuild(ctx);
	}
}
