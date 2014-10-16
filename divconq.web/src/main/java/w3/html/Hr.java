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

public class Hr extends Element implements ICodeTag {
    
	@Override
	public Node deepCopy(Element parent) {
		Hr cp = new Hr();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);

        this.myArguments = new Object[] { attrs, view.getDomain().parseXml("HtmlOutput", view, xel) };
		
		nodes.add(this);
	}
	
	@Override
    public void doBuild() {
        this.build();
    }

	@Override
    public void build(Object... args) {
        super.build("hr", true);  
    }

	// special case, want new line after <br/>
    @Override
    public void stream(PrintStream strm, String indent, boolean firstchild, boolean fromblock) {
        this.print(strm, "", true, "<hr />");
    }
}
