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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import divconq.lang.OperationCallback;

public class ContentPlaceholder extends Element {
	protected List<Object> held = new ArrayList<Object>();
	
    public ContentPlaceholder() {
    	super();
    }
    
    @Override
    public void doBuild() {
    	this.getPartRoot().awaitForFutures(new OperationCallback() {
			@Override
			public void callback() {
				ContentPlaceholder.this.build(ContentPlaceholder.this.held.toArray());
				ContentPlaceholder.this.held.clear();
			}
		});
    }

    @Override
    public void stream(PrintStream strm, String indent, boolean firstchild, boolean fromblock) {
    	if (this.held.size() > 0) 
    		this.doBuild();
    	
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

	public void addChildren(Object... nn) {
		this.held.addAll(Arrays.asList(nn));
	}

	@Override
	public Node deepCopy(Element parent) {
		ContentPlaceholder cp = new ContentPlaceholder();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}
}
