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
import divconq.hub.Hub;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.util.IOUtil;
import divconq.web.IOutputAdapter;
import divconq.web.WebDomain;
import divconq.web.WebContext;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

public class ViewOutputAdapter implements IOutputAdapter  {
	protected XElement source = null;
	protected WebDomain domain = null;
	protected CommonPath webpath = null;
	protected String[] auth = null;

	protected boolean isPreview = false;
	
	// content info
	protected Nodes pagetemplate = null; 
	public Nodes contenttemplate = null;
	protected Class<? extends IContentBuilder> pagebuilder = null;
	
	// content building
	protected Class<? extends IViewExecutor> adapter = null;
	
	public XElement getSource() {
		return this.source;
	}
	
	public WebDomain getDomain() {
		return this.domain;
	}
	
	public boolean isPreview() {
		return this.isPreview;
	}
	
	public ViewOutputAdapter(WebDomain domain, CommonPath webpath, Path filepath, boolean isPreview) {
		this.domain = domain;
		this.webpath = webpath;
		this.isPreview = isPreview;
		
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
					new XElement("Skeleton", 
							new XElement("h1", "Parse Error!!")
					)
			);
		}
		
		if (this.source.hasAttribute("AuthTags"))
			this.auth = this.source.getAttribute("AuthTags").split("\\|");

		if (this.source.hasAttribute("ViewClass")) {
			try {
				// TODO ideally this would come from web extension or web module...
				Class<?> cls = Hub.instance.getClass(this.source.getAttribute("ViewClass"));
				
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
			if (!this.load(this.source)) {
				this.source = new XElement("dcui",
						new XElement("Skeleton", 
								new XElement("h1", "Compile Error!!")
						)
				);
				
				this.load(this.source);
			}
		}		
	}
	
	// ContentInfo migration
	public boolean load(XElement root) {
		WebDomain domain = this.getDomain();
		
		if (root.hasAttribute("SkeletonClass")) {
			try {
				// TODO ideally this would come from web extension or web module...
				Class<?> cls = Hub.instance.getClass(root.getAttribute("SkeletonClass"));
				
				// TODO improve tracing
				if (cls != null) 
					this.pagebuilder = cls.asSubclass(IContentBuilder.class);
			}
			catch (Exception x) {
				// TODO improve tracing
				System.out.println("Could not load Layout class: " + root.getAttribute("SkeletonClass"));
			}
		}
		
		if ("dcui".equals(root.getName()))
			this.pagetemplate = domain.parseElement(this, root);
		else
			this.contenttemplate = domain.parseXml(this, root.find("Skeleton"));		
		
		return true;
	}
	
	public Nodes getOutput(Fragment frag, WebContext ctx, boolean dynamic) {
		try {
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
				ctx.getResponse().setHeader("Location", "/");
				ctx.send();
			}
			
			return or;
		}
		
		IViewExecutor vex = this.adapter.newInstance();
		vex.setViewInfo(this);
		
		return vex.execute(ctx);
	}
}
