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
package divconq.db.common;

import divconq.db.DataRequest;
import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;

/**
 * Get the user id for a given user name.
 * 
 * @author Andy
 *
 */
public class UsernameLookupRequest extends DataRequest {
	protected String name = null;

	/**
	 * @return user name used in query
	 */
	public String getUsername() {
		return this.name;
	}
	
	/**
	 * @param v user name used in query
	 */
	public void setUsername(String v) {
		this.name = v;
	}
	
	/**
	 * @param username for use in query
	 */
	public UsernameLookupRequest(String username) {
		super("dcUsernameLookup");
		this.name = username;
	}
	
	/* (non-Javadoc)
	 * @see divconq.db.IDatabaseRequest#getDatabaseParams()
	 */
	@Override
	public RecordStruct buildParams() {
		return new RecordStruct(new FieldStruct("Username", (this.name != null) ? this.name.trim().toLowerCase() : null));
	}
}
