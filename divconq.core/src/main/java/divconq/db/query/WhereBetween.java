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

public class WhereBetween extends WhereExpression {
	public WhereBetween(Object a, Object b, Object c) {
		super("Between");
		
		this.addValue("A", a);
		this.addValue("B", b);
		this.addValue("C", c);
	}
	
	public WhereBetween(IWhereField a, Object b, Object c) {
		super("Between");
		
		this.addField("A", a);
		this.addValue("B", b);
		this.addValue("C", c);
	}
	
	public WhereBetween(Object a, IWhereField b, Object c) {
		super("Between");
		
		this.addValue("A", a);
		this.addField("B", b);
		this.addValue("C", c);
	}
	
	public WhereBetween(Object a, Object b, IWhereField c) {
		super("Between");
		
		this.addValue("A", a);
		this.addValue("B", b);
		this.addField("C", c);
	}
		
	public WhereBetween(IWhereField a, Object b, IWhereField c) {
		super("Between");
		
		this.addField("A", a);
		this.addValue("B", b);
		this.addField("C", c);
	}
	
	public WhereBetween(IWhereField a, IWhereField b, Object c) {
		super("Between");
		
		this.addField("A", a);
		this.addField("B", b);
		this.addValue("C", c);
	}
	
	public WhereBetween(Object a, IWhereField b, IWhereField c) {
		super("Between");
		
		this.addValue("A", a);
		this.addField("B", b);
		this.addField("C", c);
	}
	
	public WhereBetween(IWhereField a, IWhereField b, IWhereField c) {
		super("Between");
		
		this.addField("A", a);
		this.addField("B", b);
		this.addField("C", c);
	}
}
