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
import java.nio.file.Path;

import org.markdown4j.Markdown4jProcessor;

import w3.html.Img;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.locale.LocaleInfo;
import divconq.struct.Struct;
import divconq.util.IOUtil;
import divconq.web.WebContext;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

public class PagePart extends Element implements ICodeTag {
    protected String id = null;

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
    	this.id = xel.getRawAttribute("id");
    	
        this.myArguments = new Object[] { attrs };
    	
		nodes.add(this);
	}
	
	@Override
	public void doBuild(WebContext ctx) {
		XElement ppel = null;
		
		LocaleInfo li = ctx.getLocale();
		String lname = li.getName();
		
		XElement src = this.getViewRoot().getSource();
		
		for (XElement pel : src.selectAll("PagePart")) {
			if (this.id.equals(pel.getAttribute("For")) && (lname.equals(pel.getAttribute("Locale"))
					|| "default".equals(pel.getAttribute("Locale")) || "true".equals(pel.getAttribute("ForceLocale")) || !pel.hasAttribute("Locale"))) {
				ppel = pel;
				break;
			}
		}
		
		if (ppel == null) {
			lname = li.getLanguage();
			
			for (XElement pel : src.selectAll("PagePart")) {
				if (this.id.equals(pel.getAttribute("For")) && lname.equals(pel.getAttribute("Locale"))) {
					ppel = pel;
					break;
				}
			}
		}
		
		if (ppel == null) {
			li = ctx.getDomain().getDefaultLocaleInfo();
			
			lname = li.getName();
			
			for (XElement pel : src.selectAll("PagePart")) {
				if (this.id.equals(pel.getAttribute("For")) && lname.equals(pel.getAttribute("Locale"))) {
					ppel = pel;
					break;
				}
			}
			
			if (ppel == null) {
				lname = li.getLanguage();
				
				for (XElement pel : src.selectAll("PagePart")) {
					if (this.id.equals(pel.getAttribute("For")) && lname.equals(pel.getAttribute("Locale"))) {
						ppel = pel;
						break;
					}
				}
			}
		}
		
		Attributes attrs = new Attributes("id", this.id);
		
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
			
			if ("pre".equals(ppel.getAttribute("Format"))) {
				this.name = "pre"; 
				nl = ctx.getDomain().parseXml(ctx, ppel);
			}
			else if ("html".equals(ppel.getAttribute("Format"))) {
				this.name = "div"; 
				nl = ctx.getDomain().parseXml(ctx, ppel);
			}
			else if ("literal".equals(ppel.getAttribute("Format"))) {
				this.name = "div";
				
				FuncResult<XElement> pres = XmlReader.parse("<div>" + ppcontent + "</div>", true);
				
				// TODO error message
				
				if (pres.isNotEmptyResult())
					nl = ctx.getDomain().parseXml(ctx, pres.getResult());
			}
			else if ("md".equals(ppel.getAttribute("Format"))) {
				this.name = "div"; 
				attrs.add("data-dcui-mode", "enhance");
				
				String html = null;
				
				//System.out.println("md: " + ppcontent);
				
				try {
					html = new Markdown4jProcessor().process(ppcontent.toString());
				} 
				catch (IOException x) {
					System.out.println("error: " + x);
				}
				
				//System.out.println("html: " + html);
				
				nl = new Nodes();
				nl.add(new LiteralText(html));
				
				//nl = builder.getContext().getDomain().parseXml(view, ppel);
			}
			else if ("image".equals(ppel.getAttribute("Format"))) {
				this.name = "div"; 
				
				//System.out.println("image: " + ppcontent);
				
				nl = new Nodes();

				nl.add(new Img(new Attributes("src", ppcontent.charAt(0) == '/' 
						? "/galleries" + ppcontent
						: "/galleries/" + ppcontent
				)));
			}
			else if ("groovy".equals(ppel.getAttribute("Format"))) {
				this.name = "div"; 
				
				//System.out.println("script: " + ppcontent);
				
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
			
			this.myArguments = new Object[] { this.myArguments, attrs, nl };
		}
		
        super.doBuild(ctx);
	}
}
