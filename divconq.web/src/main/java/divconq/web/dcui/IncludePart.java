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
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
		Map<String,String> params = new HashMap<String,String>();
    	
    	for (divconq.xml.XElement iel : xel.selectAll("Param")) 
			if (iel.hasAttribute("Name"))
				params.put(iel.getRawAttribute("Name"), iel.getRawAttribute("Value"));
    	
		Map<String,Nodes> cparams = new HashMap<String,Nodes>();
    	
    	for (divconq.xml.XElement iel : xel.selectAll("ComplexParam")) 
			if (iel.hasAttribute("Name"))
				cparams.put(iel.getRawAttribute("Name"), view.getDomain().parseXml(view, iel));
    	
    	this.src = xel.getRawAttribute("Src");
		
		this.setComplexParams(cparams);
		this.setParams(params);
		
		nodes.add(this);
	}
	
	@Override
	public void doBuild() {
		CommonPath pp = new CommonPath(this.src);		
		
		IOutputAdapter sf = this.getContext().getDomain().findFile(this.getContext(), pp);
		
		if (sf instanceof ViewOutputAdapter) {
			this.getPartRoot().incrementFuture();
			
			PartBuilder pb = new PartBuilder((ViewOutputAdapter) sf);
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
