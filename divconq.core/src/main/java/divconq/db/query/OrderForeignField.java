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

import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;

/**
 * A database field to order results in a query.
 * Field may be formated and also have a direction.
 * 
 * @author Andy
 *
 */
public class OrderForeignField implements IOrderField {
	protected RecordStruct column = new RecordStruct();
	
	/**
	 * @param field name of foreign key field
	 * @param foreignfield name of foreign field to use for value
	 */
	public OrderForeignField(String field, String foreignfield) {
		this.column.setField("Field", field);
		this.column.setField("ForeignField", foreignfield);
	}
	
	/**
	 * @param field name of foreign key field
	 * @param foreignfield name of foreign field to use for value
	 * @param direction if this field should be traversed
	 */
	public OrderForeignField(String field, String foreignfield, OrderAs direction) {
		this(field, foreignfield);
		
		if (direction != null)
			this.column.setField("Name", direction.toString());
	}
	
	/**
	 * @param field name of foreign key field
	 * @param foreignfield name of foreign field to use for value
	 * @param direction if this field should be traversed
	 * @param format formatting for return value
	 */
	public OrderForeignField(String field, String foreignfield, OrderAs direction, String format) {
		this(field, foreignfield, direction);
		
		if (StringUtil.isNotEmpty(format))
			this.column.setField("Format", format);
	}
	
	/* (non-Javadoc)
	 * @see divconq.db.query.ISelectItem#getSelection()
	 */
	@Override
	public Struct getParams() {
		return this.column;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.column.toString();
	}
}
