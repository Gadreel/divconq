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

public class FutureNodes extends Nodes {
	protected boolean done = false;
	protected FuturePlaceholder placeholder = null;
	
	public void signal() {
		synchronized (this) {
			this.done = true;
			this.complete();
		}
	}

	public void setNotify(FuturePlaceholder placeholder) {
		synchronized (this) {
			this.placeholder = placeholder;
			
			if (this.done) 
				this.complete();			
		}
	}
	
	private void complete() {
		if (this.placeholder == null) 
			return;
		
        for (Node nn : this.getList()) 
            this.placeholder.addChild(nn);
        
        this.placeholder.decrementFuture();
	}
}
