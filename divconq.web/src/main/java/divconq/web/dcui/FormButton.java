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

import w3.html.Input;
import divconq.web.WebContext;
import divconq.web.dcui.Attributes;
import divconq.web.dcui.Nodes;
import divconq.xml.XElement;

public class FormButton extends Input {
    protected String label = null;
    protected String icon = null;
    protected String click = null;
    protected boolean submit = false;
	
    public FormButton() {
    	super();
	}
    
    public FormButton(Object... args) {
    	super(args);
	}

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		super.parseElement(ctx, nodes, xel);
		
		this.label = xel.getRawAttribute("Label");
		this.icon = xel.getRawAttribute("Icon");
		this.click = xel.getRawAttribute("Click");		
		this.submit = "SubmitButton".equals(xel.getName());
	}
	
    @Override
	public void build(WebContext ctx, Object... args) {
		Attributes attrs = new Attributes("value", this.label, "type", this.submit ? "submit" : "button",
				 "data-icon", this.icon, "dir", ctx.isRightToLeft() ? "rtl" : "ltr");

       	super.build(ctx, "input", args, attrs);
	}
    
    @Override
    public boolean writeDynamic(PrintStream buffer, String tabs, boolean first) {
    	this.name = this.submit ? "SubmitButton" : "Button";
    	
    	this.attributes.put("Click", this.click);
    	
    	return super.writeDynamic(buffer, tabs, first);
    }
}
