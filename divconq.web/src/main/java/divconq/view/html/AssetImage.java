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
package divconq.view.html;

import divconq.util.StringUtil;
import divconq.view.Attributes;
import divconq.view.Element;
import divconq.view.Node;
import divconq.view.Nodes;
import divconq.web.ViewInfo;
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
	public Node deepCopy(Element parent) {
		AssetImage cp = new AssetImage();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		super.parseElement(view, nodes, xel);
		
		this.bundle = xel.getRawAttribute("Bundle");
		this.src = xel.getRawAttribute("Src");
		this.alt = xel.getRawAttribute("Alt");
	}
	
	@Override
	protected void doCopy(Node n) {
		super.doCopy(n);
		((AssetImage)n).bundle = this.bundle;
		((AssetImage)n).src = this.src;
		((AssetImage)n).alt = this.alt;
	}

    @Override
    public void build(Object... args) {
    	String path = "/";
    	
    	if (StringUtil.isEmpty(this.bundle))
    		path += this.getPartRoot().getContext().getExtension().getAppName();
    	else 
    		path += this.bundle;
    	
    	path += "/Asset/";
    	
    	if (this.src.startsWith("/"))
    		this.src = this.src.substring(1);
    	
    	path += this.src;
    	
        super.build(new Attributes("src", path, "alt", this.expandMacro(this.alt)), args);
    }
}
