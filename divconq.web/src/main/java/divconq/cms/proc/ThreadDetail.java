package divconq.cms.proc;

import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.db.TablesAdapter;
import divconq.lang.op.OperationResult;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.struct.builder.ICompositeBuilder;

public class ThreadDetail implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		ICompositeBuilder out = task.getBuilder();
		
		// TODO replicating
		// if (task.isReplicating()) 
		
		TablesAdapter db = new TablesAdapter(conn, task); 
		
		String id = UpdateThreadCore.getThreadId(db, params);
		
		String party = params.getFieldAsString("Party");
		
		try {
			out.startRecord();
			out.field("Id", id);
			out.field("Uuid", db.getStaticScalar("dcmThread", id, "dcmUuid"));
			out.field("Title", db.getStaticScalar("dcmThread", id, "dcmTitle"));
			out.field("TargetDate", db.getStaticScalar("dcmThread", id, "dcmTargetDate"));
			out.field("EndDate", db.getStaticScalar("dcmThread", id, "dcmEndDate"));
			out.field("Created", db.getStaticScalar("dcmThread", id, "dcmCreated"));
			out.field("Modified", db.getStaticScalar("dcmThread", id, "dcmModified"));
			
			String oid = Struct.objectToString(db.getStaticScalar("dcmThread", id, "dcmOriginator"));
			
			out.field("Originator", oid);
			
			if (params.isFieldEmpty("DisplayNameField"))
				out.field("OriginatorName", db.getStaticScalar("dcUser", oid, "dcFirstName") + " " + db.getStaticScalar("dcUser", oid, "dcLastName"));
			else
				out.field("OriginatorName", db.getStaticScalar("dcUser", oid, params.getFieldAsString("DisplayNameField")));
			
			out.field("Read", db.getStaticList("dcmThread", id, "dcmRead", party));
			out.field("Folder", db.getStaticList("dcmThread", id, "dcmFolder", party));
			
			out.field("Parties");
			out.startList();
			
			for (String pvalue : db.getStaticListKeys("dcmThread", id, "dcmParty"))
				out.value(ThreadDetail.partyValueToPartyName(pvalue));
			
			out.endList();
			
			// TODO split and output labels
			out.field("Labels");
			out.startList();
			out.endList();
			
			out.field("Content");
			out.startList();
			
			for (String stamp : db.getStaticListKeys("dcmThread", id, "dcmContent")) {
				out.startRecord();
				out.field("Content", db.getStaticList("dcmThread", id, "dcmContent", stamp));
				out.field("ContentType", db.getStaticList("dcmThread", id, "dcmContentType", stamp));
				out.field("Attributes", db.getStaticList("dcmThread", id, "dcmAttributes", stamp));
				out.endRecord();
			}
			
			out.endList();
			
			out.endRecord();
		}
		catch (Exception x) {
			log.error("Issue with thread detail: " + x);
		}
		
		task.complete();
	}
	
	static public String partyValueToPartyName(String name) {
		// TODO 
		
		return name;
	}
}
