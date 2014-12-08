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
import divconq.util.StringUtil;

/**
 * TODO add support in M
 * 
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
	
	/**
	 * @param name of indexed field
	 * @param values to use as parameters for script 
	 */
	public CollectorField(String name, Collection<String> values) {
		this.column.setField("Field", name);
		
		if (values != null) {
			ListStruct list = new ListStruct();
			
			for (String v : values)
				list.addItem(v);
			
			this.column.setField("Values", list);
		}
	}
	
	/**
	 * @param name of indexed field
	 * @param values to use as parameters for script 
	 */
	public CollectorField(String name, String from, String to) {
		this.column.setField("Field", name);
		
		if (StringUtil.isNotEmpty(from)) 
			this.column.setField("From", from);
		
		if (StringUtil.isNotEmpty(to)) 
			this.column.setField("To", to);
	}
	
	/* (non-Javadoc)
	 * @see divconq.db.query.ICollector#getParams()
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
