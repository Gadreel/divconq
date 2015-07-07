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
 * Names the collector procedure to use to provide record ids to a Select or List request.  
 * 
 * @author Andy
 *
 */
public class CollectorFunc implements ICollector {
	protected RecordStruct column = new RecordStruct();
	
	/**
	 * @param name of id looping script
	 */
	public CollectorFunc(String name) {
		this.column.setField("Func", name);
	}
	
	public CollectorFunc withValues(Object... values) {
		if (values != null) {
			ListStruct list = new ListStruct();
			
			for (Object v : values)
				list.addItem(v);
			
			this.column.setField("Values", list);
		}
		
		return this;
	}
	
	public CollectorFunc withValues(ListStruct values) {
		this.column.setField("Values", values);
		
		return this;
	}
	
	public CollectorFunc withExtra(RecordStruct v) {
		this.column.setField("Extras", v);
		
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
