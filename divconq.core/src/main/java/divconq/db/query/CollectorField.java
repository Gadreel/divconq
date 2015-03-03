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
import divconq.struct.RecordStruct;
import divconq.struct.Struct;

/**
 * Names the indexed field to use to provide record ids to a Select or List request.  
 * 
 * @author Andy
 *
 */
public class CollectorField implements ICollector {
	protected RecordStruct column = new RecordStruct();
	
	/**
	 * Same as From - To but inclusive.
	 * 
	 * @param name of indexed field
	 */
	public CollectorField(String name) {
		this.column.setField("Field", name);
	}
	
	public CollectorField withValues(Object... values) {
		if (values != null) {
			ListStruct list = new ListStruct();
			
			for (Object v : values)
				list.addItem(v);
			
			this.column.setField("Values", list);
		}
		
		return this;
	}
	
	public CollectorField withFrom(Object v) {
		this.column.setField("From", v);
		
		return this;
	}
	
	public CollectorField withTo(Object v) {
		this.column.setField("To", v);
		
		return this;
	}
	
	@Override
	public Struct getParams() {
		return this.column;
	}
	
	@Override
	public String toString() {
		return this.column.toString();
	}
}
