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

import java.io.PrintStream;
import java.nio.file.Path;

import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.locale.LocaleDefinition;
import divconq.struct.Struct;
import divconq.util.IOUtil;
import divconq.web.WebContext;
import divconq.xml.XElement;

public class TextPart extends Element implements ICodeTag {
    protected String id = null;

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
    	this.id = xel.getRawAttribute("id");
    	
		nodes.add(this);
	}
	
	@Override
	public void doBuild(WebContext ctx) {
		XElement ppel = null;
		
		LocaleDefinition li = OperationContext.get().getWorkingLocaleDefinition();
		String lname = li.getName();
		
		XElement src = this.getViewRoot().getSource();
		
		for (XElement pel : src.selectAll("TextPart")) {
			if (this.id.equals(pel.getAttribute("For")) && (lname.equals(pel.getAttribute("Locale"))
					|| "default".equals(pel.getAttribute("Locale")) || !pel.hasAttribute("Locale"))) {
				ppel = pel;
				break;
			}
		}
		
		if (ppel == null) {
			lname = li.getLanguage();
			
			for (XElement pel : src.selectAll("TextPart")) {
				if (this.id.equals(pel.getAttribute("For")) && lname.equals(pel.getAttribute("Locale"))) {
					ppel = pel;
					break;
				}
			}
		}
		
		if (ppel == null) {
			li = ctx.getDomain().getDomainInfo().getDefaultLocaleDefinition();
			
			lname = li.getName();
			
			for (XElement pel : src.selectAll("TextPart")) {
				if (this.id.equals(pel.getAttribute("For")) && lname.equals(pel.getAttribute("Locale"))) {
					ppel = pel;
					break;
				}
			}
			
			if (ppel == null) {
				lname = li.getLanguage();
				
				for (XElement pel : src.selectAll("TextPart")) {
					if (this.id.equals(pel.getAttribute("For")) && lname.equals(pel.getAttribute("Locale"))) {
						ppel = pel;
						break;
					}
				}
			}
		}
		
		if (ppel != null) {
			Nodes nl = null;
			
			CharSequence ppcontent = ppel.getText();
			
			if (Struct.objectToBooleanOrFalse(ppel.getAttribute("External"))) {
				String sname = ppel.getAttribute("Source");
				
				Path srcpath = OperationContext.get().getDomain().resolvePath(sname);
				
				sname = srcpath.getFileName().toString();
				
				int pos = sname.indexOf('.');
				sname = sname.substring(0, pos) + "." + this.id + "." + ppel.getAttribute("Format");
				
				FuncResult<CharSequence> mres = IOUtil.readEntireFile(srcpath.resolveSibling(sname));
				
				if (mres.isNotEmptyResult()) 
					ppcontent = mres.getResult();
			}
			
			if ("literal".equals(ppel.getAttribute("Format"))) {
				nl = new Nodes(new LiteralText(ppcontent.toString()));
			}
			else if ("md".equals(ppel.getAttribute("Format"))) {
				nl = new Nodes(new LiteralText(ppcontent.toString()));
			}
			else if ("groovy".equals(ppel.getAttribute("Format"))) {
				//System.out.println("script: " + ppel.getText());
				
				String methname = "run";
				GroovyObject groovyObject = null;
				
				if (ppel.hasAttribute("ScriptMethod")) {
					methname = ppel.getAttribute("ScriptMethod");
					groovyObject = ctx.getServerScript();
				}
				else {
					try (GroovyClassLoader loader = new GroovyClassLoader()) {
						Class<?> groovyClass = loader.parseClass(ppcontent.toString());
						groovyObject = (GroovyObject) groovyClass.newInstance();
					}
					catch (Exception x) {
						OperationContext.get().error("Unable to compile or create script!");
						OperationContext.get().error("Error: " + x);
					}
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
		}
		
        super.doBuild(ctx);
	}

	/*
	public void write(WebContext ctx) throws IOException {
		this.stream(ctx, ctx.getResponse().getPrintStream(), "", false, true);
	}
	*/

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

}
