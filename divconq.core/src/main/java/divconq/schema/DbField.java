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
package divconq.schema;

import divconq.db.Constants;

public class DbField {
	/*
	 * Types recognized by M:
	 * 
	 * Time
	 * Date
	 * DateTime
	 * BigDateTime
	 * String
	 * Id
	 * Integer
	 * Decimal
	 * BigInteger
	 * BigDecimal (aka Number)
	 * Number
	 * Boolean
	 * Binary  (really hex for in M)
	 * Json
	 */

	protected String name = null;
	protected boolean dynamic = false;
	protected boolean list = false;
	protected String type = null;
	protected String typeid = null;
	protected boolean required = false;
	protected boolean indexed = false;
	protected boolean unique = false;
	protected String fkey = null;
	
	public boolean isList() {
		return this.list;
	}

	public boolean isDynamic() {
		return this.dynamic;
	}

	public boolean isIndexed() {
		return this.indexed || this.unique;		// uniques have to be indexed
	}

	public boolean isRequired() {
		return this.required;
	}

	public boolean isUnique() {
		return this.unique;
	}
	
	public String getName() {
		return this.name;
	}

	public String getType() {
		return this.type;
	}

	public String getTypeId() {
		return this.typeid;
	}
	
	public String getForeignKey() {
		return this.fkey;
	}

	public boolean isStaticScalar() {
		return (!this.list && !this.dynamic);
	}

	public boolean isStaticList() {
		return (this.list && !this.dynamic);
	}

	public boolean isDynamicScalar() {
		return (!this.list && this.dynamic);
	}

	public boolean isDynamicList() {
		return (this.list && this.dynamic);
	}
	
	public String getIndexName() {
		return (!this.list && !this.dynamic) ? Constants.DB_GLOBAL_INDEX : Constants.DB_GLOBAL_INDEX_SUB;
	}
	
}
