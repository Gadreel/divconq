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

import divconq.db.update.ConditionalValue;
import divconq.db.update.InsertRecordRequest;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;

/**
 * Insert a new group record into dcDatabase.  Name is required.
 * 
 * @author Andy
 *
 */
public class AddGroupRequest extends InsertRecordRequest {
	protected ConditionalValue name = new ConditionalValue();
	protected ConditionalValue desc = new ConditionalValue();
	protected ListStruct tags = null;
			
	public void setName(String v) {
		this.name.setValue(v);
	}
	
	public void setDescription(String v) {
		this.desc.setValue(v);
	}
	
	public void setAuthorizationTags(ListStruct tags) {
		this.tags = tags;
	}
	
	public void addAuthorizationTag(String... tags) {
		if (this.tags == null) 
			this.tags = new ListStruct();
		
		for (String name : tags)
			this.tags.addItem(name);
	}
	
	public AddGroupRequest(String name) {
		this.withTable("dcGroup");
		this.setName(name);
	}
	
	@Override
	public CompositeStruct buildParams() {
		this.withSetField("dcName", this.name);
		this.withSetField("dcDescription", this.desc);
		this.withSetList("dcAuthorizationTag", this.tags);
		
		this.parameters = super.buildParams();	
		
		return this.parameters;
	}
}
