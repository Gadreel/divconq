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

abstract public class MixedElement extends Element {
    public MixedElement(Object... args) {
    	super(args);
	}
	
    @Override
	public void build(Object... args) {
	    for (int i = 0; i < args.length; i++) {
	        if ((i != 0) && (args[i] instanceof CharSequence)) 
	            args[i] = new LiteralText(args[i].toString());
	    }
	
	    super.build(args);
	}
}
