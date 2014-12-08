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

public class WhereIsNot extends WhereExpression {
	public WhereIsNot(Object a) {
		super("IsNot");
		
		this.addValue("A", a);
	}
	
	public WhereIsNot(IWhereField a) {
		super("IsNot");
		
		this.addField("A", a);
	}
}
