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

import java.io.IOException;
import java.io.PrintStream;

import divconq.lang.op.OperationCallback;
import divconq.util.StringUtil;
import divconq.web.IOutputAdapter;
import divconq.web.WebContext;
import divconq.xml.XElement;

public class ViewBuilder implements IViewBuilder {
	protected boolean dynamic = false;
	protected Fragment frag = null;

	@Override
	public void execute(WebContext ctx, IOutputAdapter adapt) throws Exception {
		//System.out.println("a " + System.currentTimeMillis());
		
		String mode = ctx.getExternalParam("_dcui");

		if ("dyn".equals(mode) || "dyn".equals(mode)) 
			this.dynamic = true;
			
		this.frag = new Fragment();
		
		if (this.dynamic) {
			this.frag.initializePart(ctx, adapt, new OperationCallback() {			
				@Override
				public void callback() {
					String pclass = ViewBuilder.this.frag.source.getAttribute("PageClass");
					XElement skel = ViewBuilder.this.frag.source.find("Skeleton");
					String sclass = skel.getAttribute("class");
					
					String fclass = "";
					
					if (StringUtil.isNotEmpty(pclass))
						fclass += pclass + " ";
					
					if (StringUtil.isNotEmpty(sclass))
						fclass += sclass + " ";
					
					ViewBuilder.this.frag.addArgs(new Attributes("class", fclass));
					
					ViewBuilder.this.doBuild(ctx);
				}
			});
		}
		else {
			this.frag.initializeRoot(ctx, adapt, new OperationCallback() {			
				@Override
				public void callback() {
					ViewBuilder.this.doBuild(ctx);
				}
			});
		}
	}

	public void doBuild(WebContext ctx) {		
		ctx.setServerScript(this.frag.getServerScript());
		
		XElement src = ViewBuilder.this.frag.getSource();
		
		ctx.putInternalParam("PageTitle", ctx.expandMacros(src.getAttribute("Title")));
		
		this.frag.doBuild(ctx);
		
		this.frag.awaitForFutures(new OperationCallback() {
			@Override
			public void callback() {
		    	try {
		    		ViewBuilder.this.doWrite(ctx);
				} 
				catch (Exception x) {
					// TODO 
					System.out.println("View builder build error: " + x);
				}  
			}
		});
    }

	public void doWrite(WebContext ctx) throws IOException {
		XElement src = this.frag.getSource();
		
		if (this.dynamic) {
			ctx.getResponse().setHeader("Content-Type", "application/javascript");
			
			PrintStream ps = ctx.getResponse().getPrintStream();

			ps.println("dc.pui.Loader.addPageDefinition('" + ctx.getRequest().getOriginalPath() + "', {");
			
			if (src.hasAttribute("Title")) {
				String title = ctx.getInternalParam("PageTitle"); // ctx.expandMacros(src.getAttribute("Title"));
				
				ps.print("\tTitle: '");
				Node.writeDynamicJsString(ps, title);
				ps.println("',");
			}
			
			if (this.frag.hasAttribute("class")) {
				String pclass = this.frag.getAttribute("class"); 
				
				ps.print("\tPageClass: '");
				Node.writeDynamicJsString(ps, pclass);
				ps.println("',");
			}
			
			ps.println("\tLayout: [");

			this.frag.writeDynamicChildren(ps, "");
			
			ps.println();
			ps.println("\t],");
			
			// ==============================================
			//  Styles
			// ==============================================
			
			boolean first = true;
			
			ps.print("\tRequireStyles: [");
			
			for (XElement func : src.selectAll("RequireStyle")) {
				if (!func.hasAttribute("Path"))
					continue;
				
				if (first)
					first = false;
				else
					ps.print(",");
				
				ps.print(" '");				
				Node.writeDynamicJsString(ps, func.getAttribute("Path"));				
				ps.print("'");
			}
			
			ps.println(" ], ");
			
			first = true;
			
			// ==============================================
			//  Libs
			// ==============================================
			
			ps.print("\tRequireLibs: [");
			
			for (XElement func : src.selectAll("RequireLib")) {
				if (!func.hasAttribute("Path"))
					continue;
				
				if (first)
					first = false;
				else
					ps.print(",");
				
				ps.print(" '");				
				Node.writeDynamicJsString(ps, func.getAttribute("Path"));				
				ps.print("'");
			}
			
			ps.println(" ], ");
			
			// ==============================================
			//  Functions
			// ==============================================
			
			first = true;
			
			ps.println("\tFunctions: {");
			
			for (XElement func : src.selectAll("Function")) {
				if (!func.hasAttribute("Name"))
					continue;
				
				if (first)
					first = false;
				else
					ps.println(",");
				
				ps.print("\t\t" + func.getAttribute("Name") + ": function(" + func.getAttribute("Params", "") + ") { \n");
				
				ps.print(func.getText());
				
				ps.print("\n\t\t}");
			}
			
			ps.println();
			
			ps.println("\t}, ");
			
			// ==============================================
			//  Load Functions
			// ==============================================
			
			first = true;
			
			ps.println("\tLoadFunctions: [");
			
			for (XElement func : src.selectAll("Function")) {
				if (!func.hasAttribute("Name") || !"Load".equals(func.getAttribute("Mode")))
					continue;
				
				if (first)
					first = false;
				else
					ps.println(",");
				
				ps.print("\t\t function(" + func.getAttribute("Params", "") + ") {");
				
				ps.print("\t\t\t // func " + func.getAttribute("Name") + "\n");
				
				ps.print(func.getText());
				
				ps.print("\n\t\t}");
			}
			
			ps.println();
			
			ps.println("\t]");
			
			ps.println("});");
			
			ps.println();
			
			ps.println("dc.pui.Loader.resumePageLoad();");			
		}
		else {
			ctx.getResponse().setHeader("Content-Type", "text/html; charset=utf-8");
			ctx.getResponse().setHeader("X-UA-Compatible", "IE=Edge,chrome=1");
			
	    	ctx.getResponse().getPrintStream().println("<!DOCTYPE html>");        
	    	
	    	this.frag.write(ctx);
		}
		
		ctx.send();
		
		//System.out.println("b " + System.currentTimeMillis());		
    }

    /*
	public static String streamNodes(String indent, List<Node> children, boolean cleanWhitespace) {
        if (children.size() == 0) 
        	return null;
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(os);
        
        boolean lastblock = true;
        boolean firstch = true;   

        for (Node node : children) {
            node.stream(out, indent, (firstch || lastblock), true);
            
            lastblock = node.getBlockIndent();
            firstch = false;
        }
        
        out.flush();
        
        if (cleanWhitespace)
        	return StringUtil.stripWhitespacePerXml(os.toString());
        
        return os.toString();
	} */
}
