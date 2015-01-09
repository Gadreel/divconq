/* ************************************************************************
#
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
import divconq.lang.op.OperationContext;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.util.StringUtil;

/**
 * Insert a new user record into dcDatabase.  Username is required.
 * 
 * @author Andy
 *
 */
public class UpdateUserRequest extends UpdateRecordRequest {
	protected ConditionalValue username = new ConditionalValue();
	protected ConditionalValue firstname = new ConditionalValue();
	protected ConditionalValue lastname = new ConditionalValue();
	protected ConditionalValue email = new ConditionalValue();
	protected ConditionalValue backupemail = new ConditionalValue();
	protected ConditionalValue password = new ConditionalValue();
	protected ConditionalValue locale = new ConditionalValue();
	protected ConditionalValue chrono = new ConditionalValue();
	protected ConditionalValue confirmed = new ConditionalValue();
	protected ConditionalValue confirmcode = new ConditionalValue();
	protected ConditionalValue desc = new ConditionalValue();
	protected ListStruct tags = null;
			
	public void setUsername(String v) {
		this.username.setValue(v);
	}
	
	public void setDescription(String v) {
		this.desc.setValue(v);
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
	
	public void setFirstName(String v) {
		this.firstname.setValue(v);
	}
	
	public void setLastName(String v) {
		this.lastname.setValue(v);
	}
	
	public void setPassword(String v) {
		this.password.setValue(v);
	}
	
	public void setEmail(String v) {
		this.email.setValue(v);
	}
	
	public void setBackupEmail(String v) {
		this.backupemail.setValue(v);
	}
	
	public void setLocale(String v) {
		this.locale.setValue(v);
	}
	
	public void setChronology(String v) {
		this.chrono.setValue(v);
	}
	
	public void setConfirmed(boolean v) {
		this.confirmed.setValue(v);
		
		if (v)
			this.confirmcode.clear();
		else
			this.confirmcode.setValue(StringUtil.buildSecurityCode());
	}
	
	public void setConfirmCode(String v) {
		this.confirmcode.setValue(v);
	}

	/**
	 * @return recovery code for user
	 */
	public String getConfirmCode() {
		return (String)this.confirmcode.getValue();
	}
	
	public UpdateUserRequest(String id) {
		this.withTable("dcUser");		
		this.withId(id);
		
		this.filter = "dcIsAccountTaken";
	}
	
	@Override
	public CompositeStruct buildParams() {		
		if (this.username.isSet())
			this.withSetField("dcUsername", ((String) this.username.getValue()).trim().toLowerCase());
		
		this.withSetField("dcFirstName", this.firstname);
		this.withSetField("dcLastName", this.lastname);
		this.withSetField("dcEmail", this.email);
		this.withSetField("dcBackupEmail", this.backupemail);
		this.withSetField("dcDescription", this.desc);
		this.withSetField("dcLocale", this.locale);
		this.withSetField("dcChronology", this.chrono);
		this.withSetField("dcConfirmed", this.confirmed);
		this.withSetField("dcConfirmCode", this.confirmcode);
		
		// password crypto 
		if (this.password.isSet()) {
			String pword = ((String) this.password.getValue()).trim();
			
			if (AddUserRequest.meetsPasswordPolicy(pword, false).hasErrors())
				return null;		
			
			this.withSetField("dcPassword", OperationContext.get().getUserContext().getDomain().getObfuscator().hashStringToHex(pword));
		}
		
		// warning - setting an empty list removes all tags
		if (this.tags != null)
			this.withReplaceList("dcAuthorizationTag", this.tags);
		
		return super.buildParams();	
	}
}
