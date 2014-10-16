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

import divconq.struct.CompositeStruct;
import divconq.struct.Struct;

public class SqlSelectSqlJson extends SqlSelect {
	public SqlSelectSqlJson(String sql, CompositeStruct defaultvalue) {
		super(sql, defaultvalue);
	}
	
	public SqlSelectSqlJson(String sql, String name, CompositeStruct defaultvalue) {
		super(sql, name, defaultvalue);
	}

	@Override
	public Object format(Object v) {
		CompositeStruct tv = Struct.objectToComposite(v);
		
		if (tv == null)
			tv = (CompositeStruct) this.defaultvalue;
		
		return tv;
	}
}
