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
import java.util.Arrays;

import divconq.sql.SqlManager.SqlDatabase;
import divconq.util.StringUtil;

abstract public class SqlSelect {
	//static public SqlSelect[] selectClause(Object[] fields) {
	//	return Arrays.copyOf(fields, fields.length, SqlSelect[].class);
	//}
	
	static public SqlSelect[] selectClause(Object... fields) {
		return Arrays.copyOf(fields, fields.length, SqlSelect[].class);
	}
	
	static public SqlSelect selectString(String name) {
		return new SqlSelectString(name);
	}
	
	static public SqlSelect selectString(String sql, String name) {
		return new SqlSelectString(sql, name, null);
	}
	
	static public SqlSelect selectString(String sql, String name, String def) {
		return new SqlSelectString(sql, name, def);
	}
	
	static public SqlSelect selectInteger(String name) {
		return new SqlSelectInteger(name);
	}
	
	static public SqlSelect selectInteger(String sql, String name) {
		return new SqlSelectInteger(sql, name, null);
	}
	
	static public SqlSelect selectInteger(String sql, String name, Integer def) {
		return new SqlSelectInteger(sql, name, def);
	}
	
	static public SqlSelect selectDecimal(String name) {
		return new SqlSelectDecimal(name);
	}
	
	static public SqlSelect selectDecimal(String sql, String name) {
		return new SqlSelectDecimal(sql, name, null);
	}
	
	static public SqlSelect selectDecimal(String sql, String name, BigDecimal def) {
		return new SqlSelectDecimal(sql, name, def);
	}
	
	static public SqlSelect selectBoolean(String name) {
		return new SqlSelectBoolean(name);
	}
	
	static public SqlSelect selectBoolean(String sql, String name) {
		return new SqlSelectBoolean(sql, name, null);
	}
	
	static public SqlSelect selectBoolean(String sql, String name, Boolean def) {
		return new SqlSelectBoolean(sql, name, def);
	}
	
	protected String sql = null;
	protected String name = null;
	protected Object defaultvalue = null;
		
	public SqlSelect(String sql, Object defaultvalue) {
		this.sql = sql;
		this.defaultvalue = defaultvalue;
		
		int npos = this.sql.lastIndexOf(' ');
		
		if (npos != -1)
			this.name = sql.substring(npos + 1).trim();
		else
			this.name = this.sql;
	}
	
	public SqlSelect(String sql, String name) {
		this(sql, name, null);
	}
	
	public SqlSelect(String sql, String name, Object defaultvalue) {
		this.sql = sql;
		this.defaultvalue = defaultvalue;
		this.name = name;
	}
	
	abstract public Object format(Object v);

	public String toSql(SqlDatabase db) {
		if (StringUtil.isNotEmpty(this.name))
			return this.sql + " AS " + db.formatColumn(this.name);
		
		return this.sql;
	}
}
