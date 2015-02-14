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
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.util.StringUtil;

/**
 * Insert a new user record into dcDatabase.  Username is required.
 * 
 * @author Andy
 *
 */
public class AddUserRequest extends InsertRecordRequest {
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
	
	public AddUserRequest(String username) {
		//this.filter = "dcIsAccountTaken";

		this.withTable("dcUser");
		this.setUsername(username);
		this.setConfirmed(true);
	}
	
	public AddUserRequest(String username, String firstname, String lastname, String email, String password) {
		this(username);
		
		this.setFirstName(firstname);
		this.setLastName(lastname);
		this.setEmail(email);
		this.setPassword(password);
	}
	
	public AddUserRequest(String username, String firstname, String lastname, String email, String password, boolean confirmed) {
		this(username, firstname, lastname, email, password);
		
		this.setConfirmed(confirmed);
	}
	
	public AddUserRequest(String username, String firstname, String lastname, String email, String password, boolean confirmed, String locale, String chronology) {
		this(username, firstname, lastname, email, password, confirmed);
		
		this.setLocale(locale);
		this.setChronology(chronology);
	}
	
	@Override
	public CompositeStruct buildParams() {
		String uname = this.username.isSet() ? ((String) this.username.getValue()).trim().toLowerCase() : null;
		
		if (StringUtil.isEmpty(uname)) {
			OperationContext.get().errorTr(127);
			return null;
		}
		
		String pword = ((String) this.password.getValue()).trim();
		
		if (AddUserRequest.meetsPasswordPolicy(pword, false).hasErrors())
			return null;		
		
		this.withSetField("dcUsername", uname);
		this.withSetField("dcFirstName", this.firstname);
		this.withSetField("dcLastName", this.lastname);
		this.withSetField("dcEmail", this.email);
		this.withSetField("dcBackupEmail", this.backupemail);
		this.withSetField("dcDescription", this.desc);
		this.withSetField("dcLocale", this.locale);
		this.withSetField("dcChronology", this.chrono);
		this.withSetField("dcConfirmed", this.confirmed);
		this.withSetField("dcConfirmCode", this.confirmcode);
		
		// this works for insert, but Set approach works with both insert and update - see UpdateUserRequest
		this.withSetList("dcAuthorizationTag", this.tags);
		
		// password crypto 
		if (this.password.isSet())
			this.withSetField("dcPassword", OperationContext.get().getUserContext().getDomain().getObfuscator().hashStringToHex(pword));
		
		return super.buildParams();	
	}
	
	/**
	 * Checks that a given password meets the applications password policy.  
	 * 
	 * @param password proposed password
	 * @param warnMode produces warnings instead of errors
	 */
	static public OperationResult meetsPasswordPolicy(String password, boolean warnMode) {
		OperationResult or = new OperationResult();
		
		// TODO make this into a configurable beast
		
		if (StringUtil.isEmpty(password)) {
			if (warnMode)
				or.warnTr(125);
			else
				or.errorTr(125);
			
			return or;
		}
		
		if (password.length() < 6) {
			if (warnMode)
				or.warnTr(126);
			else
				or.errorTr(126);
		}
		
		if (AddUserRequest.isSuspectPassword(password)) {
			if (warnMode)
				or.warnTr(135);
			else
				or.errorTr(135);
		}
		
		return or;
	}
	
	/*
	 * trying to track hack logins - create a list of suspect passwords TODO configurable
	 * 
	 * current list from http://www.splashdata.com/press/PR121023.htm which has a yearly list
	 */
	
	static public boolean isSuspectPassword(String pass) {
		pass = pass.trim().toLowerCase();
		
		if ("password".equals(pass) || "123456".equals(pass) || "12345678".equals(pass) || "abc123".equals(pass) ||
				 "qwerty".equals(pass) || "monkey".equals(pass) || "letmein".equals(pass) || "dragon".equals(pass))
			return true;
		
		if ("111111".equals(pass) || "baseball".equals(pass) || "iloveyou".equals(pass) || "trustno1".equals(pass) ||
				 "1234567".equals(pass) || "sunshine".equals(pass) || "master".equals(pass) || "123123".equals(pass))
			return true;
		
		if ("welcome".equals(pass) || "shadow".equals(pass) || "ashley".equals(pass) || "football".equals(pass) ||
				 "jesus".equals(pass) || "michael".equals(pass) || "ninja".equals(pass) || "mustang".equals(pass))
			return true;
		
		if ("password1".equals(pass) || "temp123".equals(pass) || "a1s2d3f4".equals(pass) || "a1s2d3".equals(pass))
			return true;
		
		return false;		
	}
}
