package divconq.db.rocks.proc;

import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.db.DatabaseInterface;
import divconq.lang.op.OperationResult;
import divconq.struct.builder.ICompositeBuilder;

public class KeyQueryProc implements IStoredProc {
	@Override
	public void execute(DatabaseInterface adapter, DatabaseTask task, OperationResult or) {
		ICompositeBuilder resp = task.getBuilder();
		//CompositeStruct params = task.getParams();
		
		try {
			resp.startList();
			
			resp.startList();
			resp.value("Record");
			resp.value("Person");
			resp.value(2075);
			resp.value("Name");
			resp.value("Fred Smith");
			resp.endList();
			
			resp.endList();
		}
		catch (Exception x) {
			or.error("KeyQueryProc: Unable to create list: " + x);
		}
		
		task.complete();
	}
}
