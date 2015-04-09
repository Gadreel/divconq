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

import divconq.struct.ListStruct;

abstract public class WhereGroupExpression extends WhereExpression {
	public WhereGroupExpression(String name, WhereExpression... list) {
		super(name);
		
		ListStruct dlst = new ListStruct();						
		
		for (WhereExpression ex : list) 
			dlst.addItem(ex.getFields());

		this.params.setField("Children",  dlst); 
	}
	
	public void addWhere(WhereExpression ex) {
		this.params.getFieldAsList("Children").addItem(ex.getFields()); 
	}
}
