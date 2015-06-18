package divconq.db.proc;

import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.db.TablesAdapter;
import divconq.hub.Hub;
import divconq.lang.op.OperationResult;

public class ReindexDomain implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		TablesAdapter db = new TablesAdapter(conn, task);
		
		db.rebuildIndexes();
		
		Hub.instance.getDomainInfo(task.getDomain()).fireAfterReindex();
		
		task.complete();
	}
}
