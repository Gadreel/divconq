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

import divconq.util.IProcessCallback;

public class IncludeCallback extends FragmentProxy {
    protected IProcessCallback<IncludeCallback> callback = null;

    public IncludeCallback() {
    	super();
    }
    
    public IncludeCallback(IProcessCallback<IncludeCallback> callback) {
    	super();
        this.callback = callback;
    }
    
	@Override
	public Node deepCopy(Element parent) {
		IncludeCallback cp = new IncludeCallback();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	protected void doCopy(Node n) {
		super.doCopy(n);
		((IncludeCallback)n).callback = this.callback;
	}

	@Override
	public void doBuild() {
		//WebContext ctx = this.getContext();
		
		this.callback.callback(this);	// should call add
		
		//super.build(ph);
	}
}
