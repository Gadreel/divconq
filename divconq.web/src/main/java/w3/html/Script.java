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
import divconq.web.dcui.Element;
import divconq.web.dcui.HtmlUtil;
import divconq.web.dcui.ICodeTag;
import divconq.web.dcui.LiteralText;
import divconq.web.dcui.Node;
import divconq.web.dcui.Nodes;
import divconq.web.dcui.ViewOutputAdapter;
import divconq.xml.XElement;

public class Script extends Element implements ICodeTag {
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

    public Script() {
    	super();
    }
	
    public Script(Object... args) {
    	super(args);
	}
    
    public Script(String source, Object... args) {
    	super(args);
        this.source = source;
    }
    
	@Override
	public Node deepCopy(Element parent) {
		Script cp = new Script();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	protected void doCopy(Node n) {
		super.doCopy(n);
		((Script)n).source = this.source;
		((Script)n).direction = this.direction;
	}

	@Override
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("type"))
			attrs.add("type", xel.getRawAttribute("type"));
		
		if (xel.hasAttribute("src")) { 
			this.setSource(xel.getRawAttribute("src"));
			this.myArguments = new Object[] { attrs };
		}
		else if (xel.hasAttribute("Src")) { 
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
    		super.build("script", true, new Attributes("type", "text/javascript", "src", this.source), args);
			//super.build("script", true, new Attributes("type", "text/javascript", 
			//		"src", this.getPartRoot().buildAssetPath(this.source)), args);
    	else
    		super.build("script", true, new Attributes("type", "text/javascript"), args);
    }
}
