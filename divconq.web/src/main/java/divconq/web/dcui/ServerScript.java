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
import divconq.web.WebContext;
import w3.html.Div;
import w3.html.P;


public class ServerScript extends Div {
    protected String id = null;

    public ServerScript() {
    	super();
    }

    @Override
    public void build(WebContext ctx, Object... args) {
    	FutureNodes future = new FutureNodes();
    	
        super.build(ctx, args, future);
        
        Hub.instance.getClock().scheduleOnceInternal(new Runnable() {
			@Override
			public void run() {
				future.add(new P("Cool!"));
				future.complete(ctx);
			}
		}, 3);
    }
}
