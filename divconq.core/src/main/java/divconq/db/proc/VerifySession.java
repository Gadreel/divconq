package divconq.db.proc;

import divconq.db.TablesAdapter;
import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.lang.BigDateTime;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.builder.ICompositeBuilder;
import divconq.util.StringUtil;

public class VerifySession extends LoadRecord {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		ICompositeBuilder out = task.getBuilder();
		TablesAdapter db = new TablesAdapter(conn, task); 
		String did = task.getDomain();
		BigDateTime when = BigDateTime.nowDateTime();
		
		RecordStruct params = task.getParamsAsRecord();
		String token = params.getFieldAsString("AuthToken");
		
		String uid = OperationContext.get().getUserContext().getUserId();

		try {
			if (StringUtil.isEmpty(token)) 
				log.errorTr(117);
			else {
				String dd = (String) conn.get("dcSession", token, "Domain");
				String uu = (String) conn.get("dcSession", token, "User");
				
				if (!uid.equals(uu) || !did.equals(dd)) {
					log.errorTr(121);
				}
				else {					
					conn.set("dcSession", token, "LastAccess", task.getStamp());
					

					// load info about the user
					ListStruct select = new ListStruct(
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
							)
					);		

					//out.startRecord();
					//out.field("UserInfo");
					
					this.writeRecord(conn, task, log, out, db, "dcUser",
							uid, when, select, true, false, false);
					
					/*
					// load info about the user
					out.startRecord();
					<Field Name="Username" Type="dcUser:dcUsername" />
					
					out.field("AuthorizationTags");
					
					this.writeField(conn, task, log, out, db, "dcUser",
							uid, when, "dcAuthorizationTag", false, false);
					
					// TODO someday get group tags too
					
					out.endRecord();
					*/
				}
			}
		}
		catch (Exception x) {
			log.error("SignOut: Unable to create resp: " + x);
		}
		
		task.complete();
	}
}
