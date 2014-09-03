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

import java.math.BigDecimal;

import divconq.struct.Struct;

public class SqlSelectDecimal extends SqlSelect {
	public SqlSelectDecimal(String sql) {
		super(sql, sql);
	}
	
	public SqlSelectDecimal(String sql, BigDecimal defaultvalue) {
		super(sql, defaultvalue);
	}
	
	public SqlSelectDecimal(String sql, String name, BigDecimal defaultvalue) {
		super(sql, name, defaultvalue);
	}

	@Override
	public Object format(Object v) {
		v = Struct.objectToDecimal(v);
		
		if (v != null)
			return v;

		return this.defaultvalue;
	}
}
