package divconq.db.proc;

import divconq.db.TablesAdapter;
import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.lang.BigDateTime;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.session.Session;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.builder.ICompositeBuilder;
import divconq.util.StringUtil;

public class SignIn extends LoadRecord {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		ICompositeBuilder out = task.getBuilder();
		TablesAdapter db = new TablesAdapter(conn, task); 
		String did = task.getDomain();
		BigDateTime when = BigDateTime.nowDateTime();
				
		String token = null;
		String uid = null;
		//boolean confirmed = false;
		String password = params.getFieldAsString("Password");
		String uname = params.getFieldAsString("Username");
		//String code = params.getFieldAsString("Code");
		// TODO part of Trust monitoring -- boolean suspect = params.getFieldAsBooleanOrFalse("Suspect");	
		
		try {			
			if (task.isReplicating()) {
				token = params.getFieldAsString("Token");
				uid = params.getFieldAsString("Uid");
				//confirmed = params.getFieldAsBooleanOrFalse("Confirmed");
			}
			else {
				Object userid = db.firstInIndex("dcUser", "dcUsername", uname, when, false);
				
				if (userid != null) 
					uid = userid.toString();
				
				if (StringUtil.isNotEmpty(uid)) {
					if (StringUtil.isNotEmpty(password)) {
						Object fndpass = db.getDynamicScalar("dcUser", uid, "dcPassword", when);
						
						if (fndpass != null) {
							// TODO check if it is hex, if so it is probably hashed so then run incoming password through
							// hash to do compare
							//params.setField("Password", OperationContext.get().getUserContext().getDomain().getObfuscator().hashStringToHex(this.password.trim()));
							
							String password2 = OperationContext.get().getUserContext().getDomain().getObfuscator().hashStringToHex(password.trim());
							
							// otherwise do plain text compare
							if (!password2.equals(fndpass)) {
								// TODO if recover is not expired
								//. i recoverExpire]]$$get1^dcDb("dcUser",uid,"dcRecoverAt") q
								
								fndpass = db.getStaticScalar("dcUser", uid, "dcConfirmCode");
								
								if (!password.equals(fndpass)) 
									uid = null;
							}
						}
						else {
							// TODO if recover is not expired
							//. i recoverExpire]]$$get1^dcDb("dcUser",uid,"dcRecoverAt") q
							
							fndpass = db.getStaticScalar("dcUser", uid, "dcConfirmCode");
							
							if (!password.equals(fndpass)) 
								uid = null;
						}
					}
				}
			}
			
			if (StringUtil.isEmpty(uid)) {
				log.errorTr(123);
				task.complete();
				return;
			}
			
			if (!db.isCurrent("dcUser", uid, when, false)) {
				log.errorTr(123);
				task.complete();
				return;
			}
			
			if (!task.isReplicating()) {
				// TODO a confirmed login requires at least user name and a confirmation code, it might also take a password
				// but the code must be present to become a confirmed user
				// i '$$get1^dcDb("dcUser",uid,"dcConfirmed") d
				// . i (code'="")&($$get1^dcDb("dcUser",uid,"dcConfirmCode")=code) s Params("Confirmed")=1,confirmed=1 q
				// . d err^dcConn(124) q							
				
				token = Session.nextSessionId();
			}
			
			if (log.hasErrors()) {
				task.complete();
				return;
			}

			// replication will need these later
			if (!task.isReplicating()) {
				params.setField("Token", token);
				params.setField("Uid", uid);
			}

			// both isReplicating and normal store the token
			
			conn.set("dcSession", token, "LastAccess", task.getStamp());
			conn.set("dcSession", token, "User", uid);
			conn.set("dcSession", token, "Domain", did);
			
			//if (confirmed) 
			//	db.setStaticScalar("dcUser", uid, "dcConfirmed", confirmed);
			
			// TODO create some way to track last login that doesn't take up db space
			// or make last login an audit thing...track all logins in StaticList?
			
			// done with replication stuff
			if (task.isReplicating()) {
				task.complete();
				return;
			}			
			
			// load info about the user
			ListStruct select = new ListStruct(
					new RecordStruct(
							new FieldStruct("Field", "Id"),
							new FieldStruct("Name", "UserId")
					),
					new RecordStruct(
							new FieldStruct("Field", "dcUsername"),
							new FieldStruct("Name", "Username")
					),
					new RecordStruct(
							new FieldStruct("Field", "dcFirstName"),
							new FieldStruct("Name", "FirstName")
					),
					new RecordStruct(
							new FieldStruct("Field", "dcLastName"),
							new FieldStruct("Name", "LastName")
					),
					new RecordStruct(
							new FieldStruct("Field", "dcEmail"),
							new FieldStruct("Name", "Email")
					),
					new RecordStruct(
							new FieldStruct("Field", "dcLocale"),
							new FieldStruct("Name", "Locale")
					),
					new RecordStruct(
							new FieldStruct("Field", "dcChronology"),
							new FieldStruct("Name", "Chronology")
					),
					// TODO we actually need group tags too - extend how this works
					new RecordStruct(
							new FieldStruct("Field", "dcAuthorizationTag"),		
							new FieldStruct("Name", "AuthorizationTags")
					),
					new RecordStruct(
							new FieldStruct("Value", token),
							new FieldStruct("Name", "AuthToken")
					)
			);		

			//out.startRecord();
			//out.field("UserInfo");
			
			this.writeRecord(conn, task, log, out, db, "dcUser",
					uid, when, select, true, false, false);
			
			//out.field("AdditionalTags", null);		
			//out.endRecord();
		}
		catch (Exception x) {
			log.error("SignIn: Unable to create resp: " + x);
		}
		
		task.complete();
	}
}
