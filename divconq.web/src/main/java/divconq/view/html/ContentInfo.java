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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import divconq.view.ContentPlaceholder;
import divconq.view.Fragment;
import divconq.view.Nodes;
import divconq.web.IContentBuilder;
import divconq.web.IContentInfo;
import divconq.web.IWebDomain;
import divconq.web.ViewInfo;
import divconq.web.WebContext;
import divconq.xml.XElement;

import w3.html.Script;
import w3.html.Style;

public class ContentInfo implements IContentInfo {
	protected Nodes contenttemplate = null; 
	protected Class<? extends IContentBuilder> contentbuilder = null;
	
	protected Map<String,Nodes> templatemap = new HashMap<String,Nodes>(); 
	protected Map<String,Class<? extends IContentBuilder>> templatebuilder = new HashMap<String, Class<? extends IContentBuilder>>();
	
	protected List<Style> styletemplate = new ArrayList<Style>();
	protected List<Script> scripttemplate = new ArrayList<Script>();
	
	protected ViewInfo info = null;

	public void load(ViewInfo info, XElement el) {
		this.info = info;
		
		if (el.hasAttribute("ContentClass")) {
			try {
				Class<?> cls = info.getDomain().getExtension().getBundle().getClass(el.getAttribute("ContentClass"));
				
				// TODO improve tracing
				if (cls != null) 
					this.contentbuilder = cls.asSubclass(IContentBuilder.class);
			}
			catch (Exception x) {
				// TODO improve tracing
				System.out.println("could not load class: " + el.getAttribute("ContentClass"));
			}
		}
		
		IWebDomain domain = info.getDomain();
		//IViewParser parser = domain.getFormatParser("HtmlOutput");
		
		// TODO log
		//if (parser == null)
		//	return;
		
		XElement cel = el.find("Content");
		
		if (cel != null)
			this.contenttemplate = domain.parseXml("HtmlOutput", info, cel);		
		
		for (XElement stel : el.selectAll("Style")) 		
			this.styletemplate.add((Style)domain.parseElement("HtmlOutput", info, stel).getFirst());
		
		for (XElement scel : el.selectAll("Script")) 		
			this.scripttemplate.add((Script)domain.parseElement("HtmlOutput", info, scel).getFirst());
		
		for (XElement tmpel : el.selectAll("Template")) {
			if (!tmpel.hasAttribute("Id"))
				continue;
			
			String tid = tmpel.getAttribute("Id");
			
			try {
				if (tmpel.hasAttribute("ContentClass")) {
					try {
						Class<?> cls = domain.getExtension().getBundle().getClass(tmpel.getAttribute("ContentClass"));
						
						// TODO improve tracing
						if (cls != null) 
							this.templatebuilder.put(tid, cls.asSubclass(IContentBuilder.class)); 
					}
					catch (Exception x) {
						// TODO improve tracing
						System.out.println("could not load class: " + el.getAttribute("ContentClass"));
					}
				}
				else
					this.templatemap.put(tid, domain.parseXml("HtmlOutput", info, tmpel));
			}
			catch (Exception x) {
				// TODO trace
			}
		}
	}
	
	public Nodes getOutput(Fragment frag, WebContext ctx) {
		try {			
			if (this.styletemplate.size() > 0) {
			    	ContentPlaceholder ph = ctx.getHolder("Styles");
			    	
		    		for (Style nds : this.styletemplate) {
		    			Style cds = (Style)nds.deepCopy(ph);
		    			cds.setPartRoot(frag);
		    			ph.addChildren(cds);
		    		}
			}
			
			if (this.scripttemplate.size() > 0) {
		    	ContentPlaceholder ph = ctx.getHolder("Scripts");
		    	
	    		for (Script nds : this.scripttemplate) {
	    			Script cds = (Script)nds.deepCopy(ph);
	    			cds.setPartRoot(frag);
	    			ph.addChildren(cds);
	    		}
			}
		
			if (this.contentbuilder != null)
				return this.contentbuilder.newInstance().getContent(ctx, this.info, frag);
			
			if (this.contenttemplate != null)
				return this.contenttemplate.deepCopy();		
		} 
		catch (Exception x) {
			// TODO Auto-generated catch block
		}
		
		return null;
	}

	/*
	public Nodes getTemplateOutput(String name, Element parent, Map<String, String> params) {
		try {
			Nodes tmpl = null;
			
			if (this.templatebuilder.containsKey(name))
				tmpl = this.templatebuilder.get(name).newInstance().getContent(null, this.info, parent);			
			else if (this.templatemap.containsKey(name))
				tmpl = this.templatemap.get(name).deepCopy();
			
			for (Node n : tmpl.getList()) {
				n.setParams(params);
			}
			
			return tmpl;
		} 
		catch (Exception x) {
			// TODO Auto-generated catch block
		}
		
		return null;
	}
	*/
}
