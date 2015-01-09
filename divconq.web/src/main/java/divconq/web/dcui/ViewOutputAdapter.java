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

import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.PrintStream;
import java.nio.file.Path;

import divconq.filestore.CommonPath;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.util.IOUtil;
import divconq.web.IOutputAdapter;
import divconq.web.IWebDomain;
import divconq.web.IWebExtension;
import divconq.web.WebContext;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

public class ViewOutputAdapter implements IOutputAdapter  {
	protected XElement source = null;
	protected IWebDomain domain = null;
	protected CommonPath webpath = null;
	//protected Path filepath = null;
	protected String[] auth = null;

	// content info
	protected Nodes pagetemplate = null; 
	public Nodes contenttemplate = null;
	protected Class<? extends IContentBuilder> pagebuilder = null;
	//protected List<Style> styletemplate = new ArrayList<Style>();
	//protected List<Script> scripttemplate = new ArrayList<Script>();
	
	// content building
	protected Class<? extends IViewExecutor> adapter = null;
	
	public XElement getSource() {
		return this.source;
	}
	
	public IWebDomain getDomain() {
		return this.domain;
	}
	
	public ViewOutputAdapter(IWebDomain domain, CommonPath webpath, Path filepath, IWebExtension ext) {
		this.domain = domain;
		this.webpath = webpath;
		//this.filepath = filepath;
		
		FuncResult<CharSequence> rres = IOUtil.readEntireFile(filepath);
		
		if (rres.hasErrors()) { 
			System.out.println("Error reading view: " + rres.getMessages());
			throw new IllegalArgumentException("Bad file path: cannot read");
		}
		
		String content = rres.getResult().toString();

		FuncResult<XElement> xres = XmlReader.parse(content, true);
		
		if (xres.hasErrors()) {
			System.out.println("Error parsing view: " + xres.getMessages());
			throw new IllegalArgumentException("Bad file path: cannot parse");
		}
		
		this.adapter = ViewBuilder.class;
		
		this.source = xres.getResult();
		
		if (this.source == null) {
			this.source = new XElement("dcui",
					new XElement("Layout", 
							new XElement("h1", "Parse Error!!")
					)
			);
		}
		
		if (this.source.hasAttribute("AuthTags"))
			this.auth = this.source.getAttribute("AuthTags").split("\\|");

		if (this.source.hasAttribute("ViewClass")) {
			try {
				Class<?> cls = ext.getBundle().getClass(this.source.getAttribute("ViewClass"));
				
				// TODO improve tracing
				if (cls != null) 
					this.adapter = cls.asSubclass(IViewExecutor.class);
			}
			catch (Exception x) {
				// TODO improve tracing
				System.out.println("could not load class: " + this.source.getAttribute("ViewClass"));
			}
		}
		else {
			if (!this.load(ext, this.source)) {
				this.source = new XElement("dcui",
						new XElement("Layout", 
								new XElement("h1", "Compile Error!!")
						)
				);
				
				this.load(ext, this.source);
			}
		}
		
		/*
		catch (Exception x) {
			// TODO log
			System.out.println("Compile error on View (" + this.filepath + "): " + x);
		}
		*/
		
	}
	
	// ContentInfo migration
	public boolean load(IWebExtension ext, XElement root) {
		IWebDomain domain = this.getDomain();
		
		XElement el = root.find("Layout");
		
		if (el.hasAttribute("LayoutClass")) {
			try {
				Class<?> cls = ext.getBundle().getClass(el.getAttribute("LayoutClass"));
				
				// TODO improve tracing
				if (cls != null) 
					this.pagebuilder = cls.asSubclass(IContentBuilder.class);
			}
			catch (Exception x) {
				// TODO improve tracing
				System.out.println("Could not load Layout class: " + el.getAttribute("LayoutClass"));
			}
		}
		
		//this.contenttemplate = new Nodes(new Html5Head(el));
		
		if ("dcui".equals(root.getName()))
			this.pagetemplate = domain.parseElement(this, root);
		else
			this.contenttemplate = domain.parseXml(this, el);		
		
		return true;
		
		/*
		for (XElement stel : el.selectAll("Style")) 		
			this.styletemplate.add((Style)domain.parseElement(info, stel).getFirst());
		
		for (XElement scel : el.selectAll("Script")) 		
			this.scripttemplate.add((Script)domain.parseElement(info, scel).getFirst());
		
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
					this.templatemap.put(tid, domain.parseXml(info, tmpel));
			}
			catch (Exception x) {
				// TODO trace
			}
		}
		*/
	}
	
	public Nodes getOutput(Fragment frag, WebContext ctx, boolean dynamic) {
		try {			
			/*
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
			*/
		
			if (this.pagebuilder != null)
				return this.pagebuilder.newInstance().getContent(ctx, this, frag);
			
			if (!dynamic && (this.pagetemplate != null))
				return this.pagetemplate.deepCopy();
			
			if (this.contenttemplate != null)
				return this.contenttemplate.deepCopy();
		} 
		catch (Exception x) {
			// TODO Auto-generated catch block
		}
		
		return null;
	}

	@Override
	public OperationResult execute(WebContext ctx) throws Exception {
		if ((this.auth != null) && !OperationContext.get().getUserContext().isTagged(this.auth)) {
			OperationResult or = new OperationResult();
			
			String mode = ctx.getExternalParam("_dcui");

			if ("dyn".equals(mode) || "dyn".equals(mode)) {
				ctx.getResponse().setHeader("Content-Type", "application/javascript");
				PrintStream ps = ctx.getResponse().getPrintStream();
				ps.println("dc.pui.Loader.failedPageLoad(1);");			
				ctx.send();
			}
			else {
				ctx.getResponse().setStatus(HttpResponseStatus.FOUND);
				ctx.getResponse().setHeader("Location", ctx.getDomain().getMainPath().toString());
				ctx.send();
			}
			
			return or;
		}
		
		IViewExecutor vex = this.adapter.newInstance();
		vex.setViewInfo(this);
		
		return vex.execute(ctx);
	}
}
