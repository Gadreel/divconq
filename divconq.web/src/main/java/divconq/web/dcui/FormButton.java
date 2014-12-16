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
import divconq.web.dcui.Attributes;
import divconq.web.dcui.Element;
import divconq.web.dcui.Node;
import divconq.web.dcui.Nodes;
import divconq.web.dcui.ViewOutputAdapter;
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
	public Node deepCopy(Element parent) {
		FormButton cp = new FormButton();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	protected void doCopy(Node n) {
		super.doCopy(n);
		((FormButton)n).label = this.label;
		((FormButton)n).icon = this.icon;
		((FormButton)n).click = this.click;
		((FormButton)n).submit = this.submit;
	}

	@Override
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
		super.parseElement(view, nodes, xel);
		
		this.label = xel.getRawAttribute("Label");
		this.icon = xel.getRawAttribute("Icon");
		this.click = xel.getRawAttribute("Click");		
		this.submit = "SubmitButton".equals(xel.getName());
	}
	
    @Override
	public void build(Object... args) {
		Attributes attrs = new Attributes("value", this.label, "type", this.submit ? "submit" : "button",
				 "data-icon", this.icon, "dir", this.getContext().isRightToLeft() ? "rtl" : "ltr");

       	super.build("input", args, attrs);
	}
    
    @Override
    public boolean writeDynamic(PrintStream buffer, String tabs, boolean first) {
    	this.name = this.submit ? "SubmitButton" : "Button";
    	
    	this.attributes.put("Click", this.click);
    	
    	return super.writeDynamic(buffer, tabs, first);
    }
}
