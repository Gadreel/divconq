package divconq.db.proc;

import divconq.db.DatabaseAdapter;
import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationResult;
import divconq.struct.RecordStruct;
import divconq.struct.builder.ICompositeBuilder;
import divconq.util.StringUtil;

public class UpdateRecord implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		boolean isUpdate = task.getName().equals("dcUpdateRecord");

		RecordStruct params = task.getParamsAsRecord();
		String table = params.getFieldAsString("Table");
		
		DatabaseAdapter db = new DatabaseAdapter(conn, task); 
		
		// ===========================================
		//  run before trigger
		// ===========================================
		OperationResult cor = db.executeTrigger(table, isUpdate ? "BeforeUpdate" : "BeforeInsert", conn, task, log);
		
		if (cor.hasErrors()) {
			task.complete();
			return;
		}
		
		// ===========================================
		//  verify the fields
		// ===========================================
		
		RecordStruct fields = params.getFieldAsRecord("Fields");
		
		// it is possible for Id to be set by trigger (e.g. with domains)
		String id = params.getFieldAsString("Id");
		
		cor = db.checkFields(table, fields);
		
		if (cor.hasErrors()) {
			task.complete();
			return;
		}
		
		// TODO add db filter option
		//d runFilter("Insert" or "Update") quit:Errors  ; if any violations in filter then do not proceed
		
		// ===========================================
		//  create new id
		// ===========================================
		
		// don't create a new id during replication - not even for dcInsertRecord
		if (StringUtil.isEmpty(id)) {
			FuncResult<String> ires = db.createRecord(table);
			
			if (ires.hasErrors()) {
				task.complete();
				return;
			}
			
			id = ires.getResult();
			
			params.setField("Id", id);
		}

		// ===========================================
		//  do the data update
		// ===========================================
		db.setFields(table, id, fields);
		
		// ===========================================
		//  run after trigger
		// ===========================================
		cor = db.executeTrigger(table, isUpdate ? "AfterUpdate" : "AfterInsert", conn, task, log);
		
		if (cor.hasErrors()) {
			task.complete();
			return;
		}
		
		// ===========================================
		//  return results
		// ===========================================
		
		// don't bother returning data during replication 
		if (!isUpdate && !task.isReplicating()) {
			ICompositeBuilder resp = task.getBuilder();
			
			try {
				resp.startRecord();
				resp.field("Id", id);
				resp.endRecord();

				/* alternative solution
				RecordStruct rec = new RecordStruct(new FieldStruct("Id", id));
				rec.toBuilder(resp);
				*/
			}
			catch (Exception x) {
				log.error("UpdateRecord: Unable to create response: " + x);
			}
		}
		
		task.complete();
	}
}
