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

import divconq.lang.op.OperationResult;
import divconq.web.WebContext;
import divconq.xml.XElement;


public class PartBuilder extends Fragment implements IViewExecutor {
	public PartBuilder(ViewOutputAdapter info) {
		super(info);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		PartBuilder cp = new PartBuilder(null);
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}
	
    @Override
    public void doBuild() {
		String mode = this.getContext().getExternalParam("_dcui");

		boolean dyn = ("dyn".equals(mode) || "dyn".equals(mode));
		
		for (XElement func : this.view.source.selectAll("Function")) {
			this.getContext().addFunction(func);
		}
		
    	// fine to catch, this part just won't show...
    	try {
    		this.build(this.view.getOutput(this, this.getContext(), dyn));
    	}
    	catch (Exception x) {    		
			// TODO: log?
		}
    }

	@Override
	public OperationResult execute(WebContext ctx) {
		System.out.println("Part Builder Run directly...");
		return null;
	}
}
