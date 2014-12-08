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
 * A database field to select in a query.
 * Field may be formated and also may hold a display name.
 * 
 * @author Andy
 *
 */
public class SelectReverseForeignField implements ISelectField {
	protected RecordStruct column = new RecordStruct();
	
	public SelectReverseForeignField withField(String v) {
		this.column.setField("Field", v);		
		return this;
	}
	
	public SelectReverseForeignField withName(String v) {
		this.column.setField("Name", v);		
		return this;
	}
	
	public SelectReverseForeignField withTable(String v) {
		this.column.setField("Table", v);		
		return this;
	}
	
	public SelectReverseForeignField withKeyField(String v) {
		this.column.setField("KeyField", v);		
		return this;
	}
	
	public SelectReverseForeignField withForeignField(String v) {
		this.column.setField("ForeignField", v);		
		return this;
	}
	
	public SelectReverseForeignField withFormat(String v) {
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
