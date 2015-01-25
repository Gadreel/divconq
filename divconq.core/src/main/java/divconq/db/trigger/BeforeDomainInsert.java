package divconq.db.trigger;

import divconq.db.TablesAdapter;
import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.hub.Hub;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationResult;
import divconq.struct.RecordStruct;
import divconq.xml.XElement;

public class BeforeDomainInsert implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		if (task.isReplicating())
			return;
		
		RecordStruct fields = task.getParamsAsRecord().getFieldAsRecord("Fields");
		
		if (!fields.hasField("dcObscureSeed")) {
			XElement obfconfig = new XElement("Clock");
			
			Hub.instance.getClock().getObfuscator().configure(obfconfig);
			
			// set the obscure seed before insert
			fields.withField("dcObscureSeed", new RecordStruct()
				.withField("Data", obfconfig.getAttribute("Feed"))
			);
		}
		
		TablesAdapter db = new TablesAdapter(conn, task); 
		
		FuncResult<String> ires = db.createRecord("dcDomain");
		
		if (ires.hasErrors()) 
			return;
		
		String id = ires.getResult();
		
		task.getParamsAsRecord().setField("Id", id);		// now the calling code thinks we are an update rather than insert
		
		// for the rest of this request we are running in another domain
		task.pushDomain(id);
	}
}
