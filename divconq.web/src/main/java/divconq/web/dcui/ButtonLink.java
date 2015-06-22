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
import divconq.web.WebContext;
import divconq.xml.XElement;

public class ButtonLink extends MixedElement implements ICodeTag {
    protected String id = null;
    protected String to = null;
    protected String label = null;
    protected String icon = null;
    protected String click = null;
    protected String page = null;
    protected boolean wide = false;
    protected String css = null;

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
    
    public ButtonLink withCss(String v) {
    	this.css = v;
    	return this;
    }
	
	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("href"))
			attrs.add("href", xel.getRawAttribute("href"));
		
		if (xel.hasAttribute("rel"))
			attrs.add("rel", xel.getRawAttribute("rel"));
		
		if (xel.hasAttribute("target"))
			attrs.add("target", xel.getRawAttribute("target"));

        this.myArguments = new Object[] { attrs, ctx.getDomain().parseXml(ctx, xel) };
		
		nodes.add(this);
		
		if (xel.hasAttribute("To"))
			this.to = xel.getRawAttribute("To");
		else
			this.to = "#";
		
		this.label = xel.getRawAttribute("Label");
		this.icon = xel.getRawAttribute("Icon");
		this.click = xel.getRawAttribute("Click");
		this.page = xel.getRawAttribute("Page");
		this.css = xel.getRawAttribute("class");
		
		if (StringUtil.isNotEmpty(this.page))
			this.to = this.page;
		
		this.wide = "WideButton".equals(xel.getName());
	}

    @Override
    public void build(WebContext ctx, Object... args) {
    	Attributes attrs = this.wide 
			? new Attributes("href", this.to, "class", "ui-button ui-button-wide ui-theme-a " + StringUtil.toEmpty(this.css)) 
			: new Attributes("href", this.to, "class", "ui-button ui-theme-a " + StringUtil.toEmpty(this.css));
		
		if (this.id != null)
			attrs.add("id", this.id);
		
        super.build(ctx, "a", args, attrs, this.label);
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
