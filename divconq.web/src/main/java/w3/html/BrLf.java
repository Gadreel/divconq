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

import divconq.web.dcui.Attributes;
import divconq.web.dcui.Element;
import divconq.web.dcui.HtmlUtil;
import divconq.web.dcui.ICodeTag;
import divconq.web.dcui.Node;
import divconq.web.dcui.Nodes;
import divconq.web.dcui.ViewOutputAdapter;
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
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
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
