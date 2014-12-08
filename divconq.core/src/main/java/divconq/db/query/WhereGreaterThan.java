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

public class WhereGreaterThan extends WhereExpression {
	public WhereGreaterThan(Object a, Object b) {
		super("GreaterThan");
		
		this.addValue("A", a);
		this.addValue("B", b);
	}
	
	public WhereGreaterThan(IWhereField a, Object b) {
		super("GreaterThan");
		
		this.addField("A", a);
		this.addValue("B", b);
	}
	
	public WhereGreaterThan(Object a, IWhereField b) {
		super("GreaterThan");
		
		this.addValue("A", a);
		this.addField("B", b);
	}
	
	public WhereGreaterThan(IWhereField a, IWhereField b) {
		super("GreaterThan");
		
		this.addField("A", a);
		this.addField("B", b);
	}
}
