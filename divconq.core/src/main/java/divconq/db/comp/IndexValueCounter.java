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
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.struct.builder.BuilderStateException;
import divconq.struct.builder.ICompositeBuilder;
import divconq.util.StringUtil;

// TODO re-think, this is not yet used
public class IndexValueCounter implements IComposer {
	@Override
	public void writeField(DatabaseInterface conn, DatabaseTask task, OperationResult log, ICompositeBuilder out, TablesAdapter db,
			String table, String id, BigDateTime when, RecordStruct field, boolean historical, boolean compact)
	{	
		try {
			String fname = field.getFieldAsString("Field");

			if (StringUtil.isEmpty(fname)) {
				out.value(new Long(0));
				return;
			}
			
			DbField fdef = task.getSchema().getDbField(table, fname);

			if (fdef == null) {
				out.value(new Long(0));
				return;
			}
			
			RecordStruct params = field.getFieldAsRecord("Params");

			if ((params == null) || params.isFieldEmpty("Value")) {
				out.value(new Long(0));
				return;
			}
			
			// get as a type we understand
			Object val = Struct.objectToCore(field.getField("Value"));
			
			AtomicLong cnt = new AtomicLong();

			db.traverseIndex(table, fname, val, when, historical, new Consumer<Object>() {				
				@Override
				public void accept(Object subid) {
					cnt.incrementAndGet();
				}
			});
			
			task.getBuilder().value(new Long(cnt.get()));
		} 
		catch (BuilderStateException x) {
			// TODO Auto-generated catch block
			x.printStackTrace();
		}
	}
}
