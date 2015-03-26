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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import divconq.hub.DomainInfo;
import divconq.hub.Hub;
import divconq.lang.op.OperationCallback;
import divconq.lang.op.OperationResult;
import divconq.util.StringUtil;
import divconq.web.WebContext;
import divconq.xml.XElement;

public class ViewBuilder extends Fragment implements IViewExecutor {
	protected WebContext context = null;
	protected boolean dynamic = false;
	
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
			Nodes content = this.view.getOutput(this, this.context, this.dynamic);  
	
			if (this.context.isCompleted())
				return;
		
			this.build(content);
		} 
		catch (Exception x) {
			// TODO 
			System.out.println("View builder build error: " + x);
		}  
    }

	@Override
    public void write() throws IOException {
		if (this.dynamic) {
			PrintStream ps = this.context.getResponse().getPrintStream();

			ps.println("dc.pui.Loader.addPageDefinition('" + this.context.getRequest().getOriginalPath() + "', {");
			
			if (this.view.source.hasAttribute("Title")) {
				ps.print("\tTitle: '");
				Node.writeDynamicJsString(ps, this.view.source.getAttribute("Title"));
				ps.println("',");
			}
			
			ps.println("\tLayout: [");

			this.writeDynamicChildren(ps, "");
			
			ps.println();
			ps.println("\t],");
			
			// ==============================================
			//  Styles
			// ==============================================
			
			boolean first = true;
			
			ps.print("\tRequireStyles: [");
			
			for (XElement func : this.view.source.selectAll("RequireStyle")) {
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
			
			for (XElement func : this.view.getStyles()) {
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
			
			for (XElement func : this.context.getStyles()) {
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
			
			for (XElement func : this.view.source.selectAll("RequireLib")) {
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
			
			for (XElement func : this.view.getLibs()) {
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
			
			for (XElement func : this.context.getLibs()) {
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
			
			for (XElement func : this.view.source.selectAll("Function")) {
				if (!func.hasAttribute("Name"))
					continue;
				
				if (first)
					first = false;
				else
					ps.println(",");
				
				ps.print("\t\t" + func.getAttribute("Name") + ": function(" + func.getAttribute("Params", "") + ") {");
				
				ps.print(func.getText());
				
				ps.print("\t\t}");
			}
			
			for (XElement func : this.view.getFunctions()) {
				if (!func.hasAttribute("Name") || "Load".equals(func.getAttribute("Mode")))
					continue;
				
				if (first)
					first = false;
				else
					ps.println(",");
				
				ps.print("\t\t" + func.getAttribute("Name") + ": function(" + func.getAttribute("Params", "") + ") {");
				
				ps.print(func.getText());
				
				ps.print("\t\t}");
			}
			
			for (XElement func : this.context.getFunctions()) {
				if (!func.hasAttribute("Name") || "Load".equals(func.getAttribute("Mode")))
					continue;
				
				if (first)
					first = false;
				else
					ps.println(",");
				
				ps.print("\t\t" + func.getAttribute("Name") + ": function(" + func.getAttribute("Params", "") + ") {");
				
				ps.print(func.getText());
				
				ps.print("\t\t}");
			}
			
			ps.println();
			
			ps.println("\t}, ");
			
			// ==============================================
			//  Load Functions
			// ==============================================
			
			first = true;
			
			ps.println("\tLoadFunctions: [");
			
			for (XElement func : this.context.getFunctions()) {
				if (!func.hasAttribute("Name") || !"Load".equals(func.getAttribute("Mode")))
					continue;
				
				if (first)
					first = false;
				else
					ps.println(",");
				
				ps.print("\t\t function(" + func.getAttribute("Params", "") + ") {");
				
				ps.print("\t\t\t // func " + func.getAttribute("Name"));
				
				ps.print(func.getText());
				
				ps.print("\t\t}");
			}
			
			ps.println();
			
			ps.println("\t]");
			
			ps.println("});");
			
			ps.println();
			
			ps.println("dc.pui.Loader.resumePageLoad();");			
		}
		else {
	    	this.context.getResponse().getPrintStream().println("<!DOCTYPE html>");        
	        super.write();
		}
    }

	@Override
	public OperationResult execute(WebContext ctx) throws Exception {
		OperationResult or = new OperationResult();
		
		this.context = ctx;
		
		String mode = ctx.getExternalParam("_dcui");

		if ("dyn".equals(mode) || "dyn".equals(mode)) {
			this.dynamic = true;
			this.context.getResponse().setHeader("Content-Type", "application/javascript");
		}
		else {
			this.context.getResponse().setHeader("Content-Type", "text/html; charset=utf-8");
			this.context.getResponse().setHeader("X-UA-Compatible", "IE=Edge,chrome=1");
		}
		
		this.doBuild();
		
		if (this.context.isCompleted()) {
			this.context.send();
			return or;
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
		
		return or;
	}

	@Override
	public WebContext getContext() {
		return this.context;
	}

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
