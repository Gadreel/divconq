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

	public String name = null;
	public String type = null;
	public String typeid = null;
	public boolean required = false;
	public boolean indexed = false;
	public boolean dynamic = false;
	public boolean list = false;
	public String fkey = null;

}
