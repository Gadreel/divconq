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

public class SqlSelectInteger extends SqlSelect {
	public SqlSelectInteger(String sql) {
		super(sql, sql);
	}
	
	public SqlSelectInteger(String sql, Integer defaultvalue) {
		super(sql, defaultvalue);
	}
	
	public SqlSelectInteger(String sql, String name, Integer defaultvalue) {
		super(sql, name, defaultvalue);
	}

	@Override
	public Object format(Object v) {
		v = Struct.objectToInteger(v);
		
		if (v != null)
			return v;

		return this.defaultvalue;
	}
}
