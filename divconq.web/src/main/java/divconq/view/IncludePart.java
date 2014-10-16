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
package divconq.view;

import java.util.HashMap;
import java.util.Map;

import divconq.interchange.CommonPath;
import divconq.lang.FuncResult;
import divconq.lang.OperationCallback;
import divconq.web.IWebExtension;
import divconq.web.ViewInfo;
import divconq.web.WebSiteManager;
import divconq.xml.XElement;

public class IncludePart extends FragmentProxy implements ICodeTag {
    protected String src = null;

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
	public Node deepCopy(Element parent) {
		IncludePart cp = new IncludePart();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	protected void doCopy(Node n) {
		super.doCopy(n);
		
		((IncludePart)n).src = this.src;
	}

	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		Map<String,String> params = new HashMap<String,String>();
    	
    	for (divconq.xml.XElement iel : xel.selectAll("Param")) 
			if (iel.hasAttribute("Name"))
				params.put(iel.getRawAttribute("Name"), iel.getRawAttribute("Value"));
    	
		Map<String,Nodes> cparams = new HashMap<String,Nodes>();
    	
    	for (divconq.xml.XElement iel : xel.selectAll("ComplexParam")) 
			if (iel.hasAttribute("Name"))
				cparams.put(iel.getRawAttribute("Name"), view.getDomain().parseXml("HtmlOutput", view, iel));
    	
    	this.src = xel.getRawAttribute("Src");
		
		this.setComplexParams(cparams);
		this.setParams(params);
		
		nodes.add(this);
	}
	
	@Override
	public void doBuild() {
		CommonPath pp = new CommonPath(this.src);		
		
		String ext = pp.getName(0);
		
		IWebExtension wext = ("local".equals(ext)) 
				? this.getContext().getExtension()
				: WebSiteManager.instance.getExtension(ext);
		
		if (wext == null)
			return;
		
		FuncResult<ViewInfo> info = this.getContext().getDomain().getView(this.getContext(), pp, "part");
		
		if (!info.hasErrors()) {
			this.getPartRoot().incrementFuture();
			
			PartBuilder pb = new PartBuilder(info.getResult());
			pb.setParent(this);
			
	        super.build(pb);
			
			pb.awaitForFutures(new OperationCallback() {
				@Override
				public void callback() {
					IncludePart.this.getPartRoot().decrementFuture();
				}
			});
		}
	}
}
