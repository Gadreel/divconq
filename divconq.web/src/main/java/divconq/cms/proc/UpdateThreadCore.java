package divconq.cms.proc;

import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.db.TablesAdapter;
import divconq.db.update.DbRecordRequest;
import divconq.db.update.UpdateRecordRequest;
import divconq.lang.BigDateTime;
import divconq.lang.op.OperationResult;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;

public class UpdateThreadCore implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		
		// TODO replicating
		// if (task.isReplicating()) 
		
		TablesAdapter db = new TablesAdapter(conn, task); 
		
		String tid = UpdateThreadCore.getThreadId(db, params);
		
		DbRecordRequest req = new UpdateRecordRequest()
			.withId(tid)
			.withTable("dcmThread")
			.withConditionallyUpdateFields(params, "Title", "dcmTitle", "EndDate", "dcmEndDate", "TargetDate", "dcmTargetDate", "Originator", "dcmOriginator");
			
		ListStruct lbs = params.getFieldAsList("Labels");
		
		if (!lbs.isEmpty())
			req.withUpdateField("dcmLabels", "|" + StringUtil.join(lbs.toStringList(), "|") + "|");

		task.getDbm().submit(req, task.getResult());
	}
	
	static public String getThreadId(TablesAdapter db, RecordStruct params) {
		BigDateTime when = BigDateTime.nowDateTime();

		String tid = params.getFieldAsString("Id");

		if (StringUtil.isEmpty(tid) && !params.isFieldEmpty("Uuid")) {
			String uuid = params.getFieldAsString("Uuid");
			
			Object oid = db.firstInIndex("dcmThread", "dcmUuid", uuid, when, false);
			
			if (oid != null)
				tid = oid.toString();
		}

		if (StringUtil.isEmpty(tid) && !params.isFieldEmpty("Hash")) {
			String hash = params.getFieldAsString("Hash");
			
			Object oid = db.firstInIndex("dcmThread", "dcmHash", hash, when, false);
			
			if (oid != null)
				tid = oid.toString();
		}
		
		return tid;
	}
}
