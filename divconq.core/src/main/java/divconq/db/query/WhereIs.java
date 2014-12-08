/* ************************************************************************
#
#  DivConq
#
#  http://divconq.com/
#
#  Copyright:
#    Copyright 2012 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package divconq.db.query;

public class WhereIs extends WhereExpression {
	public WhereIs(Object a) {
		super("Is");
		
		this.addValue("A", a);
	}
	
	public WhereIs(IWhereField a) {
		super("Is");
		
		this.addField("A", a);
	}
}
