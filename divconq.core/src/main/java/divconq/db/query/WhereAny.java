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

public class WhereAny extends WhereExpression {
	public WhereAny(Object a, Object b) {
		super("Any");
		
		this.addValue("A", a);
		this.addValue("B", b);
	}
	
	public WhereAny(IWhereField a, Object b) {
		super("Any");
		
		this.addField("A", a);
		this.addValue("B", b);
	}
	
	public WhereAny(Object a, IWhereField b) {
		super("Any");
		
		this.addValue("A", a);
		this.addField("B", b);
	}
	
	public WhereAny(IWhereField a, IWhereField b) {
		super("Any");
		
		this.addField("A", a);
		this.addField("B", b);
	}
}
