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

public class WhereLessThan extends WhereExpression {
	public WhereLessThan(Object a, Object b) {
		super("LessThan");
		
		this.addValue("A", a);
		this.addValue("B", b);
	}
	
	public WhereLessThan(IWhereField a, Object b) {
		super("LessThan");
		
		this.addField("A", a);
		this.addValue("B", b);
	}
	
	public WhereLessThan(Object a, IWhereField b) {
		super("LessThan");
		
		this.addValue("A", a);
		this.addField("B", b);
	}
	
	public WhereLessThan(IWhereField a, IWhereField b) {
		super("LessThan");
		
		this.addField("A", a);
		this.addField("B", b);
	}
}
