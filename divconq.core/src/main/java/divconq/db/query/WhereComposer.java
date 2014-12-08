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
 * A Composer script to generate values to filter on.
 * 
 * @author Andy
 *
 */
public class WhereComposer implements IWhereField {
	protected RecordStruct column = new RecordStruct();
	
	/**
	 * @param composer name of content generating script
	 */
	public WhereComposer(String composer) {
		this.column.setField("Composer", composer);
	}
	
	/**
	 * @param composer name of content generating script
	 * @param format formatting for return value
	 */
	public WhereComposer(String composer, String format) {
		this.column.setField("Composer", composer);
		
		if (StringUtil.isNotEmpty(format))
			this.column.setField("Format", format);
	}
	
	/* (non-Javadoc)
	 * @see divconq.db.query.IWhereField#getParams()
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
