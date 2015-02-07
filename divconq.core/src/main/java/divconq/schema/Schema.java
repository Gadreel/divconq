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

import divconq.xml.XElement;

public class Schema {
	protected SchemaManager manager = null;
	protected String file = null;
	
	public Schema(String pathname, SchemaManager manager) {
		this.manager = manager;
		this.file = pathname;
	}
	
	// used with includes as well
	public void loadSchema(XElement def) {
		if (def == null)
			return;
		
		XElement shared = def.find("Shared");
		
		if (shared != null) {
			for (XElement dtel : shared.selectAll("*")) {
				this.manager.loadDataType(this, dtel);
			}			
		}
		
		XElement db = def.find("Database");
		
		if (db != null) 
			this.manager.loadDb(this, db);
		
		
		XElement ser = def.find("Services");
		
		if (ser != null) 
			this.manager.loadService(this, ser);
	}

}
