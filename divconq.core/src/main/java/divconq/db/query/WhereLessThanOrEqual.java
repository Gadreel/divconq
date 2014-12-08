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

public class WhereLessThanOrEqual extends WhereExpression {
	public WhereLessThanOrEqual(Object a, Object b) {
		super("LessThanOrEqual");
		
		this.addValue("A", a);
		this.addValue("B", b);
	}
	
	public WhereLessThanOrEqual(IWhereField a, Object b) {
		super("LessThanOrEqual");
		
		this.addField("A", a);
		this.addValue("B", b);
	}
	
	public WhereLessThanOrEqual(Object a, IWhereField b) {
		super("LessThanOrEqual");
		
		this.addValue("A", a);
		this.addField("B", b);
	}
	
	public WhereLessThanOrEqual(IWhereField a, IWhereField b) {
		super("LessThanOrEqual");
		
		this.addField("A", a);
		this.addField("B", b);
	}
}
