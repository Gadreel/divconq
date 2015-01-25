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

import divconq.xml.XElement;

/**
 * Manages one or more connections to a dcDb compatible server.
 * 
 * Database Requests (IDatabaseRequest) are submitted asynchronously.  After a request has been processed 
 * a callback is triggered letting the calling code know that a result is available.
 * 
 * Database results are typically collected into an object structure (see CompositeStruct) and
 * returned in one large lump.  As an alternative database results may be processed as a stream, 
 * see ICompositeBuilder and JsonStreamBuilder.
 * 
 * See divconq.test.TestDb for usage examples.
 * 
 * @author Andy
 *
 */
public interface IDatabaseManager {
	/**
	 * 
	 * @param config configuration for this database pool
	 */
	void init(XElement config);
	
	/**
	 * Online full operation, including doing backups
	 */
	void start();
	
	/**
	 * Gracefully stop the database pool, allowing for up to 1 minute for shutdown.
	 */
	void stop();
	
	/**
	 * Asynchronously submit a database request for processing.  If the submit succeeds then look
	 * for the result (even if in error) to come to the callback.  If submit does not succeed then
	 * look for the result to come from the method return, and callback will not be called. 
	 *  
	 *  @param request object to be processed
	 *  @param cb the callback to use when database response is complete
	 */
	void submit(IDatabaseRequest request, DatabaseResult cb);	
}
