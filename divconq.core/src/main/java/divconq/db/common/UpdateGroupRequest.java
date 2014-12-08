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

/**
 * Update a group record.  Name is required.
 * 
 * @author Andy
 *
 */
public class UpdateGroupRequest extends UpdateRecordRequest {
	protected ConditionalValue name = new ConditionalValue();
	protected ConditionalValue desc = new ConditionalValue();
			
	public void setName(String v) {
		this.name.setValue(v);
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
		
		this.parameters = super.buildParams();	
		
		return this.parameters;
	}
}
