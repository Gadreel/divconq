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
import java.util.Map.Entry;

import divconq.filestore.CommonPath;
import divconq.hub.Hub;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.util.IOUtil;
import divconq.web.IOutputAdapter;
import divconq.web.WebContext;
import divconq.web.WebDomain;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

public class ViewOutputAdapter implements IOutputAdapter  {
	protected XElement source = null;
	protected Path filepath = null;
	protected CommonPath webpath = null;
	protected String[] auth = null;		// TODO get auth from Skeleton alternatively --- copy skeleton in here at load time
	
	// content building
	protected Class<? extends IViewBuilder> builder = null;
	
	public XElement getSource() {
		return this.source;
	}
	
	@Override
	public Path getFilePath() {
		return this.filepath;
	}

	@Override
	public CommonPath getLocationPath() {
		return this.webpath;
	}

	@Override
	public void init(WebDomain domain, Path filepath, CommonPath webpath, boolean isPreview) {
		this.webpath = webpath;
		this.filepath = filepath;
		
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
		
		this.builder = ViewBuilder.class;
		
		this.source = xres.getResult();
		
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
			
			IOutputAdapter sf = domain.findFile(isPreview, pp, null);
			
			if (sf instanceof ViewOutputAdapter) {
				XElement layout = ((ViewOutputAdapter)sf).getSource();
				
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
