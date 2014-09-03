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

import divconq.view.Attributes;
import divconq.view.Element;
import divconq.view.ICodeTag;
import divconq.view.LiteralText;
import divconq.view.Node;
import divconq.view.Nodes;
import divconq.view.html.HtmlUtil;
import divconq.web.ViewInfo;
import divconq.web.WebContext;
import divconq.xml.XElement;

public class Style extends Element implements ICodeTag {
	public static int DIR_BOTH = 0;
	public static int DIR_LTR = 1;
	public static int DIR_RTL = 2;
	
	protected String source = null;
	protected int direction = Style.DIR_BOTH;
	
	public void setDirection(int dir) {
		this.direction = dir;
	}
	
	public int getDirection() {
		return this.direction;
	}
	
	public void setSource(String src) {
		this.source = src;
	}
	
    public Style() {
    	super();
	}
	
    public Style(Object... args) {
    	super(args);
	}
    
    public Style(String source) {
    	super();
    	this.source = source;
	}    
    
	@Override
	public Node deepCopy(Element parent) {
		Style cp = new Style();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	protected void doCopy(Node n) {
		super.doCopy(n);
		((Style)n).source = this.source;
		((Style)n).direction = this.direction;
	}

	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("Src")) {
			this.setSource(xel.getRawAttribute("Src"));
	        this.myArguments = new Object[] { attrs };
		}
		else {
	        this.myArguments = new Object[] { new LiteralText(xel.getText()), attrs };
		}
		
		if ("ltr".equals(xel.getRawAttribute("Dir")))
			this.setDirection(Style.DIR_LTR);		
		else if ("rtl".equals(xel.getRawAttribute("Dir")))
			this.setDirection(Style.DIR_RTL);
		
		nodes.add(this);
	}

	// TODO what?  maybe remove?
    @Override
    public void setParent(Element value) {
    	this.parent = value; 
    	
    	if (value != null) {
    		// it is possible to already have a part root assigned, if so keep the assigned
    		if (this.partroot == null)
    			this.partroot = value.getPartRoot();
    		
    		// our view root is always the same as the parent's
	        this.viewroot = value.getViewRoot();
    	}
    }
	
    @Override
	public void build(Object... args) {
    	if (this.direction != Style.DIR_BOTH) {
    		WebContext mc = this.getContext();
    		
    		// if incompatible directions then just skip
    		if (mc.isRightToLeft() && (this.direction != Style.DIR_RTL))
    			return;
    		
    		if (!mc.isRightToLeft() && (this.direction != Style.DIR_LTR))
    			return;
    	}
    	
    	if (this.source != null) 
    		super.build("link", true, new Attributes("type", "text/css", "rel", "stylesheet", "href", this.source), args);
    		//super.build("link", true, new Attributes("type", "text/css", "rel", "stylesheet", 
			//	"href", this.getPartRoot().buildAssetPath(this.source)), args);
    	else
    		super.build("style", true, new Attributes("type", "text/css"), args);
	}
}
