package divconq.db.thru;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import divconq.db.ObjectResult;
import divconq.db.query.ISelectField;
import divconq.db.query.ListDirectRequest;
import divconq.db.query.LoadRecordRequest;
import divconq.db.query.SelectField;
import divconq.hub.Hub;
import divconq.lang.CountDownCallback;
import divconq.lang.op.OperationCallback;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.struct.builder.BuilderStateException;
import divconq.struct.builder.ICompositeBuilder;
import divconq.struct.builder.JsonStreamBuilder;
import divconq.util.IOUtil;

public class Export {
	// TODO stream results directly to files - one query for export per table (not lots of queries)
	static public void export(final ExportTable table, final File dest, final CountDownCallback dcallback) {
		try {
			final FileOutputStream fos3 = new FileOutputStream(dest);				
			final ICompositeBuilder json = new JsonStreamBuilder(new PrintStream(fos3), true);
			json.startList();
			
			final OperationCallback occallback = new OperationCallback() {
				@Override
				public void callback() {
					try {
						json.endList();
					} 
					catch (BuilderStateException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					IOUtil.closeQuietly(fos3);
					
					table.done();
					
					dcallback.countDown();
				}
			};
			
			ISelectField selt = new SelectField().withField("Id");
			ListDirectRequest req = new ListDirectRequest(table.table, selt);
			
			Hub.instance.getDatabase().submit(req, new ObjectResult() {
				@Override
				public void process(CompositeStruct result) {
					if (this.hasErrors()) {
						System.out.println(table + " bad load: " + result);
						occallback.complete();
					}
					else {
						ListStruct ids = (ListStruct) result;
						
						if (ids.getSize() == 0) {
							occallback.complete();
							return;
						}
						
						final CountDownCallback cdcallback = new CountDownCallback(ids.getSize(), occallback);
						
						for (Struct itm : ids.getItems()) {
							String id = itm.toString();
							
							LoadRecordRequest req = new LoadRecordRequest()
								.withTable(table.table)
								.withId(id)
								.withCompact(false);
							
							Hub.instance.getDatabase().submit(req, new ObjectResult() {
								@Override
								public void process(CompositeStruct result) {
									System.out.println("loaded: " + table.table + " - " + id);
									
									if (this.hasErrors())
										System.out.println(table + " bad load: " + result + " - " + id);
									else {
										synchronized (json) {
											try {
												RecordStruct rs = (RecordStruct) result;
												
												rs = table.transform(rs);
												
												if (rs != null)
													rs.toBuilder(json);
												//else
												//	System.out
												//			.println("could not find: " + id + " in domain: " + TaskContext.get().getUserContext().getDomainId());
											} 
											catch (BuilderStateException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
											}
										}										
									}
									
									cdcallback.countDown();
								}
							});
						}
					}
				}
			});
		}
		catch (Exception x) {
			System.out.println(table + " bad load: " + x);
			
			table.done();
			
			dcallback.countDown();
		}
	}
}
