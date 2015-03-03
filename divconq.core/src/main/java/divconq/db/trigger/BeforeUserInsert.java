package divconq.db.trigger;

import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.db.TablesAdapter;
import divconq.lang.BigDateTime;
import divconq.lang.op.OperationResult;
import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;

public class BeforeUserInsert implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		if (task.isReplicating())
			return;
		
		RecordStruct params = task.getParamsAsRecord();
		RecordStruct fields = params.getFieldAsRecord("Fields");
		
		RecordStruct uname = fields.getFieldAsRecord("dcUsername");
		
		if (uname == null) {
			log.error("Username required to insert a user.");
			return;
		}
		
		TablesAdapter db = new TablesAdapter(conn, task); 

		try {
			for (FieldStruct fs : uname.getFields()) {
				RecordStruct rec = (RecordStruct) fs.getValue();
				
				if (rec.isFieldEmpty("Data")) {
					log.error("Username required to insert a user.");
					return;
				}
				
				Object userid = db.firstInIndex("dcUser", "dcUsername", rec.getFieldAsString("Data"), BigDateTime.nowDateTime(), false);
				
				if (userid != null) {
					log.error("Username must be unique, this username (email) already in use.");
					return;
				}
			}
		}
		catch (Exception x) {
			log.error("Insert User: Failed to read Index: " + x);
		}
	}
}
