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

import divconq.web.dcui.Element;
import divconq.web.dcui.ICodeTag;
import divconq.web.dcui.Node;
import divconq.web.dcui.Nodes;
import divconq.web.dcui.ViewOutputAdapter;
import divconq.xml.XElement;


public class NbSp extends Element implements ICodeTag {

	@Override
	public Node deepCopy(Element parent) {
		NbSp cp = new NbSp();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}
	
	@Override
    public void doBuild() {
        this.build();
    }

	@Override
    public void build(Object... args) {
        super.build("nbsp", false);  
    }

    @Override
    public void stream(PrintStream strm, String indent, boolean firstchild, boolean fromblock) {
        this.print(strm, indent, false, "&nbsp;");
    }

	@Override
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
		// TODO support Count attribute - for N spaces 
		nodes.add(this);
	}
}
