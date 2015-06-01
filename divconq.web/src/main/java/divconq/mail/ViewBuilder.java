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
package divconq.mail;

import java.io.IOException;

import org.markdown4j.Markdown4jProcessor;

import divconq.hub.DomainInfo;
import divconq.hub.Hub;
import divconq.lang.Memory;
import divconq.lang.op.OperationCallback;
import divconq.lang.op.OperationResult;
import divconq.web.WebContext;
import divconq.web.dcui.Fragment;
import divconq.web.dcui.IViewExecutor;
import divconq.web.dcui.Nodes;
import divconq.xml.XElement;

public class ViewBuilder extends Fragment implements IViewExecutor {
	protected WebContext context = null;
	
	public ViewBuilder() {
		super(null);
	}
	
    @Override
    public void doBuild() {
		DomainInfo domain = Hub.instance.getDomainInfo(this.context.getDomain().getId());
		
		XElement domconfig = domain.getSettings();
		
		if (domconfig!= null) {
			XElement web = domconfig.selectFirst("Web");
			
			if (web != null) {
				if (web.hasAttribute("SignInPath")) 
					this.addParams("SignInPath", web.getAttribute("SignInPath"));
				
				if (web.hasAttribute("HomePath")) 
					this.addParams("HomePath", web.getAttribute("HomePath"));
				
				if (web.hasAttribute("PortalPath")) 
					this.addParams("PortalPath", web.getAttribute("PortalPath"));
				
				if (web.hasAttribute("SiteTitle")) 
					this.addParams("SiteTitle", web.getRawAttribute("SiteTitle"));
				
				if (web.hasAttribute("SiteAuthor")) 
					this.addParams("SiteAuthor", web.getRawAttribute("SiteAuthor"));
				
				if (web.hasAttribute("SiteCopyright")) 
					this.addParams("SiteCopyright", web.getRawAttribute("SiteCopyright"));
			}
		}
    	
    	try {
			Nodes content = this.view.getOutput(this, this.context, true);  
		
			if (content != null)
				this.build(content);
		} 
		catch (Exception x) {
			// TODO 
			System.out.println("View builder build error: " + x);
		}  
    }

	@Override
	public OperationResult execute(WebContext ctx) throws Exception {
		OperationResult or = new OperationResult();
		
		this.context = ctx;
		
		this.view.loadContext(this.context, new OperationCallback() {			
			@Override
			public void callback() {
				ViewBuilder.this.doBuild();
				
				EmailInnerContext ictx = (EmailInnerContext) ctx.getInnerContext();
			
				ViewBuilder.this.awaitForFutures(new OperationCallback() {
					@Override
					public void callback() {
						try {
							ViewBuilder.this.write();
						} 
						catch (IOException x) {
							// TODO log
							System.out.println("View builder execute error: " + x);
						}
						
						ViewBuilder.this.children.clear();		// reset our output
						
						ictx.useHtml();
						
						ViewBuilder.this.doBuild();
						
						ViewBuilder.this.awaitForFutures(new OperationCallback() {
							@Override
							public void callback() {
								if (ViewBuilder.this.children.isEmpty()) {
									System.out.println("process as MD");
									
									String md = ((EmailInnerContext) ViewBuilder.this.context.getInnerContext()).getTextResponse().getBody().toString();
									String html = null;
									
									//System.out.println("md: " + ppel.getText());
									
									try {
										html = new Markdown4jProcessor().process(md);

										Memory mem = new Memory();
										mem.write(html);
										
										((EmailInnerContext) ViewBuilder.this.context.getInnerContext()).getHtmlResponse().setBody(mem);
									} 
									catch (IOException x) {
										System.out.println("error: " + x);
									}
								}
								
								try {
									ViewBuilder.this.write();
								} 
								catch (IOException x) {
									// TODO log
									System.out.println("View builder execute error: " + x);
								}
								
								String title = ViewBuilder.this.expandMacro(ViewBuilder.this.view.getParam("PageTitle"));
								
								//String title = ViewBuilder.this.getAttribute("Title");
								
								((EmailInnerContext) ViewBuilder.this.context.getInnerContext()).setSubject(title);
								
								ViewBuilder.this.context.send();
							}
						});
					}
				});
			}
		});
		
		return or;
	}

	@Override
	public WebContext getContext() {
		return this.context;
	}
	
	@Override
	public String getParam(String name) {
    	if ((this.valueparams != null) && this.valueparams.containsKey(name))
    		return this.valueparams.get(name);
    	
    	if (this.view != null)
    		return this.view.getParam(name);
    	
    	return this.getContext().getInternalParam(name);
	}
}
