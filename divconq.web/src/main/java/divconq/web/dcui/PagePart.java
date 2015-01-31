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
import java.lang.reflect.Method;

import org.markdown4j.Markdown4jProcessor;

import w3.html.Img;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.locale.LocaleInfo;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

public class PagePart extends Element implements ICodeTag {
    protected String id = null;
    protected String content = null;
    protected XElement src = null;
    
	@Override
	public Node deepCopy(Element parent) {
		PagePart cp = new PagePart();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	protected void doCopy(Node n) {
		super.doCopy(n);
		
		((PagePart)n).id = this.id;
		((PagePart)n).content = this.content;
		((PagePart)n).src = this.src;
	}

	@Override
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
    	this.id = xel.getRawAttribute("id");
    	this.content = xel.getRawAttribute("Content");
		
    	this.src = view.getSource();
    	
		nodes.add(this);
	}
	
	@Override
	public void doBuild() {
		ViewOutputAdapter view = (ViewOutputAdapter) this.getContext().getAdapter();
		
		XElement ppel = null;
		
		LocaleInfo li = this.getContext().getLocale();
		String lname = li.getName();
		
		for (XElement pel : this.src.selectAll("PagePart")) {
			if (this.id.equals(pel.getAttribute("For")) && (lname.equals(pel.getAttribute("Locale"))
					|| "default".equals(pel.getAttribute("Locale")) || !pel.hasAttribute("Locale"))) {
				ppel = pel;
				break;
			}
		}
		
		if (ppel == null) {
			lname = li.getLanguage();
			
			for (XElement pel : this.src.selectAll("PagePart")) {
				if (this.id.equals(pel.getAttribute("For")) && lname.equals(pel.getAttribute("Locale"))) {
					ppel = pel;
					break;
				}
			}
		}
		
		if (ppel == null) {
			li = this.getContext().getDomain().getDefaultLocaleInfo();
			
			lname = li.getName();
			
			for (XElement pel : this.src.selectAll("PagePart")) {
				if (this.id.equals(pel.getAttribute("For")) && lname.equals(pel.getAttribute("Locale"))) {
					ppel = pel;
					break;
				}
			}
			
			if (ppel == null) {
				lname = li.getLanguage();
				
				for (XElement pel : this.src.selectAll("PagePart")) {
					if (this.id.equals(pel.getAttribute("For")) && lname.equals(pel.getAttribute("Locale"))) {
						ppel = pel;
						break;
					}
				}
			}
		}
		
		if (ppel != null) {
			Nodes nl = null;
			
			if ("pre".equals(ppel.getAttribute("Format"))) {
				this.name = "pre"; 
				nl = view.getDomain().parseXml(view, ppel);
			}
			else if ("html".equals(ppel.getAttribute("Format"))) {
				this.name = "div"; 
				nl = view.getDomain().parseXml(view, ppel);
			}
			else if ("literal".equals(ppel.getAttribute("Format"))) {
				this.name = "div";
				
				FuncResult<XElement> pres = XmlReader.parse("<div>" + ppel.getText() + "</div>", true);
				
				// TODO error message
				
				if (pres.isNotEmptyResult())
					nl = view.getDomain().parseXml(view, pres.getResult());
			}
			else if ("md".equals(ppel.getAttribute("Format"))) {
				this.name = "div"; 
				
				String html = null;
				
				System.out.println("md: " + ppel.getText());
				
				try {
					html = new Markdown4jProcessor().process(ppel.getText());
				} 
				catch (IOException x) {
					System.out.println("error: " + x);
				}
				
				System.out.println("html: " + html);
				
				nl = new Nodes();
				nl.add(new UnescapedText(true, html));
				
				//nl = view.getDomain().parseXml(view, ppel);
			}
			else if ("image".equals(ppel.getAttribute("Format"))) {
				this.name = "div"; 
				
				System.out.println("image: " + ppel.getText());
				
				nl = new Nodes();
				//nl.add(new Img(new Attributes("src", "/galleries/" 
				//		+ ppel.getText() + ".v/" + ppel.getAttribute("Variation") + ".jpg")));
				nl.add(new Img(new Attributes("src", "/galleries/" 
						+ ppel.getText())));
			}
			else if ("groovy".equals(ppel.getAttribute("Format"))) {
				this.name = "div"; 
				
				System.out.println("script: " + ppel.getText());
				
				try (GroovyClassLoader loader = new GroovyClassLoader()) {
					Class<?> groovyClass = loader.parseClass(ppel.getText());
					Method runmeth = null;
					
					for (Method m : groovyClass.getMethods()) {
						if (!m.getName().startsWith("run"))
							continue;
						
						runmeth = m;
						break;
					}
					
					if (runmeth != null) {
				    	FutureNodes future = new FutureNodes();
						
						GroovyObject groovyObject = (GroovyObject) groovyClass.newInstance();
						Object[] args2 = { this.getContext(), future };
						
						groovyObject.invokeMethod("run", args2);
						
						nl = future;
					}
				}
				catch (Exception x) {
					OperationContext.get().error("Unable to execute script!");
				}
			}
			
			this.myArguments = new Object[] { new Attributes("id", this.id), nl };
		}
		
        super.doBuild();
	}
}
