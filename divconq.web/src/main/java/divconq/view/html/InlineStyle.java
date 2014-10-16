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
package divconq.view.html;

import divconq.view.Element;
import divconq.view.Node;
import w3.html.Style;


public class InlineStyle extends Style {
    public InlineStyle(Object... args) {
    	super(args);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		InlineStyle cp = new InlineStyle();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}
}
