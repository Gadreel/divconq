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
import divconq.web.dcui.Element;
import divconq.web.dcui.ICodeTag;
import divconq.web.dcui.Nodes;
import divconq.xml.XElement;


public class NbSp extends Element implements ICodeTag {

	@Override
    public void build(WebContext ctx, Object... args) {
        super.build(ctx, "nbsp", false);  
    }

    @Override
    public void stream(WebContext ctx, PrintStream strm, String indent, boolean firstchild, boolean fromblock) {
        this.print(ctx, strm, indent, false, "&nbsp;");
    }

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		// TODO support Count attribute - for N spaces 
		nodes.add(this);
	}
}
