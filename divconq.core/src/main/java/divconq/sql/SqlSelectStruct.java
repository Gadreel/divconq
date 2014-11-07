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

import divconq.lang.op.FuncResult;
import divconq.struct.CompositeParser;
import divconq.struct.CompositeStruct;
import divconq.struct.Struct;

public class SqlSelectStruct extends SqlSelect {
	public SqlSelectStruct(String sql, Struct defaultvalue) {
		super(sql, defaultvalue);
	}
	
	public SqlSelectStruct(String sql, String name, Struct defaultvalue) {
		super(sql, name, defaultvalue);
	}

	@Override
	public Object format(Object v) {
		if (v != null) {
			FuncResult<CompositeStruct> jsonres = CompositeParser.parseJson(Struct.objectToString(v));
			
			if (jsonres.hasErrors())
				return null;
			
			return jsonres.getResult();
		}
		
		return this.defaultvalue;
	}
}
