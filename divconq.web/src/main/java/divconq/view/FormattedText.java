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

public class FormattedText extends Node {
    protected String value = "";
    protected Object[] values = null;

    public FormattedText() {
    	super();
    }
    
    public FormattedText(String value, Object... values) {
    	super();
        this.value = value;
        this.values = values;
    }

    @Override
    protected void doCopy(Node n) {
    	super.doCopy(n);
    	
    	FormattedText nn = (FormattedText)n;
    	nn.value = this.value;
    	nn.values = this.values;
    }
    
	@Override
	public Node deepCopy(Element parent) {
		FormattedText cp = new FormattedText();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

    @Override
    public void doBuild() {
       	this.value = this.expandMacro(this.value);        
        this.value = this.getContext().format(this.value, this.values);
    }

    @Override
    public void stream(PrintStream strm, String indent, boolean firstchild, boolean fromblock) {
        this.print(strm, firstchild ? indent : "", false, this.value);
    }
}
