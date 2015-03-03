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

import java.util.Collection;

import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;

/**
 * Names the collector procedure to use to provide record ids to a Select or List request.  
 * 
 * @author Andy
 *
 */
public class CollectorScript implements ICollector {
	protected RecordStruct column = new RecordStruct();
	
	/**
	 * @param name of id looping script
	 */
	public CollectorScript(String name) {
		this.column.setField("Name", name);
	}
	
	/**
	 * @param name of id looping script
	 * @param values to use as parameters for script 
	 */
	public CollectorScript(String name, Collection<String> values) {
		this.column.setField("Name", name);
		
		if (values != null) {
			ListStruct list = new ListStruct();
			
			for (String v : values)
				list.addItem(v);
			
			this.column.setField("Values", list);
		}
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
