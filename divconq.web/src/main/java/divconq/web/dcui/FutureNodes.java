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

public class FutureNodes extends Nodes {
	protected boolean done = false;
	protected FuturePlaceholder placeholder = null;
	
	public void complete(WebContext ctx) {
		synchronized (this) {
			this.done = true;
			this.finish(ctx);
		}
	}

	public void setNotify(WebContext ctx, FuturePlaceholder placeholder) {
		synchronized (this) {
			this.placeholder = placeholder;
			
			if (this.done) 
				this.finish(ctx);			
		}
	}
	
	private void finish(WebContext ctx) {
		if (this.placeholder == null) 
			return;
		
        for (Node nn : this.getList()) 
            this.placeholder.addChild(ctx, nn);
        
        this.placeholder.decrementFuture();
	}
}
