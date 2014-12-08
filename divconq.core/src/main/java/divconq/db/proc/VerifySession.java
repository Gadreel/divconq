package divconq.db.proc;

import divconq.db.DatabaseAdapter;
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
		DatabaseAdapter db = new DatabaseAdapter(conn, task); 
		String did = task.getDomain();
		BigDateTime when = BigDateTime.nowDateTime();
		
		RecordStruct params = task.getParamsAsRecord();
		String token = params.getFieldAsString("Token");
		
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
									new FieldStruct("Field", "dcAuthorizationTag"),		
									new FieldStruct("Name", "AuthorizationTags")
							)
					);		

					out.startRecord();
					out.field("UserInfo");
					
					this.writeRecord(conn, task, log, out, db, "dcUser",
							uid, when, select, true, false, false);
					
					out.field("AdditionalTags", null);		// TODO we actually need group tags too
					out.endRecord();
				}
			}
		}
		catch (Exception x) {
			log.error("SignOut: Unable to create resp: " + x);
		}
		
		task.complete();
	}
}
