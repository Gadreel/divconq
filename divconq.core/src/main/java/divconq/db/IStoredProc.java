package divconq.db;

import divconq.lang.op.OperationResult;

public interface IStoredProc {
	void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log);
}
