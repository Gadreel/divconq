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
package divconq.web.ui.adapter;

import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.PrintStream;
import java.util.Map.Entry;

import divconq.filestore.CommonPath;
import divconq.hub.Hub;
import divconq.io.CacheFile;
import divconq.lang.op.OperationContext;
import divconq.web.IOutputAdapter;
import divconq.web.WebContext;
import divconq.web.WebSite;
import divconq.web.dcui.IViewBuilder;
import divconq.web.dcui.ViewBuilder;
import divconq.xml.XElement;

public class DcuiOutputAdapter implements IOutputAdapter  {
	protected XElement source = null;
	protected CacheFile cfile = null;
	protected CommonPath webpath = null;
	protected String[] auth = null;		// TODO get auth from Skeleton alternatively --- copy skeleton in here at load time
	
	// content building
	protected Class<? extends IViewBuilder> builder = null;
	
	public XElement getSource() {
		return this.source;
	}

	/*
	public CacheFile getFile() {
		return this.cfile;
	}

	public CommonPath getPath() {
		return this.webpath;
	}
	*/

	@Override
	public void init(WebSite site, CacheFile filepath, CommonPath webpath, boolean isPreview) {
		this.webpath = webpath;
		this.cfile = filepath;

		this.builder = ViewBuilder.class;
		
		this.source = this.cfile.asXml();
		
		if (this.source == null) {
			this.source = new XElement("dcui",
					new XElement("Skeleton", 
							new XElement("h1", "Parse Error!!")
					)
			);
		}
		
		if (this.source.getName().equals("dcem"))
			this.builder = divconq.mail.ViewBuilder.class;

		if (this.source.hasAttribute("Skeleton")) {
			String tpath = this.source.getAttribute("Skeleton");
			
			CommonPath pp = new CommonPath(tpath + ".dcuis.xml");		
			
			IOutputAdapter sf = site.findFile(pp, isPreview);
			
			if (sf instanceof DcuiOutputAdapter) {
				XElement layout = ((DcuiOutputAdapter)sf).getSource();
				
				if (layout != null) {
					layout = (XElement) layout.deepCopy();
					
					// copy all attributes over, unless they have been overridden
					for (Entry<String, String> attr : layout.getAttributes().entrySet())
						if (!this.source.hasAttribute(attr.getKey()))
							this.source.setAttribute(attr.getKey(), attr.getValue());
					
					// copy all child elements over
					for (XElement chel : layout.selectAll("*"))
						this.source.add(chel);
				}
			}
		}
		
		if (this.source.hasAttribute("ViewClass")) {
			try {
				Class<?> cls = Hub.instance.getClass(this.source.getAttribute("ViewClass"));
				
				if (cls != null) 
					this.builder = cls.asSubclass(IViewBuilder.class);
			}
			catch (Exception x) {
				// TODO improve tracing
				System.out.println("could not load class: " + this.source.getAttribute("ViewClass"));
			}
		}

		// cache auth tags - only after source has been fully loaded
		if (this.source.hasAttribute("AuthTags"))
			this.auth = this.source.getAttribute("AuthTags").split(",");
	}

	@Override
	public void execute(WebContext ctx) throws Exception {
		if ((this.auth != null) && !OperationContext.get().getUserContext().isTagged(this.auth)) {
			String mode = ctx.getExternalParam("_dcui");

			if ("dyn".equals(mode) || "dyn".equals(mode)) {		// TODO fix second dyn
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
			
			return;
		}
		
		IViewBuilder vex = this.builder.newInstance();
		
		vex.execute(ctx, this);
	}
}
