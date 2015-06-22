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
package divconq.web.dcui;

import java.io.PrintStream;

import divconq.web.WebContext;

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
    public void doBuild(WebContext ctx) {
        // nothing to do
    }

    @Override
    public void stream(WebContext ctx, PrintStream strm, String indent, boolean firstchild, boolean fromblock) {
        this.print(ctx, strm, firstchild ? indent : "", firstchild, "<? " + this.value + " ?>");
    }
}
