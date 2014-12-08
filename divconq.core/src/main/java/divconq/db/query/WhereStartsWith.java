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

public class WhereStartsWith extends WhereExpression {
	public WhereStartsWith(Object a, Object b) {
		super("StartsWith");
		
		this.addValue("A", a);
		this.addValue("B", b);
	}
	
	public WhereStartsWith(IWhereField a, Object b) {
		super("StartsWith");
		
		this.addField("A", a);
		this.addValue("B", b);
	}
	
	public WhereStartsWith(Object a, IWhereField b) {
		super("StartsWith");
		
		this.addValue("A", a);
		this.addField("B", b);
	}
	
	public WhereStartsWith(IWhereField a, IWhereField b) {
		super("StartsWith");
		
		this.addField("A", a);
		this.addField("B", b);
	}
}
