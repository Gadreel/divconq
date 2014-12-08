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

/**
 * Points to a value for use in ordering results,  value comes from one of the Select fields
 * saving effort in looking up a value twice.
 * 
 * @author Andy
 *
 */
public class OrderLinked implements IOrderField {
	protected RecordStruct column = new RecordStruct();
	
	/**
	 * @param name of the field in Select (must be a Name, not a Field) 
	 */
	public OrderLinked(String name) {
		this.column.setField("Name", name);
	}
	
	/**
	 * @param name of the field in Select (must be a Name, not a Field) 
	 * @param direction if this field should be traversed
	 */
	public OrderLinked(String name, OrderAs direction) {
		this(name);
		
		if (direction != null)
			this.column.setField("Direction", direction.toString());
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
