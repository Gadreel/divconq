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
 * A database field to filter results in a query.
 * Field may be formated.
 * 
 * @author Andy
 *
 */
public class WhereField implements IWhereField {
	protected RecordStruct column = new RecordStruct();
	
	/**
	 * @param field field name
	 */
	public WhereField(String field) {
		this.column.setField("Field", field);
	}
	
	/**
	 * @param field field name
	 * @param subid if field is a list and you wish to match just one value
	 */
	public WhereField(String field, String subid) {
		this(field);
		
		if (StringUtil.isNotEmpty(subid))
			this.column.setField("SubId", subid);
	}
	
	public WhereField withFormat(String format) {
		if (StringUtil.isNotEmpty(format))
			this.column.setField("Format", format);
		
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
