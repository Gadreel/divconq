/* ************************************************************************
#
#  DivConq
#
#  http://divconq.com/
#
#  Copyright:
#    Copyright 2012 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package divconq.db;

import divconq.struct.CompositeStruct;

/**
 * All requests to the dcDb must implement this interface.  Each request is required
 * to provide essentials details to the database.
 *  
 * @author Andy
 *
 */
public interface IDatabaseRequest {
	/**
	 * A request is composed of a procedure name and a collection of
	 * java objects (CompositeStruct) as parameters.  Parameters
	 * are either a ListStruct or a RecordStruct.
	 * 
	 * @return parameters for this request
	 */
	CompositeStruct buildParams();
	
	/**
	 * Does this request make any updates to the database, or is it purely
	 * a query. Most updates and such need to be replicated 
	 * 
	 * @return true if data is (supposed to be) altered by this request
	 */
	boolean isReplicate();
	
	/**
	 * @return the name of the stored procedure to call
	 */
	String getProcedure();

	/**
	 * Most calls to the database are for the current domain, but some calls are for other
	 * domains.  A request can mark a request destined for another domain using this property.
	 * 
	 * @return true if this request is root domain specific
	 */
	boolean hasDomain();
	
	String getDomain();
}
