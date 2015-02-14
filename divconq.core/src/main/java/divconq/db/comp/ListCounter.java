package divconq.db.comp;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IComposer;
import divconq.db.TablesAdapter;
import divconq.lang.BigDateTime;
import divconq.lang.op.OperationResult;
import divconq.schema.DbField;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.builder.BuilderStateException;
import divconq.struct.builder.ICompositeBuilder;

public class ListCounter implements IComposer {
	@Override
	public void writeField(DatabaseInterface conn, DatabaseTask task, OperationResult log, ICompositeBuilder out, TablesAdapter db,
			String table, String id, BigDateTime when, ListStruct select, RecordStruct field, boolean historical, boolean compact)
	{	
		try {
			String fname = field.getFieldAsString("Field");
			
			DbField fdef = task.getSchema().getDbField(table, fname);

			if (fdef == null) {
				out.value(new Long(0));
				return;
			}
			
			AtomicLong cnt = new AtomicLong();
			
			if ("Id".equals(fname)) {
				cnt.set(1);
			}
			// DynamicList, StaticList (or DynamicScalar is when == null)
			else if (fdef.list || (fdef.dynamic && when == null)) {
				// keep in mind that `id` is the "value" in the index
				db.traverseSubIds(table, id, fname, when, historical, new Consumer<Object>() {				
					@Override
					public void accept(Object subid) {
						cnt.incrementAndGet();
					}
				});
			}		
			// DynamicScalar
			else if (fdef.dynamic) {
				if (db.getDynamicScalarRaw(table, id, fname, when, historical) != null)
					cnt.set(1);
			}
			// StaticScalar
			else {
				if (db.getStaticScalarRaw(table, id, fname) != null)
					cnt.set(1);
			}
			
			task.getBuilder().value(new Long(cnt.get()));
		} 
		catch (BuilderStateException x) {
			// TODO Auto-generated catch block
			x.printStackTrace();
		}
	}
}
