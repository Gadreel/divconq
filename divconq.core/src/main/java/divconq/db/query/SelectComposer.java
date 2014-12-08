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
 * A Composer script to generate content in a query.
 * 
 * @author Andy
 *
 */
public class SelectComposer implements ISelectField {
	protected RecordStruct column = new RecordStruct();
	
	public SelectComposer withComposer(String v) {
		this.column.setField("Composer", v);		
		return this;
	}
	
	public SelectComposer withName(String v) {
		this.column.setField("Name", v);		
		return this;
	}
	
	public SelectComposer withFormat(String v) {
		this.column.setField("Format", v);		
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
