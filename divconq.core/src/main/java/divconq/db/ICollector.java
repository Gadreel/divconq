package divconq.db;

import java.util.function.Consumer;

import divconq.lang.op.OperationResult;
import divconq.struct.RecordStruct;

public interface ICollector {
	void collect(DatabaseInterface conn, DatabaseTask task, OperationResult log, 
			RecordStruct collector, Consumer<Object> uniqueConsumer);
}
