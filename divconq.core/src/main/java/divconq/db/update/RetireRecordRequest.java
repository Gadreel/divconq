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

import divconq.db.ReplicatedDataRequest;
import divconq.struct.RecordStruct;

/**
 * dcDatabase does not remove records, but it does have the concept of retiring a record.
 * A retired record will not show up in a standard DivConq query, though of course
 * you can code a query to use it because the data is all there.
 * 
 * @author Andy
 *
 */
public class RetireRecordRequest extends ReplicatedDataRequest {
	/**
	 * @param table name
	 * @param id of record
	 */
	public RetireRecordRequest(String table, String id) {
		super("dcRetireRecord");
		
		RecordStruct params = new RecordStruct();
		
		this.parameters = params;
		
		params.setField("Table", table);		
		params.setField("Id", id);		
	}
}
