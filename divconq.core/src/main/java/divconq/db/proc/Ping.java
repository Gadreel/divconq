package divconq.db.proc;

import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.lang.op.OperationResult;
import divconq.struct.builder.ICompositeBuilder;

public class Ping implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		ICompositeBuilder resp = task.getBuilder();
		//CompositeStruct params = task.getParams();
		
		try {
			resp.startRecord();
			resp.field("Text", "Pong");
			resp.endRecord();

			/* alternative solution
			rec.toBuilder(new RecordStruct(new FieldStruct("Text", "Pong")));
			*/
		}
		catch (Exception x) {
			log.error("Ping: Unable to create response: " + x);
		}
		
		task.complete();
	}
}
