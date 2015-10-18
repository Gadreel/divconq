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

import divconq.web.WebContext;
import divconq.xml.XElement;
import w3.html.Div;
import w3.html.H4;

// dead right now...

public class CaptionedImage  extends Element implements ICodeTag {
    protected String src = null;
    protected String caption = null;
    protected String alt = null;
    protected String position = null;

    public CaptionedImage() {
    	super();
    }
    
    public CaptionedImage(String src, String alt) {
    	super();
        this.src = src;
        this.alt = alt;
    }
    
    public CaptionedImage(String src, String alt, String caption) {
    	super();
        this.caption = caption;
        this.src = src;
        this.alt = alt;
    }
    
    public CaptionedImage(String src, String alt, String caption, String position) {
    	super();
        this.caption = caption;
        this.position = position;
        this.src = src;
        this.alt = alt;
    }
	
	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		
		if (xel.hasAttribute("class"))
			xel.setAttribute("class", xel.getAttribute("class") + " ui-corner-all custom-corners section");
		else
			xel.setAttribute("class", "ui-corner-all custom-corners section");
		
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("align"))
			attrs.add("align", xel.getRawAttribute("align"));

		Div title = new Div(new Attributes("class", "ui-bar ui-bar-a"), new H4(xel.getAttribute("Title")));
		
		Div body = (xel.hasAttribute("id"))
				? new Div(new Attributes("id", xel.getAttribute("id") + "Body", "class", "ui-body ui-body-a"), ctx.getDomain().parseXml(ctx, xel))
				: new Div(new Attributes("class", "ui-body ui-body-a"), ctx.getDomain().parseXml(ctx, xel));
		
        this.myArguments = new Object[] { attrs, title, body };
		
		nodes.add(this);

		/*
		this.caption = xel.getRawAttribute("Caption");
		this.position = xel.getRawAttribute("Position");
		this.src = xel.getRawAttribute("Path");
		this.alt = xel.getRawAttribute("Alt");
		*/
	}
	
    @Override
	public void build(WebContext ctx, Object... args) {
	    super.build(ctx, "div", true, args);
	}
}
