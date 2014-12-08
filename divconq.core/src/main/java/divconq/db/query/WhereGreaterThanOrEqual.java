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

public class WhereGreaterThanOrEqual extends WhereExpression {
	public WhereGreaterThanOrEqual(Object a, Object b) {
		super("GreaterThanOrEqual");
		
		this.addValue("A", a);
		this.addValue("B", b);
	}
	
	public WhereGreaterThanOrEqual(IWhereField a, Object b) {
		super("GreaterThanOrEqual");
		
		this.addField("A", a);
		this.addValue("B", b);
	}
	
	public WhereGreaterThanOrEqual(Object a, IWhereField b) {
		super("GreaterThanOrEqual");
		
		this.addValue("A", a);
		this.addField("B", b);
	}
	
	public WhereGreaterThanOrEqual(IWhereField a, IWhereField b) {
		super("GreaterThanOrEqual");
		
		this.addField("A", a);
		this.addField("B", b);
	}
}
