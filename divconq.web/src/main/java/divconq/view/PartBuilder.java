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

public class PartBuilder extends Fragment {
	public PartBuilder(ViewInfo info) {
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
    	// fine to catch, this part just won't show...
    	
    	try {
    		this.build(this.view.getForOutput(this.getContext(), this));
    	}
    	catch (Exception x) {    		
			// TODO: log?
		}
    }
}
