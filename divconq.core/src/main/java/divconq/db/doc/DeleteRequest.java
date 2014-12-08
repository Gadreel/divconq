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
package divconq.db.doc;

import divconq.db.ReplicatedDataRequest;

public class DeleteRequest extends ReplicatedDataRequest {
	public DeleteRequest(String id) {
		this(id, null);
	}
	
	public DeleteRequest(String id, String path) {
		super("dcDeleteDocument");
		
		/* TODO
		this.parameters.setField("Id", id);		
		
		if (StringUtil.isNotEmpty(path))
			this.parameters.setField("Path", path);
			*/
	}
}
