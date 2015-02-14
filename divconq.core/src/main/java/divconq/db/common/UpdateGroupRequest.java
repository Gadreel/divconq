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
import divconq.db.update.UpdateRecordRequest;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;

/**
 * Update a group record.  Name is required.
 * 
 * @author Andy
 *
 */
public class UpdateGroupRequest extends UpdateRecordRequest {
	protected ConditionalValue name = new ConditionalValue();
	protected ConditionalValue desc = new ConditionalValue();
	protected ListStruct tags = null;
			
	public void setName(String v) {
		this.name.setValue(v);
	}
	
	public void setAuthorizationTags(ListStruct tags) {
		this.tags = tags;
	}
	
	public void emptyAuthorizationTags(ListStruct tags) {
		this.tags = new ListStruct();
	}
	
	public void addAuthorizationTag(String... tags) {
		if (this.tags == null) 
			this.tags = new ListStruct();
		
		for (String name : tags)
			this.tags.addItem(name);
	}
	
	public void setDescription(String v) {
		this.desc.setValue(v);
	}
	
	public UpdateGroupRequest(String id) {
		this.withTable("dcGroup");
		this.withId(id);
	}
	
	@Override
	public CompositeStruct buildParams() {
		this.withSetField("dcName", this.name);
		this.withSetField("dcDescription", this.desc);

		// warning - setting an empty list removes all tags
		if (this.tags != null)
			this.withSetList("dcAuthorizationTag", this.tags);
		
		this.parameters = super.buildParams();	
		
		return this.parameters;
	}
}
