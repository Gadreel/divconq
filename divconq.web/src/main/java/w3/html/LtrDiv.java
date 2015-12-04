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

import divconq.lang.op.OperationContext;
import divconq.web.WebContext;
import divconq.web.dcui.Attributes;

public class LtrDiv extends Div {
	public LtrDiv() {
		super();
	}
	
    public LtrDiv(String id, String cssclass, Object... args) {
    	super(id, cssclass, args);
	}
	
    @Override
	public void build(WebContext ctx, Object... args) {
        Attributes alignattr = new Attributes("align", 
        		(OperationContext.get().getWorkingLocaleDefinition().isRightToLeft()) ? "right" : "left");
        
        super.build(ctx, alignattr, args);
	}
}
