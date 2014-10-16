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

public class SqlSelectDateTime extends SqlSelect {
	public SqlSelectDateTime(String sql, DateTime defaultvalue) {
		super(sql, defaultvalue);
	}
	
	public SqlSelectDateTime(String sql, String name, DateTime defaultvalue) {
		super(sql, name, defaultvalue);
	}

	@Override
	public Object format(Object v) {
		v = Struct.objectToDateTime(v);
		
		if (v != null)
			return v;

		return this.defaultvalue;
	}
}
