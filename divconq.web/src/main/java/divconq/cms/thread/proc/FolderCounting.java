package divconq.cms.thread.proc;

import java.util.HashMap;
import java.util.Map;
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

public class FolderCounting implements IStoredProc {
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
		Map<String, FolderCount> currdata = new HashMap<>(); 
		
		try {
			Consumer<Object> partyConsumer = new Consumer<Object>() {				
				@Override
				public void accept(Object t) {
					try {
						String id = t.toString();						
						String party = currparty.get();

						// TODO filter labels 
						
						String foldr = (String) db.getStaticList("dcmThread", id, "dcmFolder", party);
						
						FolderCount fd = currdata.get(foldr);
						
						if (fd == null) {
							fd = new FolderCount();
							fd.name = foldr;
							currdata.put(foldr, fd);
						}
						
						Boolean read = (Boolean) db.getStaticList("dcmThread", id, "dcmRead", party);
						
						if ((read == null) || !read)
							fd.newcnt++;
						
						fd.totalcnt++;
						
					}
					catch (Exception x) {
						log.error("Issue with folder counting: " + x);
					}
				}
			};				
			
			ListStruct values = params.getFieldAsList("FilterParties");
			
			out.startList();
			
			for (Struct s : values.getItems()) {
				currparty.set(s.toString());
				currdata.clear();
				
				// collect data for this party
				db.traverseIndex("dcmThread", "dcmParty", currparty.get(), when, historical, partyConsumer);
				
				//output data for this party
				out.startRecord();
				out.field("Party", currparty.get());
				out.field("Folders");
				out.startList();
				
				for (FolderCount cnt : currdata.values()) {
					out.startRecord();
					out.field("Name", cnt.name);
					out.field("New", cnt.newcnt);
					out.field("Total", cnt.totalcnt);
					out.field("Labels");
					
					// TODO split and output labels
					out.startList();
					out.endList();
					
					out.endRecord();
				}
				
				out.endList();
				
				out.endRecord();						
			}
			
			out.endList();
		}
		catch (Exception x) {
			log.error("Issue with folder counting: " + x);
		}
		
		task.complete();
	}
	
	public class FolderCount {
		public String name = null;
		public int newcnt = 0;
		public int totalcnt = 0;
		public String Labels = null;
	}
}
