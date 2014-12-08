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

public class WhereContains extends WhereExpression {
	public WhereContains(Object a, Object b) {
		super("Contains");
		
		this.addValue("A", a);
		this.addValue("B", b);
	}
	
	public WhereContains(IWhereField a, Object b) {
		super("Contains");
		
		this.addField("A", a);
		this.addValue("B", b);
	}
	
	public WhereContains(Object a, IWhereField b) {
		super("Contains");
		
		this.addValue("A", a);
		this.addField("B", b);
	}
	
	public WhereContains(IWhereField a, IWhereField b) {
		super("Contains");
		
		this.addField("A", a);
		this.addField("B", b);
	}
}
