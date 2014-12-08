package divconq.db.proc;

import divconq.db.DatabaseAdapter;
import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.lang.op.OperationResult;
import divconq.struct.RecordStruct;

public class RetireRecord implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		
		String table = params.getFieldAsString("Table");
		String id = params.getFieldAsString("Id");
		
		// TODO add db filter option
		//d runFilter("Retire") quit:Errors  ; if any violations in filter then do not proceed
		
		DatabaseAdapter db = new DatabaseAdapter(conn, task); 

		db.setStaticScalar(table, id, "Retired", true);
		
		task.complete();
	}
}
