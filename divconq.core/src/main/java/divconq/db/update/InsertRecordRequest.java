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
package divconq.db.update;

/**
 * Insert a new record into dcDatabase, see dcInsertRecord schema.
 * 
 * @author Andy
 *
 */
public class InsertRecordRequest extends DbRecordRequest {
	public InsertRecordRequest() {
		super("dcInsertRecord");
	}
	
	public InsertRecordRequest(String proc) {
		super(proc);
	}
}
