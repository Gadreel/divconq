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

public class SqlSelectString extends SqlSelect {
	public SqlSelectString(String sql) {
		super(sql, (Object)null);
	}

	public SqlSelectString(String sql, String defaultvalue) {
		super(sql, (Object)defaultvalue);
	}
	
	public SqlSelectString(String sql, String name, String defaultvalue) {
		super(sql, name, defaultvalue);
	}

	@Override
	public Object format(Object v) {
		v = Struct.objectToString(v);
		
		if (v != null)
			return v;

		return this.defaultvalue;
	}
}
