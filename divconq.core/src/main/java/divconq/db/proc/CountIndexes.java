package divconq.db.proc;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import divconq.db.TablesAdapter;
import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.lang.BigDateTime;
import divconq.lang.op.OperationResult;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.struct.builder.ICompositeBuilder;

public class CountIndexes implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		
		String table = params.getFieldAsString("Table");
		String fname = params.getFieldAsString("Field");
		BigDateTime when = params.getFieldAsBigDateTime("When");
		boolean historical = params.getFieldAsBooleanOrFalse("Historical");	
		ListStruct values = params.getFieldAsList("Values");
		
		if (when == null)
			when = BigDateTime.nowDateTime();
		
		TablesAdapter db = new TablesAdapter(conn, task); 
		ICompositeBuilder out = task.getBuilder();
		
		try {
			out.startList();
			
			for (Struct vs : values.getItems()) {
				Object val = Struct.objectToCore(vs);
				
				AtomicLong cnt = new AtomicLong();
		
				db.traverseIndex(table, fname, val, when, historical, new Consumer<Object>() {				
					@Override
					public void accept(Object subid) {
						cnt.incrementAndGet();
					}
				});
				
				out.startRecord();
				out.field("Name", val);
				out.field("Count", new Long(cnt.get()));
				out.endRecord();
			}
			
			out.endList();
		}
		catch (Exception x) {
			log.error("Issue with counting index record: " + x);
		}
		
		task.complete();
	}
}
