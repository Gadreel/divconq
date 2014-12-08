package divconq.db.trigger;

import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.lang.op.OperationResult;

public class BeforeUserInsert implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		if (task.isReplicating())
			return;
		
		/*
		
		<RecordFilter Name="dcIsAccountTaken" Execute="acctTaken^dcUser">
			<Description>
				Checks to see if the account name is already in use by another
				user.
			</Description>
		</RecordFilter>
		 * 
		
		RecordStruct fields = task.getParamsAsRecord().getFieldAsRecord("Fields");
		
		// set the obscure seed before insert
		fields.withField("dcObscureSeed", new RecordStruct()
			.withField("Data", StringUtil.buildSecurityCode(64))
		);
		
		DatabaseAdapter db = new DatabaseAdapter(conn, task); 
		
		FuncResult<String> ires = db.createRecord("dcDomain");
		
		if (ires.hasErrors()) 
			return;
		
		String id = ires.getResult();
		
		task.getParamsAsRecord().setField("Id", id);
		
		// for the rest of this request we are running in another domain
		task.pushDomain(id);
		 */
	}
}
