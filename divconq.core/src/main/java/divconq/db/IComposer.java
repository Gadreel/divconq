package divconq.db;

import divconq.lang.BigDateTime;
import divconq.lang.op.OperationResult;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.builder.ICompositeBuilder;

public interface IComposer {
	void writeField(DatabaseInterface conn, DatabaseTask task, OperationResult log, ICompositeBuilder out,
			TablesAdapter db, String table, String id, BigDateTime when, ListStruct select, RecordStruct field, 
			boolean historical, boolean compact);
}
