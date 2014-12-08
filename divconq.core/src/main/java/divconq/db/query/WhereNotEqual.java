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

public class WhereNotEqual extends WhereExpression {
	public WhereNotEqual(Object a, Object b) {
		super("NotEqual");
		
		this.addValue("A", a);
		this.addValue("B", b);
	}
	
	public WhereNotEqual(IWhereField a, Object b) {
		super("NotEqual");
		
		this.addField("A", a);
		this.addValue("B", b);
	}
	
	public WhereNotEqual(Object a, IWhereField b) {
		super("NotEqual");
		
		this.addValue("A", a);
		this.addField("B", b);
	}
	
	public WhereNotEqual(IWhereField a, IWhereField b) {
		super("NotEqual");
		
		this.addField("A", a);
		this.addField("B", b);
	}
}
