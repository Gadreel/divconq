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

import divconq.lang.Memory;
import divconq.lang.op.OperationCallback;
import divconq.web.IOutputAdapter;
import divconq.web.WebContext;
import divconq.web.dcui.Fragment;
import divconq.web.dcui.IViewBuilder;
import divconq.web.md.Processor;
import divconq.xml.XElement;

public class ViewBuilder implements IViewBuilder {
	@Override
	public void execute(WebContext ctx, IOutputAdapter adapt) throws Exception {
		Fragment frag = new Fragment();
		
		frag.initializeRoot(ctx, adapt, new OperationCallback() {			
			@Override
			public void callback() {
				ctx.setServerScript(frag.getServerScript());
				
				XElement src = frag.getSource();
				
				ctx.putInternalParam("PageTitle", ctx.expandMacros(src.getAttribute("Title")));
				
				frag.doBuild(ctx);
				
				frag.awaitForFutures(new OperationCallback() {
					@Override
					public void callback() {
				    	try {
				        	frag.write(ctx);
				        	
							EmailInnerContext ictx = (EmailInnerContext) ctx.getInnerContext();
							ictx.useHtml();
							
				    		Fragment htmlfrag = new Fragment();
				    		
				    		htmlfrag.initializeRoot(ctx, adapt, new OperationCallback() {			
				    			@Override
				    			public void callback() {
				    				ctx.setServerScript(frag.getServerScript());
				    				
				    				htmlfrag.doBuild(ctx);
				    				
				    				htmlfrag.awaitForFutures(new OperationCallback() {
				    					@Override
				    					public void callback() {
				    				    	try {
				    				    		if (htmlfrag.getChildren().isEmpty()) {
													//System.out.println("process as MD");
													
													try {
														String md = ictx.getTextResponse().getBody().toString();
													
														XElement html = Processor.parse(ctx.getMarkdownContext(), md);

														Memory mem = new Memory();
														mem.write(html.toInnerString(true));
														
														ictx.getHtmlResponse().setBody(mem);
													} 
													catch (IOException x) {
														System.out.println("error: " + x);
													}
				    				    		}
				    				    		else {
				    				    			htmlfrag.write(ctx);
				    				    		}
												
												//ictx.setSubject(ctx.expandMacros(frag.getSource().getAttribute("Title")));
												ictx.setSubject(ctx.getInternalParam("PageTitle"));
				    				        	
				    				    		ctx.send();
				    						} 
				    						catch (Exception x) {
				    							// TODO 
				    							System.out.println("View builder build error: " + x);
				    						}  
				    					}
				    				});
				    			}
				    		});
						} 
						catch (Exception x) {
							// TODO 
							System.out.println("View builder build error: " + x);
						}  
					}
				});
			}
		});
	}
}
