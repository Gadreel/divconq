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

/**
 * Assemble a generic Update request for the database.  An update request should 
 * result in an update/alteration to data within the database (other than temp tables 
 * and caches).  There can be some auditing overhead with an update request, so use
 * QueryRequest when no data will be altered to fulfill this request.  
 * Other than that restriction this class can call nearly any stored procedure if 
 * the parameters are assembled correctly.
 * 
 * @author Andy
 *
 */
public class ReplicatedDataRequest extends DataRequest {
	/**
	 * Build an unguided update request.
	 * 
	 * @param proc procedure name to call
	 */
	public ReplicatedDataRequest(String proc) {
		super(proc);
	}
	
	@Override
	public boolean isReplicate() {
		return true;
	}
}
