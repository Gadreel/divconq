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

import org.joda.time.DateTimeZone;

import divconq.locale.LocaleUtil;
import divconq.log.DebugLevel;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;

// can be used to build task context or user context
public class OperationContextBuilder {
	protected RecordStruct values = null;

	public OperationContextBuilder() {
		this.values = new RecordStruct();
	}
	
	protected OperationContextBuilder(RecordStruct ctx) {
		this.values = ctx;
	}
	
	// templates
	
	public OperationContextBuilder withGuestTaskTemplate() {
		return this
			.withOrigin("hub:")
			.withGuestUserTemplate();
	}
	
	public OperationContextBuilder withRootTaskTemplate() {
		return this
			.withElevated(true)
			.withOrigin("hub:")
			.withRootUserTemplate();
	}

	public OperationContextBuilder withGuestUserTemplate() {
		return this
			.withDomainId("00000_000000000000001")		// guest is part of root domain
			.withUserId("00000_000000000000002")
			.withUserName("guest")
			.withFullName("Guest User")
			.withVerified(true)
			.withAuthTags("Guest")
			.withLocale(LocaleUtil.getDefaultLocale())
			.withChronology("/" + DateTimeZone.getDefault().getID());		// ISOChronology w/ default zone
	}

	public OperationContextBuilder withRootUserTemplate() {
		return this		
			.withDomainId("00000_000000000000001")		// root is part of root domain
			.withUserId("00000_000000000000001")
			.withUserName("root")
			.withFullName("Root User")
			.withVerified(true)
			.withAuthTags("User", "PowerUser", "Admin", "SysAdmin")
			.withLocale(LocaleUtil.getDefaultLocale())
			.withChronology("/" + DateTimeZone.getDefault().getID());		// ISOChronology w/ default zone
	}
	
	public OperationContextBuilder elevateToRootTask() {
		return this
			.withElevated(true)
			.withDomainId("00000_000000000000001")		// root is part of root domain
			.withUserId("00000_000000000000001")
			.withUserName("root")
			.withFullName("Root User")
			.withVerified(true)
			.withAuthTags("User", "PowerUser", "Admin", "SysAdmin");
	}
	
	// for task
	public OperationContextBuilder withOpId(String v) {
		this.values.setField("OpId", v);
		return this;
	}

	public OperationContextBuilder withNewOpId() {
		this.values.setField("OpId", OperationContext.allocateOpId());
		return this;
	}
	
	public OperationContextBuilder withOrigin(String v) {
		this.values.setField("Origin", v);
		return this;
	}
	
	public OperationContextBuilder withSessionId(String v) {
		this.values.setField("SessionId", v);
		return this;
	}
	
	public OperationContextBuilder withDebugLevel(DebugLevel v) {
		this.values.setField("DebugLevel", v.toString());
		return this;
	}

	public OperationContextBuilder withElevated(boolean v) {
		this.values.setField("Elevated", v);
		return this;
	}
	
	public OperationContext toOperationContext() {
		return OperationContext.allocate(this);
	}
	
	public OperationContext toOperationContext(UserContext utx) {
		return OperationContext.allocate(utx, this);
	}
	
	// for user
	public OperationContextBuilder withUserId(String v) {
		this.values.setField("UserId", v);
		return this;
	}

	public OperationContextBuilder withUserName(String v) {
		this.values.setField("UserName", v);
		return this;
	}

	public OperationContextBuilder withFullName(String v) {
		this.values.setField("FullName", v);
		return this;
	}

	public OperationContextBuilder withEmail(String v) {
		this.values.setField("Email", v);
		return this;
	}

	public OperationContextBuilder withAuthToken(String v) {
		this.values.setField("AuthToken", v);
		return this;
	}

	public OperationContextBuilder withLocale(String v) {
		this.values.setField("Locale", v);
		return this;
	}

	public OperationContextBuilder withChronology(String v) {
		this.values.setField("Chronology", v);
		return this;
	}

	public OperationContextBuilder withDomainId(String v) {
		this.values.setField("DomainId", v);
		return this;
	}

	public OperationContextBuilder withVerified(boolean v) {
		this.values.setField("Verified", v);
		return this;
	}

	public OperationContextBuilder withCredentials(RecordStruct v) {
		this.values.setField("Credentials", v);
		
		// changing credentials clears any verification
		this.withVerified(false)
			.withAuthToken(null)
			.withAuthTags("Guest");
		
		return this;
	}

	public OperationContextBuilder withAuthTags(String... v) {
		this.values.setField("AuthTags", new ListStruct((Object[])v));
		return this;
	}

	public OperationContextBuilder withAuthTags(ListStruct v) {
		this.values.setField("AuthTags", v.deepCopy());
		return this;
	}

	public OperationContextBuilder addAuthTags(String... v) {
		ListStruct tlist = this.values.getFieldAsList("AuthTags");
		
		if (tlist == null) 
			this.values.setField("AuthTags", new ListStruct((Object[])v));
		else
			tlist.addItem((Object[])v);
		
		return this;
	}
	
	public UserContext toUserContext() {
		return UserContext.allocate(this);
	}
	
	@Override
	public String toString() {
		return this.values.toPrettyString();
	}
}
