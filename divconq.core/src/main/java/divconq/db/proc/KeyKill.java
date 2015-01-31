package divconq.db.proc;

import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.db.DatabaseInterface;
import divconq.db.util.ByteUtil;
import divconq.lang.op.OperationResult;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.HexUtil;

public class KeyKill implements IStoredProc {
	@Override
	public void execute(DatabaseInterface adapter, DatabaseTask task, OperationResult or) {
		RecordStruct params = task.getParamsAsRecord();

		ListStruct keys = params.getFieldAsList("Keys");
		
		byte[] basekey = null;
		
		for (Struct ss : keys.getItems()) 
			basekey =  ByteUtil.combineKeys(basekey, HexUtil.decodeHex(ss.toString())); 

		adapter.kill(basekey);
		
		task.complete();
	}
}
