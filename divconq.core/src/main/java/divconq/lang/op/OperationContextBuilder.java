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
package divconq.lang.op;

import org.joda.time.DateTimeZone;

import divconq.locale.LocaleUtil;
import divconq.log.DebugLevel;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;

/*
		<Record Id="UserCredentials">
			<Field Name="Username" Type="dcUser:dcUsername" Required="True" />
			<Field Name="Password" Type="dcUser:dcPassword" />
			<Field Name="ConfirmationCode" Type="dcUser:dcConfirmCode" />
			<Field Name="ThumbPrint" Type="dcTinyString" />
			<Field Name="PublicKey" Type="String" />
		</Record>
		
		<Record Id="UserContext">
			<Field Name="UserId" Type="Id" />
			<Field Name="Username" Type="dcUser:dcUsername" />
			<Field Name="FullName" Type="dcSmallString" />
			<Field Name="Email" Type="dcUser:dcEmail" />
			<Field Name="Credentials" Type="UserCredentials" />
			<Field Name="AuthToken" Type="dcAuthToken" />
			<Field Name="Verified" Type="Boolean" />
			<Field Name="Locale" Type="dcSmallString" />
			<Field Name="Chronology" Type="dcSmallString" />
			<Field Name="DomainId" Type="Id" />
			<Field Name="AuthTags">
				<List Type="dcTinyString" />
			</Field>
		</Record>
		
		<Record Id="OpContext" Inherits="UserContext">
			<Field Name="OpId" Type="dcTinyString" />
			<Field Name="SessionId" Type="dcTinyString" />
			<Field Name="Origin" Type="dcSmallString" />
			<Field Name="DebugLevel" Type="dcTinyString" />
			<Field Name="Elevated" Type="Boolean" />
			<Field Name="Gateway" Type="Boolean" />
		</Record>

		
		
		
		<Table Id="dcDomain">
			<Field Name="dcTitle" Type="dcSmallString" Required="True" />
			<Field Name="dcName" Type="dcSmallString" Required="True" List="True" />
			<Field Name="dcDescription" Type="String" />
			
			<!-- crypto support -->
			<Field Name="dcObscureClass" Type="dcSmallString" />
			<Field Name="dcObscureSeed" Type="dcSmallString" />
			
			<!-- used by root domain only, index of all domains -->
			<Field Name="dcDomainIndex" Type="Id" List="True" />
		</Table>
		
		<!--
			guest user = 00000_000000000000000
			root user  = 00000_000000000000001
			
			root user is in ^dcRecord("dcUser","00000_000000000000001" 
			
			but users may be in more than on domain - a user id only appears once in entire database.  the same user id in more than
			one domain denotes that the SAME user belongs to more than one domain.
			
			so root user in domain 00975_000000123000001 is ^dcRecord("dcUser#00975_000000123000001","00000_000000000000001"
			and user 00975_000000123000999 is ^dcRecord("dcUser#00975_000000123000001","00975_000000123000999"
			but this user may also appear in another domain, such as ^dcRecord("dcUser#00100_000000000000001","00975_000000123000999"
		-->
		<Table Id="dcUser">
			<Field Name="dcUsername" Type="dcUsername" Required="True" Indexed="True" Dynamic="True" />
			<Field Name="dcFirstName" Type="dcTinyString" Indexed="True" Dynamic="True" />
			<Field Name="dcLastName" Type="dcTinyString" Indexed="True" Dynamic="True" />
			
			<Field Name="dcEmail" Type="dcSmallString" Indexed="True" Dynamic="True" />
			<!-- dcEmail should be email within the domain, backup applies if within domain is bad, missing or user account is disabled TODO -->
			<Field Name="dcBackupEmail" Type="dcSmallString" Indexed="True" Dynamic="True" />
			<Field Name="dcPassword" Type="dcSmallString" Dynamic="True" />
			<Field Name="dcLocale" Type="dcSmallString" />
			<Field Name="dcChronology" Type="dcSmallString" />
			<Field Name="dcDescription" Type="String" />
			
			<Field Name="dcLastLogin" Type="DateTime" />
			<Field Name="dcConfirmed" Type="Boolean" />
			<Field Name="dcConfirmCode" Type="dcTinyString" />
			<Field Name="dcRecoverAt" Type="DateTime" />
			
			<Field Name="dcAuthorizationTag" Type="dcTinyString" List="True" />
			
			<Field Name="dcGroup" ForeignKey="dcGroup" List="True" /> 
		</Table>
		
		<Table Id="dcGroup">
			<Field Name="dcName" Type="dcSmallString" Required="True" Indexed="True" Dynamic="True" />
			<Field Name="dcAuthorizationTag" Type="dcTinyString" List="True" />
			<Field Name="dcDescription" Type="String" />
		</Table>
		
 * 
 */

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
			.withUsername("guest")
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
			.withUsername("root")
			.withFullName("Root User")
			.withVerified(true)
			.withAuthTags("User", "PowerUser", "Admin", "SysAdmin")
			.withLocale(LocaleUtil.getDefaultLocale())
			.withChronology("/" + DateTimeZone.getDefault().getID());		// ISOChronology w/ default zone
	}
	
	// take existing context and elevate, but do not change locale or chrono
	public OperationContextBuilder elevateToRootTask() {
		return this
			.withElevated(true)
			.withDomainId("00000_000000000000001")		// root is part of root domain
			.withUserId("00000_000000000000001")
			.withUsername("root")
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

	public OperationContextBuilder withGateway(boolean v) {
		this.values.setField("Gateway", v);
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

	public OperationContextBuilder withUsername(String v) {
		this.values.setField("Username", v);
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
