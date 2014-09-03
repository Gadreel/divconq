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

public class SqlSelectTransformer extends SqlSelect {
	protected ISqlFormater formatter = null;
	
	public SqlSelectTransformer(String sql, ISqlFormater formatter) {
		super(sql, sql, null);
		this.formatter = formatter;
	}
	
	public SqlSelectTransformer(String sql, String name, ISqlFormater formatter) {
		super(sql, name, null);
		this.formatter = formatter;
	}

	@Override
	public Object format(Object v) {
		if (this.formatter == null)
			return this.defaultvalue;

		return this.formatter.format(v);
	}
}
