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

public class ProcessingInstruction extends Node {
    protected String value = "";

    public ProcessingInstruction() {
    	super();
    }
    
    public ProcessingInstruction(String value) {
    	super();
        this.value = value;
    }
    
	@Override
	public Node deepCopy(Element parent) {
		ProcessingInstruction cp = new ProcessingInstruction();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	protected void doCopy(Node n) {
		super.doCopy(n);
		((ProcessingInstruction)n).value = this.value;
	}

    @Override
    public void doBuild() {
        // nothing to do
    }

    @Override
    public void stream(PrintStream strm, String indent, boolean firstchild, boolean fromblock) {
        this.print(strm, firstchild ? indent : "", firstchild, "<? " + this.value + " ?>");
    }
}
