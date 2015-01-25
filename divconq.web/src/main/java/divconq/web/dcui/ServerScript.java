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

import divconq.hub.Hub;
import w3.html.Div;
import w3.html.P;


public class ServerScript extends Div {
    protected String id = null;

    public ServerScript() {
    	super();
    }
    
	@Override
	public Node deepCopy(Element parent) {
		ServerScript cp = new ServerScript();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	protected void doCopy(Node n) {
		super.doCopy(n);
		((ServerScript)n).id = this.id;
	}

    @Override
    public void build(Object... args) {
    	FutureNodes future = new FutureNodes();
    	
        super.build(args, future);
        
        Hub.instance.getClock().scheduleOnceInternal(new Runnable() {
			@Override
			public void run() {
				future.add(new P("Cool!"));
				future.complete();
			}
		}, 3);
    }
}
