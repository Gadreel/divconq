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

import divconq.util.StringUtil;
import divconq.web.WebContext;
import divconq.xml.XElement;
import w3.html.Img;


public class AssetImage extends Img {
    protected String src = null;
    protected String bundle = null;
    protected String alt = null;

    public AssetImage() {
    	super();
    }
    
    public AssetImage(String src, String alt) {
    	super();
        this.src = src;
        this.alt = alt;
    }
    
    public AssetImage(String bundle, String src, String alt) {
    	super();
        this.bundle = bundle;
        this.src = src;
        this.alt = alt;
    }
	
	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		super.parseElement(ctx, nodes, xel);
		
		this.bundle = xel.getRawAttribute("Bundle");
		this.src = xel.getRawAttribute("Src");
		this.alt = xel.getRawAttribute("Alt");
	}

    @Override
    public void build(WebContext ctx, Object... args) {
    	String path = "/";
    	
    	if (StringUtil.isEmpty(this.bundle))
    		path += ctx.getExtension().getAppName();
    	else 
    		path += this.bundle;
    	
    	path += "/Asset/";
    	
    	if (this.src.startsWith("/"))
    		this.src = this.src.substring(1);
    	
    	path += this.src;
    	
        super.build(ctx, new Attributes("src", path, "alt", this.expandMacro(ctx, this.alt)), args);
    }
}
