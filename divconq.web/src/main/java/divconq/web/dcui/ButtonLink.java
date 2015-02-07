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

import divconq.util.StringUtil;
import divconq.xml.XElement;
import w3.html.A;


public class ButtonLink extends A {
    protected String id = null;
    protected String to = null;
    protected String label = null;
    protected String icon = null;
    protected String click = null;
    protected String page = null;
    protected boolean wide = false;

    public ButtonLink() {
    	super();
    }
    
    public ButtonLink(Object... args) {
    	super(args);
	}
    
    public ButtonLink(String to, Object... args) {
    	super(args);
        this.to = to;
    }
    
    public ButtonLink withId(String v) {
    	this.id = v;
    	return this;
    }
    
    public ButtonLink withLabel(String v) {
    	this.label = v;
    	return this;
    }
    
    public ButtonLink withIcon(String v) {
    	this.icon = v;
    	return this;
    }
    
    public ButtonLink withPage(String v) {
    	this.page = v;
    	return this;
    }
    
    public ButtonLink withClick(String v) {
    	this.click = v;
    	return this;
    }
    
	@Override
	public Node deepCopy(Element parent) {
		ButtonLink cp = new ButtonLink();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	protected void doCopy(Node n) {
		super.doCopy(n);
		((ButtonLink)n).id = this.id;
		((ButtonLink)n).to = this.to;
		((ButtonLink)n).page = this.page;
		((ButtonLink)n).label = this.label;
		((ButtonLink)n).icon = this.icon;
		((ButtonLink)n).click = this.click;
		((ButtonLink)n).wide = this.wide;
	}
	
	@Override
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
		super.parseElement(view, nodes, xel);
		
		if (xel.hasAttribute("To"))
			this.to = xel.getRawAttribute("To");
		else
			this.to = "#";
		
		this.label = xel.getRawAttribute("Label");
		this.icon = xel.getRawAttribute("Icon");
		this.click = xel.getRawAttribute("Click");
		this.page = xel.getRawAttribute("Page");
		
		if (StringUtil.isNotEmpty(this.page))
			this.to = this.page;
		
		this.wide = "WideButton".equals(xel.getName());
	}

    @Override
    public void build(Object... args) {
    	Attributes attrs = this.wide 
			? new Attributes("href", this.to, "class", "ui-button ui-button-wide ui-theme-a")
			: new Attributes("href", this.to, "class", "ui-button ui-theme-a");
		
		if (this.id != null)
			attrs.add("id", this.id);
		
        super.build(args, attrs, this.label);
    }
    
    @Override
    public boolean writeDynamic(PrintStream buffer, String tabs, boolean first) {
    	this.name = this.wide ? "WideButton" : "Button";
    	
    	this.attributes.put("Click", this.click);
    	this.attributes.put("Page", this.page);
    	this.attributes.put("Icon", this.icon);
    	
    	return super.writeDynamic(buffer, tabs, first);
    }
}
