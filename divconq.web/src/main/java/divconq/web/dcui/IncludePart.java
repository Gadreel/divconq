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

import java.util.HashMap;
import java.util.Map;

import divconq.filestore.CommonPath;
import divconq.lang.op.OperationCallback;
import divconq.web.IOutputAdapter;
import divconq.web.WebContext;
import divconq.xml.XElement;

public class IncludePart extends FragmentProxy implements ICodeTag {
    protected String src = null;
    protected XElement xml = null;
    protected Fragment frag = new Fragment();

    public IncludePart() {
    	super();
    }
    
    public IncludePart(String src) {
    	super();
        this.src = src;
    }
    
    public IncludePart(String src, Map<String, Nodes> params) {
    	super();
        this.src = src;
        this.complexparams = params;
    }

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
    	
    	this.src = xel.getRawAttribute("Path");
    	this.xml = xel;
    	
    	this.myArguments = new Object[] { attrs };
    	
    	nodes.add(this);
	}
	
	@Override
	public void doBuild(WebContext ctx) {
    	this.frag.setParent(this);
		
		CommonPath pp = new CommonPath(this.src);		
		
		IOutputAdapter sf = ctx.getDomain().findFile(ctx.isPreview(), pp, ctx.getExtension());
		
		if (sf instanceof ViewOutputAdapter) {
			if (this.xml != null) {
				Map<String,String> params = new HashMap<String,String>();
		    	
		    	for (divconq.xml.XElement iel : this.xml.selectAll("Param")) 
					if (iel.hasAttribute("Name"))
						params.put(iel.getRawAttribute("Name"), iel.getRawAttribute("Value"));
		    	
				Map<String,Nodes> cparams = new HashMap<String,Nodes>();
		    	
		    	for (divconq.xml.XElement iel : this.xml.selectAll("ComplexParam")) 
					if (iel.hasAttribute("Name"))
						cparams.put(iel.getRawAttribute("Name"), ctx.getDomain().parseXml(ctx, iel));
				
		    	this.frag.setComplexParams(cparams);
		    	this.frag.setParams(params);			
			}
			
			this.getPartRoot().incrementFuture();
			
			this.frag.initializePart(ctx, sf, new OperationCallback() {				
				@Override
				public void callback() {
					//IncludePart.this.frag.doBuild(ctx);
					
					IncludePart.this.build(ctx, IncludePart.this.frag);
					
					IncludePart.this.frag.awaitForFutures(new OperationCallback() {						
						@Override
						public void callback() {
							IncludePart.this.getPartRoot().decrementFuture();
						}
					});
				}
			}, this.myArguments);
		}		
	}
}
