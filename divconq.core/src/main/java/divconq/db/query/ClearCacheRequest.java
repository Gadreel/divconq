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
package divconq.db.query;

import divconq.db.DataRequest;
import divconq.struct.RecordStruct;

/**
 * Clears the cache for a given cache id.
 * 
 * @author Andy
 *
 */
public class ClearCacheRequest extends DataRequest {
	/**
	 * @param id of query cache
	 */
	public ClearCacheRequest(String id) {
		super("dcClearCache");
		
		RecordStruct params = new RecordStruct();
		params.setField("CacheId", id);		
		
		this.parameters = params;
	}
}
