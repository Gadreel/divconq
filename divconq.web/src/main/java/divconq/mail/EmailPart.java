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

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;

import divconq.lang.op.OperationContext;
import divconq.locale.LocaleInfo;
import divconq.web.dcui.Element;
import divconq.web.dcui.FutureNodes;
import divconq.web.dcui.ICodeTag;
import divconq.web.dcui.LiteralText;
import divconq.web.dcui.Node;
import divconq.web.dcui.Nodes;
import divconq.web.dcui.ViewOutputAdapter;
import divconq.xml.XElement;

public class EmailPart extends Element implements ICodeTag {
    protected String id = null;
    protected String content = null;
    protected XElement src = null;
    
	@Override
	public Node deepCopy(Element parent) {
		EmailPart cp = new EmailPart();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	protected void doCopy(Node n) {
		super.doCopy(n);
		
		((EmailPart)n).id = this.id;
		((EmailPart)n).content = this.content;
		((EmailPart)n).src = this.src;
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
		XElement ppel = null;
		
		LocaleInfo li = this.getContext().getLocale();
		String lname = li.getName();
		
		for (XElement pel : this.src.selectAll("EmailPart")) {
			if (this.id.equals(pel.getAttribute("For")) && (lname.equals(pel.getAttribute("Locale"))
					|| "default".equals(pel.getAttribute("Locale")) || !pel.hasAttribute("Locale"))) {
				ppel = pel;
				break;
			}
		}
		
		if (ppel == null) {
			lname = li.getLanguage();
			
			for (XElement pel : this.src.selectAll("EmailPart")) {
				if (this.id.equals(pel.getAttribute("For")) && lname.equals(pel.getAttribute("Locale"))) {
					ppel = pel;
					break;
				}
			}
		}
		
		if (ppel == null) {
			li = this.getContext().getDomain().getDefaultLocaleInfo();
			
			lname = li.getName();
			
			for (XElement pel : this.src.selectAll("EmailPart")) {
				if (this.id.equals(pel.getAttribute("For")) && lname.equals(pel.getAttribute("Locale"))) {
					ppel = pel;
					break;
				}
			}
			
			if (ppel == null) {
				lname = li.getLanguage();
				
				for (XElement pel : this.src.selectAll("EmailPart")) {
					if (this.id.equals(pel.getAttribute("For")) && lname.equals(pel.getAttribute("Locale"))) {
						ppel = pel;
						break;
					}
				}
			}
		}
		
		if (ppel != null) {
			Nodes nl = null;
			
			if ("literal".equals(ppel.getAttribute("Format"))) {
				nl = new Nodes(new LiteralText(ppel.getText()));
			}
			else if ("md".equals(ppel.getAttribute("Format"))) {
				nl = new Nodes(new LiteralText(ppel.getText()));
			}
			else if ("groovy".equals(ppel.getAttribute("Format"))) {
				//System.out.println("script: " + ppel.getText());
				
				String methname = "run";
				GroovyObject groovyObject = null;
				
				if (ppel.hasAttribute("ScriptMethod")) {
					methname = ppel.getAttribute("ScriptMethod");
					groovyObject = this.getViewRoot().getView().viewloader;
				}
				else {
					try (GroovyClassLoader loader = new GroovyClassLoader()) {
						Class<?> groovyClass = loader.parseClass(ppel.getText());
						
						for (Method m : groovyClass.getMethods()) {
							if (!m.getName().startsWith("run"))
								continue;
							
							groovyObject = (GroovyObject) groovyClass.newInstance();
							break;
						}
					}
					catch (Exception x) {
						OperationContext.get().error("Unable to execute script!");
						OperationContext.get().error("Error: " + x);
					}
				}
				
				if (groovyObject != null) {
			    	FutureNodes future = new FutureNodes();
					
					Object[] args2 = { this.getContext(), future };
					
					groovyObject.invokeMethod(methname, args2);
					
					nl = future;
				}
			}
			
			this.myArguments = new Object[] { this.myArguments, nl };
		}
		
        super.doBuild();
	}
	

	public void write() throws IOException {
		this.stream(this.getContext().getResponse().getPrintStream(), "", false, true);
	}

	@Override
	public void stream(PrintStream strm, String indent, boolean firstchild, boolean fromblock) {
		if (this.children.size() == 0)
			return;

		boolean fromon = fromblock;
		boolean lastblock = false;
		boolean firstch = this.getBlockIndent(); // only true once, and only if
													// bi

		for (Node node : this.children) {
			if (node.getBlockIndent() && !lastblock && !fromon)
				this.print(strm, "", true, "");

			node.stream(strm, indent, (firstch || lastblock), this.getBlockIndent());

			lastblock = node.getBlockIndent();
			firstch = false;
			fromon = false;
		}
	}

}
