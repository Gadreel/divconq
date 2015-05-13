package divconq.cms.proc;

import org.joda.time.DateTime;

import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.db.TablesAdapter;
import divconq.db.update.DbRecordRequest;
import divconq.db.update.UpdateRecordRequest;
import divconq.lang.op.OperationResult;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;

public class ChangeStatusAction implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		
		// TODO replicating
		// if (task.isReplicating()) 

		TablesAdapter db = new TablesAdapter(conn, task); 
		
		String tid = UpdateThreadCore.getThreadId(db, params);
		boolean read = params.getFieldAsBoolean("Read");
		
		DbRecordRequest req = new UpdateRecordRequest()
			.withId(tid)
			.withTable("dcmThread");
			
		ListStruct plist = params.getFieldAsList("Parties");
		
		for (int i = 0; i < plist.getSize(); i++) { 
			req.withUpdateField("dcmRead", plist.getItemAsString(i), read);
			req.withUpdateField("dcmLastRead", plist.getItemAsString(i), read ? new DateTime() : null);
		}
		
		/* TODO  5) read status changed for party
			- set in dcRecord for that party, do not change Modified
			- update dcmThreadA for that thread id
			- set dcmThreadB party+new folder = null - do so for all labels, party labels and "star"         (means force recalc)
		 */
		
		task.getDbm().submit(req, task.getResult());
	}
}
