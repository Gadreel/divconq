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

import divconq.lang.op.OperationResult;
import divconq.xml.XElement;

public class Schema {
	public SchemaManager manager = null;
	
	// used with includes as well
	public void loadSchema(OperationResult or, XElement def) {
		if (def == null)
			return;
		
		XElement shared = def.find("Shared");
		
		if (shared != null) {
			for (XElement dtel : shared.selectAll("*")) {
				this.manager.loadDataType(or, this, dtel);
			}			
		}
		
		XElement db = def.find("Database");
		
		if (db != null) 
			this.manager.getDb().load(or, this, db);
		
		
		XElement ser = def.find("Services");
		
		if (ser != null) 
			this.manager.getService().load(or, this, ser);
	}

}
