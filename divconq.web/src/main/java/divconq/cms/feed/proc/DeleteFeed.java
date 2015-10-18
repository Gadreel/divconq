package divconq.cms.feed.proc;

import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.db.TablesAdapter;
import divconq.db.update.RetireRecordRequest;
import divconq.lang.BigDateTime;
import divconq.lang.op.OperationResult;
import divconq.struct.RecordStruct;

public class DeleteFeed implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		
		//String chann = params.getFieldAsString("Channel");
		String path = params.getFieldAsString("Path");
		
		// TODO replicating
		// if (task.isReplicating())
		
		TablesAdapter db = new TablesAdapter(conn, task); 
		
		BigDateTime when = BigDateTime.nowDateTime();
		Object oid = db.firstInIndex("dcmFeed", "dcmPath", path, when, false);
		
		if (oid != null) {
			RetireRecordRequest lr1 = new RetireRecordRequest("dcmFeed", oid.toString());
			
			task.getDbm().submit(lr1, task.getResult());
		}
		else {
			task.complete();
		}
	}
}
