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
package divconq.web;

import divconq.util.StringUtil;
import divconq.view.Fragment;
import divconq.view.Nodes;
import divconq.xml.XElement;

public class ViewInfo {
	protected XElement source = null;
	protected IContentInfo output = null;	
	protected IWebDomain domain = null;
	protected Class<? extends IViewBuilder> builder = null;
	
	public XElement getSource() {
		return this.source;
	}
	
	public IWebDomain getDomain() {
		return this.domain;
	}
	
	public Nodes getForOutput(WebContext ctx, Fragment parent) throws Exception {
		if (this.output == null)
			return null;
		
		return this.output.getOutput(parent, ctx);
	}
	
	/*
	public Nodes getTemplateForOutput(String name, Element parent, String... params) {
		if (this.output == null)
			return null;
		
		Map<String, String> tmplparams = new HashMap<String, String>();
		
		for (int i = 1; i < params.length; i += 2) 
			tmplparams.put(params[i-1], params[i]);
		
		return this.output.getTemplateOutput(name, parent, tmplparams);
	}
	*/
	
	public ViewInfo(IWebDomain domain) {
		this.domain = domain;
	}
	
	public boolean load(XElement src) {
		this.source = src;
		
		if (src == null) {
			// TODO improve tracing
			System.out.println("could not load view info!");
			return false;
		}
		
		if (src.hasAttribute("ViewClass")) {
			try {
				Class<?> cls = this.domain.getExtension().getBundle().getClass(src.getAttribute("ViewClass"));
				
				// TODO improve tracing
				if (cls != null) 
					this.builder = cls.asSubclass(IViewBuilder.class);
			}
			catch (Exception x) {
				// TODO improve tracing
				System.out.println("could not load class: " + src.getAttribute("ViewClass"));
			}
		}
		else {
			String fmt = src.getName();
			
			if (StringUtil.isNotEmpty(fmt)) {
				
				Class<? extends IContentInfo> bc = this.domain.getContentLoader(fmt);
				
				if (bc != null)
					try {
						IContentInfo cinfo =  bc.newInstance();
						
						if (cinfo != null) {
							cinfo.load(this, src);
							this.output = cinfo;							
							this.builder = this.domain.getBuilder(fmt);
						}
					} 
					catch (Exception x) {
						// TODO log
						System.out.println("unable to create instance of view builder for: " + fmt);
					}
			}
		}
		
		return (this.builder != null);
	}

	public IViewBuilder getBuilder() throws Exception {
		if (this.builder == null)
			return null;
		
		IViewBuilder vb = this.builder.newInstance();
		vb.setViewInfo(this);
		return vb;
	}
}
