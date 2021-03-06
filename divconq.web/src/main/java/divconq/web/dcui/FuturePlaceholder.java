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

public class FuturePlaceholder extends Element {
    public FuturePlaceholder() {
    	super();
    }
    
    public void decrementFuture() {
    	this.getPartRoot().decrementFuture();
    }
    
    public void incrementFuture() {
    	// let the fragment know that there is something to wait on 
    	this.getPartRoot().incrementFuture();
    }

    @Override
    public void stream(WebContext ctx, PrintStream strm, String indent, boolean firstchild, boolean fromblock) {
        boolean fromon = fromblock;
        boolean lastblock = false;
        boolean firstch = this.getBlockIndent();   // only true once, and only if bi

        for (Node node : this.children) {
            if (node.getBlockIndent() && !lastblock && !fromon) 
            	this.print(ctx, strm, "", true, "");
            
            node.stream(ctx, strm, indent, (firstch || lastblock), this.getBlockIndent());
            
            lastblock = node.getBlockIndent();
            firstch = false;
            fromon = false;
        }
    }
	
	@Override
	public boolean writeDynamic(PrintStream buffer, String tabs, boolean first) {
        if (this.children.size() == 0) 
        	return false;
        
		for (Node child : this.children) {
			if (child.writeDynamic(buffer, tabs, first)) 
				first = false;
		}
		
		return true;
	}

	public void addChild(WebContext ctx, Node nn) {
        nn.setParent(this);
		this.children.add(nn);
        nn.doBuild(ctx);
	}
}
