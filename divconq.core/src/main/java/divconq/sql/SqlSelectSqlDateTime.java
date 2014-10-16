/* ************************************************************************
#
#  DivConq
#
#  http://divconq.com/
#
#  Copyright:
#    Copyright 2014 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package divconq.sql;

import org.joda.time.DateTime;

import divconq.struct.Struct;
import divconq.util.TimeUtil;

public class SqlSelectSqlDateTime extends SqlSelect {
	protected boolean sqlFmt = false;
	
	public SqlSelectSqlDateTime(String sql, DateTime defaultvalue) {
		super(sql, defaultvalue);
	}
	
	public SqlSelectSqlDateTime(String sql, DateTime defaultvalue, boolean sqlFmt) {
		super(sql, defaultvalue);
		this.sqlFmt = sqlFmt;
	}
	
	public SqlSelectSqlDateTime(String sql, String name, DateTime defaultvalue) {		
		super(sql, name, defaultvalue);
	}
	
	public SqlSelectSqlDateTime(String sql, String name, DateTime defaultvalue, boolean sqlFmt) {		
		super(sql, name, defaultvalue);
		this.sqlFmt = sqlFmt;
	}

	@Override
	public Object format(Object v) {
		DateTime tv = Struct.objectToDateTime(v);
		
		if (tv != null)
			return this.sqlFmt ? TimeUtil.sqlStampFmt.print(tv) : tv;   
		
		return this.defaultvalue;		
	}
}
