package divconq.db.proc;

import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.struct.RecordStruct;
import divconq.struct.builder.ICompositeBuilder;

public class Hash implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		ICompositeBuilder resp = task.getBuilder();
		RecordStruct params = (RecordStruct) task.getParams();
		
		try {
			resp.startRecord();
			resp.field("Value", OperationContext.get().getUserContext().getDomain().getObfuscator().hashStringToHex(params.getFieldAsString("Value")));
			resp.endRecord();
		}
		catch (Exception x) {
			log.error("Hash: Unable to create response: " + x);
		}
		
		task.complete();
	}
}
