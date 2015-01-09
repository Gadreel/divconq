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


public class FragmentProxy extends Element  {
    public FragmentProxy() {
    	super();
    }
    
	@Override
	public Node deepCopy(Element parent) {
		FragmentProxy cp = new FragmentProxy();  
		cp.setParent(parent);
		this.doCopy(cp);		// no need to override, we don't care about view field
		return cp;
	}

    @Override
    public void stream(PrintStream strm, String indent, boolean firstchild, boolean fromblock) {
        if (this.children.size() == 0) 
        	return;
        
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

	public void addChild(Node... nn) {
		for (Node n : nn) {
	        n.setParent(this);
			this.children.add(n);
	        n.doBuild();
		}
	}
}
