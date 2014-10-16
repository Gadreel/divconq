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
package divconq.view;

import java.io.PrintStream;

import divconq.web.ViewInfo;
import divconq.xml.XElement;


public class LiteralText extends Node implements ICodeTag {
    protected String value = "";

    public LiteralText() {
    	super();
    }
    
    public LiteralText(String value) {
    	super();
        this.value = value;
    }
    
	@Override
	public Node deepCopy(Element parent) {
		LiteralText cp = new LiteralText();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	protected void doCopy(Node n) {
		super.doCopy(n);
		((LiteralText)n).value = this.value;
	}

	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		this.value = xel.getText();
	}
	
    @Override
    public void doBuild() {
       	this.value = this.expandMacro(this.value);        
    }

    @Override
    public void stream(PrintStream strm, String indent, boolean firstchild, boolean fromblock) {
        this.print(strm, firstchild ? indent : "", false, this.value);
    }
}
