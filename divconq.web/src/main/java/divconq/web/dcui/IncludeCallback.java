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

import divconq.util.IProcessCallback;
import divconq.web.WebContext;

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
	public void doBuild(WebContext ctx) {
		//WebContext ctx = this.getContext();
		
		this.callback.callback(this);	// should call add
		
		//super.build(ph);
	}
}
