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

import divconq.struct.CompositeStruct;
import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;

abstract public class WhereExpression {
	protected RecordStruct params = new RecordStruct();
	
	public WhereExpression(String name) {
		this.params.setField("Expression", name);
	}
	
	public void addValue(String part, Object v) {
		this.params.setField(part, new RecordStruct(new FieldStruct("Value", v)));
	}
	
	public void addField(String part, IWhereField v) {
		if (v != null)
			this.params.setField(part, v.getParams());
	}
	
	public CompositeStruct getFields() {
		return this.params;
	}
}
