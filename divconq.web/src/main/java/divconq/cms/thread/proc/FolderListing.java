package divconq.cms.thread.proc;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.db.TablesAdapter;
import divconq.lang.BigDateTime;
import divconq.lang.op.OperationResult;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.struct.builder.ICompositeBuilder;

public class FolderListing implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		
		// TODO replicating
		// if (task.isReplicating()) 

		TablesAdapter db = new TablesAdapter(conn, task); 
		
		/* TODO use dcmThreadA or dcmThreadB 	 */
		
		BigDateTime when = BigDateTime.nowDateTime();
		boolean historical = false;
		ICompositeBuilder out = task.getBuilder();
		AtomicReference<String> currparty = new AtomicReference<>();
		String folder = params.getFieldAsString("Folder");
		
		try {
			Consumer<Object> partyConsumer = new Consumer<Object>() {				
				@Override
				public void accept(Object t) {
					try {
						String id = t.toString();						
						String party = currparty.get();

						// TODO filter labels too
						
						String foldr = (String) db.getStaticList("dcmThread", id, "dcmFolder", party);
						
						if (!folder.equals(foldr))
							return;
						
						out.startRecord();
						out.field("Id", id);
						out.field("Uuid", db.getStaticScalar("dcmThread", id, "dcmUuid"));
						out.field("Title", db.getStaticScalar("dcmThread", id, "dcmTitle"));
						out.field("TargetDate", db.getStaticScalar("dcmThread", id, "dcmTargetDate"));
						out.field("EndDate", db.getStaticScalar("dcmThread", id, "dcmEndDate"));
						out.field("Created", db.getStaticScalar("dcmThread", id, "dcmCreated"));
						out.field("Modified", db.getStaticScalar("dcmThread", id, "dcmModified"));
						out.field("Originator", db.getStaticScalar("dcmThread", id, "dcmOriginator"));
						out.field("Read", db.getStaticList("dcmThread", id, "dcmRead", party));
						
						// TODO split and output labels
						out.field("Labels");
						out.startList();
						out.endList();
						
						out.endRecord();
					}
					catch (Exception x) {
						log.error("Issue with folder listing: " + x);
					}
				}
			};				
			
			ListStruct values = params.getFieldAsList("FilterParties");
			
			out.startList();
			
			for (Struct s : values.getItems()) {
				currparty.set(s.toString());
				
				//output data for this party
				out.startRecord();
				out.field("Party", currparty.get());
				out.field("Folder");
				out.startList();
				
				// collect data for this party
				db.traverseIndex("dcmThread", "dcmParty", currparty.get(), when, historical, partyConsumer);

				out.endList();
				
				out.endRecord();						
			}
			
			out.endList();
		}
		catch (Exception x) {
			log.error("Issue with folder listing: " + x);
		}
		
		task.complete();
	}
}
