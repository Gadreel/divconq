package divconq.db.proc;

import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.lang.op.OperationResult;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;

public class SignOut implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
				
		String token = params.getFieldAsString("AuthToken");

		try {
			if (StringUtil.isEmpty(token)) 
				log.errorTr(117);
			else 
				conn.kill("dcSession", token);
		}
		catch (Exception x) {
			log.error("SignOut: Unable to create resp: " + x);
		}
		
		task.complete();
	}
}
