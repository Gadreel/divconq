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

import divconq.web.WebContext;
import divconq.web.dcui.Attributes;
import divconq.web.dcui.Element;
import divconq.web.dcui.HtmlUtil;
import divconq.web.dcui.ICodeTag;
import divconq.web.dcui.Nodes;
import divconq.xml.XElement;


public class Wbr extends Element implements ICodeTag {

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);

        this.myArguments = new Object[] { attrs };
		
		nodes.add(this);
	}

	@Override
    public void build(WebContext ctx, Object... args) {
        super.build(ctx, "wbr", false);  // like to make this a block level, but right now that would create <br></br> which we don't want (but do need for div handling)
    }

	// special case, want new line after <wbr/>
    @Override
    public void stream(WebContext ctx, PrintStream strm, String indent, boolean firstchild, boolean fromblock) {
        this.print(ctx, strm, "", false, "<wbr />");
    }
}
