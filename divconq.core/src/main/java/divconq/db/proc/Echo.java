package divconq.db.proc;

import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.lang.op.OperationResult;
import divconq.struct.RecordStruct;
import divconq.struct.builder.ICompositeBuilder;

public class Echo implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		ICompositeBuilder resp = task.getBuilder();
		RecordStruct params = (RecordStruct) task.getParams();
		
		try {
			resp.startRecord();
			resp.field("Text", params.getFieldAsString("Text"));
			resp.endRecord();
		}
		catch (Exception x) {
			log.error("Exho: Unable to create response: " + x);
		}
		
		task.complete();
	}
}
