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
package divconq.view.html;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import divconq.lang.OperationCallback;
import divconq.util.StringUtil;
import divconq.view.Fragment;
import divconq.view.Node;
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
    }

	@Override
    public void write() throws IOException {
    	this.context.getResponse().getPrintStream().println("<!doctype html>");        
        super.write();
    }

	@Override
	public void execute(WebContext ctx) throws Exception {
		this.context = ctx;
		
		this.context.getResponse().setHeader("Content-Type", "text/html; charset=utf-8");
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
}
