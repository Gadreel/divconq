package divconq.db.proc;

import static divconq.db.Constants.*;
import divconq.db.TablesAdapter;
import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.util.ByteUtil;
import divconq.lang.BigDateTime;
import divconq.lang.op.OperationResult;
import divconq.session.Session;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.builder.ICompositeBuilder;
import divconq.util.StringUtil;

public class StartSession extends LoadRecord {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		ICompositeBuilder out = task.getBuilder();
		TablesAdapter db = new TablesAdapter(conn, task); 
		String did = task.getDomain();
		BigDateTime when = BigDateTime.nowDateTime();
				
		String token = null;
		String uid = params.getFieldAsString("UserId");
		String uname = params.getFieldAsString("Username");
		
		try {			
			if (task.isReplicating()) {
				token = params.getFieldAsString("Token");
				uid = params.getFieldAsString("Uid");
			}
			else {
				if (StringUtil.isEmpty(uid)) {
					byte[] userid = conn.nextPeerKey(DB_GLOBAL_INDEX_2, did, "dcUser", "dcUsername", uname, null);
	
					if (userid != null) 
						uid = ByteUtil.extractValue(userid).toString();
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
			
			this.writeRecord(conn, task, log, out, db, "dcUser",
					uid, when, select, true, false, false);
		}
		catch (Exception x) {
			log.error("StartSession: Unable to create resp: " + x);
		}
		
		task.complete();
	}
}
