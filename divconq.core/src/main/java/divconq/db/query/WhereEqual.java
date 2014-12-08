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

public class WhereEqual extends WhereExpression {
	public WhereEqual(Object a, Object b) {
		super("Equal");
		
		this.addValue("A", a);
		this.addValue("B", b);
	}
	
	public WhereEqual(IWhereField a, Object b) {
		super("Equal");
		
		this.addField("A", a);
		this.addValue("B", b);
	}
	
	public WhereEqual(Object a, IWhereField b) {
		super("Equal");
		
		this.addValue("A", a);
		this.addField("B", b);
	}
	
	public WhereEqual(IWhereField a, IWhereField b) {
		super("Equal");
		
		this.addField("A", a);
		this.addField("B", b);
	}
}
