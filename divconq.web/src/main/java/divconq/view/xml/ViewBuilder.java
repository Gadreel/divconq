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
package divconq.view.xml;

import java.io.IOException;

import divconq.lang.OperationCallback;
import divconq.view.Fragment;
import divconq.web.IViewBuilder;
import divconq.web.WebContext;

public class ViewBuilder extends Fragment implements IViewBuilder {
	protected WebContext context = null;
	
	public ViewBuilder() {
		super(null);
	}
	
    @Override
    public void doBuild() {
    	try {
    		this.build(this.view.getForOutput(this.context, this));
		} 
		catch (Exception x) {
			// TODO 
		}  
    }

	@Override
    public void write() throws IOException {
        this.context.getResponse().getPrintStream().println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");        
        super.write();
    }

	@Override
	public void execute(WebContext ctx) throws Exception {
		this.context = ctx;
		
		this.context.getResponse().setHeader("Content-Type", "text/xml; charset=utf-8");
		this.context.getResponse().setHeader("X-UA-Compatible", "IE=Edge,chrome=1");
		
		this.doBuild();
		
		if (this.context.isCompleted()) {
			this.context.send();
			return;
		}
	
		this.awaitForFutures(new OperationCallback() {
			@Override
			public void callback() {
				if (!ViewBuilder.this.context.isCompleted()) {
					try {
						ViewBuilder.this.write();
					} 
					catch (IOException x) {
						// TODO log
					}
				}
				
				ViewBuilder.this.context.send();
			}
		});
	}

	@Override
	public WebContext getContext() {
		return this.context;
	}
}
