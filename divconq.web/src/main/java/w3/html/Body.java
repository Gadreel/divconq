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

import divconq.web.dcui.Attributes;
import divconq.web.dcui.Element;
import divconq.web.dcui.HtmlUtil;
import divconq.web.dcui.ICodeTag;
import divconq.web.dcui.MixedElement;
import divconq.web.dcui.Node;
import divconq.web.dcui.Nodes;
import divconq.web.dcui.ViewOutputAdapter;
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
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("onload"))
			attrs.add("onload", xel.getRawAttribute("onload"));
		
        this.myArguments = new Object[] { attrs, view.getDomain().parseXml(view, xel) };
		
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
