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
package divconq.view.json;

import java.io.IOException;

import divconq.lang.op.OperationCallback;
import divconq.view.Fragment;
import divconq.view.Nodes;
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
			Nodes content = this.view.getForOutput(this.context, this);

			if (this.context.isCompleted())
				return;
		
			this.build(content);
		} 
		catch (Exception x) {
			// TODO 
			System.out.println("View builder build error: " + x);
		}  
		
	    //this.build(this.view.getForOutput(this.context, this));
    }

	@Override
	public void execute(WebContext ctx) throws Exception {
		this.context = ctx;
		
		this.context.getResponse().setHeader("Content-Type", "application/json; charset=utf-8");
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
						System.out.println("View builder execute error: " + x);
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
