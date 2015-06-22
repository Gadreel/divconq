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

import java.io.PrintStream;

import w3.html.Form;
import divconq.util.StringUtil;
import divconq.web.WebContext;
import divconq.web.dcui.Nodes;
import divconq.xml.XElement;

public class AdvForm extends Form {
	protected String fname = null;
	protected String recordOrder = null;
	
    public AdvForm() {
    	super();
	}
    
    public AdvForm(Object... args) {
    	super(args);
	}

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("Prefix"))
			attrs.add("Prefix", xel.getRawAttribute("Prefix"));
		
		if (xel.hasAttribute("AlwaysNew"))
			attrs.add("AlwaysNew", xel.getRawAttribute("AlwaysNew"));
		
		this.fname = xel.getAttribute("Name");
		
		if (xel.hasAttribute("RecordOrder"))
			this.recordOrder = xel.getRawAttribute("RecordOrder");

        this.myArguments = new Object[] { attrs, ctx.getDomain().parseXml(ctx, xel) };
		
		nodes.add(this);
		
		//super.parseElement(view, nodes, xel);
	}
    
    @Override
    public boolean writeDynamic(PrintStream buffer, String tabs, boolean first) {
    	this.name = "Form";
    	
    	this.attributes.put("Name", this.fname);		// only with dynamic, let stream appear as "form"
    	
    	if (StringUtil.isNotEmpty(this.recordOrder))
        	this.attributes.put("RecordOrder", this.recordOrder);		
    	
    	return super.writeDynamic(buffer, tabs, first);
    }
}
