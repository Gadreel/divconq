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

import divconq.struct.Struct;

public class SqlSelectBoolean extends SqlSelect {
	public SqlSelectBoolean(String sql) {
		super(sql, sql);
	}
	
	public SqlSelectBoolean(String sql, Boolean defaultvalue) {
		super(sql, defaultvalue);
	}
	
	public SqlSelectBoolean(String sql, String name, Boolean defaultvalue) {
		super(sql, name, defaultvalue);
	}

	@Override
	public Object format(Object v) {
		v = Struct.objectToBoolean(v);
		
		if (v != null)
			return v;

		return this.defaultvalue;
	}
}
