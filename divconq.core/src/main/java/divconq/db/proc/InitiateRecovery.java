package divconq.db.proc;

import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.lang.op.OperationResult;

public class InitiateRecovery implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		/*
		ICompositeBuilder out = task.getBuilder();
		DatabaseAdapter db = new DatabaseAdapter(conn, task); 
		String did = task.getDomain();
		BigDateTime when = BigDateTime.nowDateTime();
		
		RecordStruct params = task.getParamsAsRecord();
		String user = params.getFieldAsString("User");

		find user by uname, email or backup email - generate and return Code, email and backup email
				
		store dcRecoverAt and dcConfirmCode
		
		leave dcConfirmed alone, that is only for registration purposes
		
					// db will set this too
					.withField("At", new DateTime())
					// TODO actually db will generate and return the code
					.withField("Code", StringUtil.buildSecurityCode())
				
				
				<Field Name="dcConfirmed" Type="Boolean" />
				<Field Name="dcConfirmCode" Type="dcTinyString" />
				<Field Name="dcRecoverAt" Type="DateTime" />
				
		*/
		
		task.complete();
	}
}
