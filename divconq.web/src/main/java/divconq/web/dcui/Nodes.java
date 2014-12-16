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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Nodes {
    protected List<Node> nodes = new ArrayList<Node>();

    public int getCount() {
        return this.nodes.size(); 
    }

    public Nodes(Node... args) {
    	this.nodes.addAll(Arrays.asList(args));
    }

    public void add(Node... args) {
    	this.nodes.addAll(Arrays.asList(args));
    }

    public Collection<Node> getList() {
        return this.nodes;
    }

	public Nodes deepCopy() {
		Nodes n = new Nodes();
		
    	for (Node h : this.nodes) 
    		n.nodes.add(h.deepCopy(null));		// no parent yet
    	
    	return n;
	}

	public Node getFirst() {
		return this.nodes.get(0);
	}
}
