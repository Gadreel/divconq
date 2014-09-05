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
package divconq.lang;

import divconq.hub.DomainInfo;
import divconq.hub.Hub;
import divconq.locale.LocaleUtil;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;

/**
 * User Context works in conjunction with Task Context and Session to track information about 
 * Authenticated User or Guest User.
 * 
 * @author Andy
 *
 */
public class UserContext {
	static public UserContext allocate(OperationContextBuilder tcb) {
		return new UserContext(tcb.values);
	}
	
	/**
	 * @return create a new guest context
	 */
	static public UserContext allocateGuest() {
		return new OperationContextBuilder().withGuestUserTemplate().toUserContext();
	}
	
	/**
	 * @return create a new root context
	 */
	static public UserContext allocateRoot() {
		return new OperationContextBuilder().withRootUserTemplate().toUserContext();
	}
	
	/**
	 * @param m create a task context from a message (RPC calls to dcBus), keep in mind
	 * this is info gathering only, message must not be allowed to force an 
	 * authenticated/elevated state inappropriately - from RPC clear "Elevated"
	 * field before calling this
	 */
	static public UserContext allocate(RecordStruct m) {
		return new UserContext(m);
	}
	
	static public UserContext allocateFromTask(RecordStruct m) {
		return new UserContext(m.deepCopyExclude("OpId", "SessionId", "Origin", "DebugLevel", "Elevated", "Pool"));
	}
	
	static public OperationContextBuilder checkCredentials(UserContext ctx, RecordStruct v) {
		if (ctx.context.isFieldEmpty("Credentials") && (v == null))
			return null;
		
		// no need to do anything if the creds are the same
		if (!ctx.context.isFieldEmpty("Credentials") && (v != null) && ctx.getCredentials().equals(v))		// TODO the equals is not right for deep records
			return null;
		
		// when credentials change you are left with only guest access
		return ctx.toBuilder().withCredentials(v);
	}

	// instance code
	
	protected RecordStruct context = null;

	/**
	 * @return the id of the user, if guest then "00000_000000000000002", if root then "00000_000000000000001"
	 */
	public String getUserId() {
		return this.context.getFieldAsString("UserId");
	}

	/**
	 * @return the username of the user
	 */
	public String getUserName() {
		return this.context.getFieldAsString("UserName");
	}

	/**
	 * @return the fullname of the user
	 */
	public String getFullName() {
		return this.context.getFieldAsString("FullName");
	}

	/**
	 * @return the primary email of the user
	 */
	public String getEmail() {
		return this.context.getFieldAsString("Email");
	}

	/**
	 * @return the authentication token
	 */
	public String getAuthToken() {
		return this.context.getFieldAsString("AuthToken");
	}
	
	public RecordStruct getCredentials() {
		return this.context.getFieldAsRecord("Credentials");
	}

	/**
	 * Verified tasks have been authenticated.  
	 * 
	 * @return true if task has been elevated
	 */
	public boolean isVerified() {
		return this.context.getFieldAsBooleanOrFalse("Verified");
	}

	/**
	 * @return chronology to use with this task (for output)
	 */
	public String getChronology() {
		return this.context.getFieldAsString("Chronology");
	}

	/**
	 * @return locale to use with this task for output
	 */
	public String getLocale() {
		return this.context.getFieldAsString("Locale");
	}

	/**
	 * Domain indicates the domain targeted by a request.
	 * 
	 * @return domain string 
	 */
	public String getDomainId() {
		return this.context.getFieldAsString("DomainId");
	}

	public DomainInfo getDomain() {
		return Hub.instance.getDomainInfo(this.getDomainId());
	}
	
	/**
	 * @param m create a task context from a message (RPC calls to dcBus), keep in mind
	 * this is info gathering only, message must not be allowed to force an 
	 * authenticated/elevated state inappropriately - from RPC clear "Elevated"
	 * field before calling this
	 */
	protected UserContext(RecordStruct m) {
		this.context = m;
	}
	
	public RecordStruct freezeToRecord() {
		return (RecordStruct) this.context.deepCopy();
	}
	
	public OperationContextBuilder toBuilder() {
		return new OperationContextBuilder(this.freezeToRecord());
	}
	
	
	/**
	 * @param m store task context into a message - for context transfer over bus
	 */
	public void freeze(RecordStruct m) {
		m.copyFields(this.context);
	}
	
	/**
	 * @param m store task context into a message - for context transfer over bus
	 */
	public void freezeRpc(RecordStruct m) {
		// rpc doesn't get creds or token
		m.copyFields(this.context, "Credentials", "AuthToken");
	}
	
	public boolean looksLikeGuest() {
		if (!"00000_000000000000002".equals(this.getUserId()))
			return false;
		
		if (!this.context.isFieldEmpty("AuthToken") || !this.context.isFieldEmpty("Credentials"))
			return false;
		
		ListStruct creds = this.context.getFieldAsList("AuthTags");
		
		if ((creds == null) || (creds.getSize() != 1))
			return false;
		
		return ("Guest".equals(creds.getItemAsString(0)));
	}
	
	public boolean looksLikeRoot() {
		if (!"00000_000000000000001".equals(this.getUserId()))
			return false;
		
		if (!this.context.isFieldEmpty("AuthToken") || !this.context.isFieldEmpty("Credentials"))
			return false;
		
		ListStruct creds = this.context.getFieldAsList("AuthTags");
		
		if ((creds == null) || (creds.getSize() == 0))
			return false;
		
		for (int i = 0; i < creds.getSize(); i++) {
			if ("SysAdmin".equals(creds.getItemAsString(i)))
				return true;
		}
		
		return false;
	}
	
	/**
	 * @param tags to search for with this user
	 * @return true if this user has one of the requested authorization tags  (does not check authentication)
	 */
	public boolean isTagged(String... tags) {
		ListStruct creds = this.context.getFieldAsList("AuthTags");
		
		if (creds == null) 
			return false;
		
		for (int i = 0; i < creds.getSize(); i++) {
			String has = creds.getItemAsString(i);

			for (String wants : tags) {
				if (has.equals(wants))
					return true;
			}
		}
		
		return false;
	}
	
	public String tr(String token, Object... params) {
		return LocaleUtil.tr(this.getLocale(), token, params);		
	}
		
	public String trp(String pluraltoken, String singulartoken, Object... params) {
		return LocaleUtil.trp(this.getLocale(), pluraltoken, singulartoken, params);		
	}
	
	@Override
	public String toString() {
		RecordStruct usr = (RecordStruct) this.context.deepCopy();
		
		RecordStruct creds = usr.getFieldAsRecord("Credentials");
		
		if (creds != null) {
			if (creds.hasField("Password"))
				creds.setField("Password", "*****");
		}
		
		return usr.toPrettyString(); 
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof UserContext))
			return false;
		
		return this.context.equals(((UserContext)obj).context);
	}

	public void freezeSafe(RecordStruct m) {
		m.copyFields(this.context);
		
		RecordStruct creds = m.getFieldAsRecord("Credentials");
		
		if (creds != null) {
			if (creds.hasField("Password"))
				creds.setField("Password", "*****");
		}
	}
}
