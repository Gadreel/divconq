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

import java.io.PrintStream;

import divconq.view.Attributes;
import divconq.view.Element;
import divconq.view.ICodeTag;
import divconq.view.Node;
import divconq.view.Nodes;
import divconq.view.html.HtmlUtil;
import divconq.web.ViewInfo;
import divconq.xml.XElement;


public class BrLf extends Element implements ICodeTag {

	@Override
	public Node deepCopy(Element parent) {
		BrLf cp = new BrLf();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);

        this.myArguments = new Object[] { attrs };
		
		nodes.add(this);
	}
	
	@Override
    public void doBuild() {
        this.build();
    }

	@Override
    public void build(Object... args) {
        super.build("br", false);  // TODO like to make this a block level, but right now that would create <br></br> which we don't want (but do need for div handling)
    }

	// special case, want new line after <br/>
    @Override
    public void stream(PrintStream html, String indent, boolean firstchild, boolean fromblock) {
        this.print(html, indent, true, "<br />");
    }
}
