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
package w3.html;

import divconq.web.WebContext;
import divconq.web.dcui.Attributes;
import divconq.web.dcui.HtmlUtil;
import divconq.web.dcui.ICodeTag;
import divconq.web.dcui.MixedElement;
import divconq.web.dcui.Node;
import divconq.web.dcui.Nodes;
import divconq.xml.XElement;

public class Select extends MixedElement implements ICodeTag {
	protected String selected = null;
	
    public Select() {
    	super();
	}
	
    public Select(Object... args) {
    	super(args);
	}

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("disabled"))
			attrs.add("disabled", xel.getRawAttribute("disabled"));
		
		if (xel.hasAttribute("multiple"))
			attrs.add("multiple", xel.getRawAttribute("multiple"));
		
		if (xel.hasAttribute("name"))
			attrs.add("name", xel.getRawAttribute("name"));
		
		if (xel.hasAttribute("size"))
			attrs.add("size", xel.getRawAttribute("size"));
		
		if (xel.hasAttribute("select"))
			this.setSelected(xel.getRawAttribute("select"));

        this.myArguments = new Object[] { attrs, ctx.getDomain().parseXml(ctx, xel) };
		
		nodes.add(this);
	}
	
    @Override
	public void build(WebContext ctx, Object... args) {
        if (ctx.isRightToLeft())
            super.build(ctx, "select", new Attributes("dir", "rtl"), args);
        else
        	super.build(ctx, "select", args);
        
        if (this.selected != null) {
        	this.selected = this.expandMacro(ctx, this.selected);
        	
        	for (Node n : this.children) {
        		if (n instanceof Option) {
        			Option on = (Option)n;
        			
        			if (this.selected.equals(on.getAttribute("value"))) {
        				on.addAttribute("selected", "selected");
        				break;
        			}
        		}
        	}
        }
	}

	public void setSelected(String value) {
		this.selected = value;
	}
}
