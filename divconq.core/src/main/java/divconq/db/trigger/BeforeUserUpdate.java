package divconq.db.trigger;

import static divconq.db.Constants.DB_GLOBAL_INDEX_2;
import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.db.util.ByteUtil;
import divconq.lang.op.OperationResult;
import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;

public class BeforeUserUpdate implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		if (task.isReplicating())
			return;
		
		RecordStruct params = task.getParamsAsRecord();
		String id = params.getFieldAsString("Id");
		
		if (StringUtil.isEmpty(id)) {
			log.error("User id required to update user.");
			return;
		}		
		
		RecordStruct fields = params.getFieldAsRecord("Fields");
		RecordStruct uname = fields.getFieldAsRecord("dcUsername");
		
		// nothing to check
		if (uname == null)
			return;
		
		String did = task.getDomain();

		try {
			for (FieldStruct fs : uname.getFields()) {
				RecordStruct rec = (RecordStruct) fs.getValue();
				
				if (rec.isFieldEmpty("Data")) {
					log.error("Username may not be empty.");
					return;
				}
				
				byte[] userid = conn.nextPeerKey(DB_GLOBAL_INDEX_2, did, "dcUser", "dcUsername", rec.getFieldAsString("Data"), null);
		
				if (userid != null) {
					String uid = ByteUtil.extractValue(userid).toString();

					if (!id.equals(uid)) {
						log.error("Username must be unique, this username (email) already in use.");
						return;
					}
				}
			}
		}
		catch (Exception x) {
			log.error("Update User: Failed to read Index: " + x);
		}
	}
}
