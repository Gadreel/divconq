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
import divconq.view.MixedElement;
import divconq.view.Node;
import divconq.view.Nodes;
import divconq.view.html.HtmlUtil;
import divconq.web.ViewInfo;
import divconq.xml.XElement;

public class Body extends MixedElement implements ICodeTag {	
    public Body() {
    	super();
	}

    public Body(Object... args) {
    	super(args);
	}

	@Override
	public Node deepCopy(Element parent) {
		Body cp = new Body();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("onload"))
			attrs.add("onload", xel.getRawAttribute("onload"));
		
        this.myArguments = new Object[] { attrs, view.getDomain().parseXml("HtmlOutput", view, xel) };
		
		nodes.add(this);
	}
	
    @Override
	public void doBuild() {
	    this.build(this.myArguments);
	    // TODO
	    //Root.Context.PageContext.AddBeforeWriteListener(BuildBodyAttrs);
	}
	
    @Override
	public void build(Object... args) {
	    super.build("body", true, args);
	}
	
    /* TODO
	public void buildBodyAttrs() {
	    string onload = String.Empty;
	
	    foreach (string code in Root.Context.PageContext.OnLoadCode)
	    {
	        onload += code;
	    }
	
	    if (onload != String.Empty) base.Build(new Attributes("onload", onload));
	}
	*/
}
