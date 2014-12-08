package divconq.db.proc;

import java.math.BigDecimal;

import org.joda.time.DateTime;

import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.db.util.ByteUtil;
import divconq.lang.op.OperationResult;
import divconq.struct.RecordStruct;

public class Cleanup implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		//DateTime expire = params.getFieldAsDateTime("ExpireThreshold");
		DateTime lexpire = params.getFieldAsDateTime("LongExpireThreshold");

		try {
			byte[] sessonid = conn.nextPeerKey("dcSession", null);

			while (sessonid != null) { 
				String token = ByteUtil.extractValue(sessonid).toString();
				
				BigDecimal la = conn.getAsDecimal("dcSession", token, "LastAccess");
				
				if ((la == null) || (lexpire.getMillis() > la.abs().longValue())) 
					conn.kill("dcSession", token);
				
				sessonid = conn.nextPeerKey("dcSession", token);
			}
		}
		catch (Exception x) {
			log.error("SignOut: Unable to create resp: " + x);
		}
		
		task.complete();
	}
}
