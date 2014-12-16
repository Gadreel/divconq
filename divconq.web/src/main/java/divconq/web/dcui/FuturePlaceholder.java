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
	public Node deepCopy(Element parent) {
		FuturePlaceholder cp = new FuturePlaceholder();  
		cp.setParent(parent);
		this.doCopy(cp);		// no need to override, we don't care about view field
		return cp;
	}

    @Override
    public void stream(PrintStream strm, String indent, boolean firstchild, boolean fromblock) {
        boolean fromon = fromblock;
        boolean lastblock = false;
        boolean firstch = this.getBlockIndent();   // only true once, and only if bi

        for (Node node : this.children) {
            if (node.getBlockIndent() && !lastblock && !fromon) 
            	this.print(strm, "", true, "");
            
            node.stream(strm, indent, (firstch || lastblock), this.getBlockIndent());
            
            lastblock = node.getBlockIndent();
            firstch = false;
            fromon = false;
        }
    }

	public void addChild(Node nn) {
        nn.setParent(this);
		this.children.add(nn);
        nn.doBuild();
	}
}
