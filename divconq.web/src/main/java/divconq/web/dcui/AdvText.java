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

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.io.IOException;
import java.io.PrintStream;

import divconq.lang.op.OperationContext;
import divconq.web.WebContext;
import divconq.web.md.Markdown;
import divconq.xml.XElement;

public class AdvText extends Element implements ICodeTag {
    protected String format = null;
    protected String content = null;
    
    public void setFormat(String v) {
		this.format = v;
	}
    
    public String getFormat() {
		return this.format;
	}
    
    public void setContent(String v) {
		this.content = v;
	}
    
    public String getContent() {
		return this.content;
	}
    
    public AdvText() {
    }
    
    public AdvText(String content) {
    	this.format = "md";
    	this.content = content;
    }
    
    public AdvText(String format, String content) {
    	this.format = format;
    	this.content = content;
    }

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
    	this.format = xel.getAttribute("Format", "md");
    	this.content = xel.getText();
    	
		nodes.add(this);
	}
	
	@Override
	public void doBuild(WebContext ctx) {
		Nodes nl = null;
		
		if ("literal".equals(this.format)) {
			nl = new Nodes(new LiteralText(this.content.toString()));
		}
		else if ("md".equals(this.format)) {
			// TODO allocate from webdomain
			Markdown mdp = new Markdown();

			try {
				nl = mdp.process(ctx, this.content.toString());
			} 
			catch (IOException x) {
				System.out.println("inline md error: " + x);
			}
		}
		else if ("groovy".equals(this.format)) {
			//System.out.println("script: " + ppel.getText());
			
			String methname = "run";
			GroovyObject groovyObject = null;
			
			try (GroovyClassLoader loader = new GroovyClassLoader()) {
				Class<?> groovyClass = loader.parseClass(this.content.toString());
				groovyObject = (GroovyObject) groovyClass.newInstance();
			}
			catch (Exception x) {
				OperationContext.get().error("Unable to compile or create script!");
				OperationContext.get().error("Error: " + x);
			}
			
			try {
				if (groovyObject != null) {
			    	FutureNodes future = new FutureNodes();
					
					Object[] args2 = { ctx, future };
					
					groovyObject.invokeMethod(methname, args2);
					
					nl = future;
				}
			}
			catch (Exception x) {
				OperationContext.get().error("Unable to execute script!");
				OperationContext.get().error("Error: " + x);
			}
		}
		
		this.myArguments = new Object[] { this.myArguments, nl };
		
        super.doBuild(ctx);
	}

	@Override
	public void stream(WebContext ctx, PrintStream strm, String indent, boolean firstchild, boolean fromblock) {
		if (this.children.size() == 0)
			return;

		boolean fromon = fromblock;
		boolean lastblock = false;
		boolean firstch = this.getBlockIndent(); // only true once, and only if
													// bi

		for (Node node : this.children) {
			if (node.getBlockIndent() && !lastblock && !fromon)
				this.print(ctx, strm, "", true, "");

			node.stream(ctx, strm, indent, (firstch || lastblock), this.getBlockIndent());

			lastblock = node.getBlockIndent();
			firstch = false;
			fromon = false;
		}
	}
	
	@Override
	public boolean writeDynamic(PrintStream buffer, String tabs, boolean first) {
        if (this.children.size() == 0) 
        	return false;
        
		for (Node child : this.children) {
			if (child.writeDynamic(buffer, tabs, first)) 
				first = false;
		}
		
		return true;
	}

}
