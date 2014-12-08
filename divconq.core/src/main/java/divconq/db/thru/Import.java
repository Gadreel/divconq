package divconq.db.thru;

import java.io.File;
import java.io.FileInputStream;

import divconq.db.ObjectResult;
import divconq.hub.Hub;
import divconq.lang.CountDownCallback;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationCallback;
import divconq.struct.CompositeParser;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.IOUtil;

public class Import {

	public static void importData(final String table, File source, final CountDownCallback mcallback) {
		try {
			final OperationCallback occallback = new OperationCallback() {
				@Override
				public void callback() {
					mcallback.countDown();
				}
			};
			
			FileInputStream fos3 = new FileInputStream(source);
			FuncResult<CompositeStruct> res = CompositeParser.parseJson(fos3);
			IOUtil.closeQuietly(fos3);
			
			if (!res.hasErrors()) {
				ListStruct records = (ListStruct) res.getResult();
				
				if ((records == null) || (records.getSize() == 0)) {
					occallback.complete();
					return;
				}
				
				final CountDownCallback cdcallback = new CountDownCallback(records.getSize(), occallback);
				
				for (Struct itm : records.getItems()) {
					RecordStruct rec = (RecordStruct)itm;
					
					final ImportRecordRequest req = new ImportRecordRequest(table, rec);
					
					Hub.instance.getDatabase().submit(req, new ObjectResult() {
						@Override
						public void process(CompositeStruct result) {
							/* TODO review and restore
							System.out.println("imported: " + table + " - " + req.getId());
							
							if (res.hasErrors())
								System.out.println(table + " bad import: " + res + " - " + req.getId());
							*/
							
							cdcallback.countDown();
						}
					});
				}
			}
			else {
				occallback.complete();
				System.out.println(table + " bad file: " + source.getName() + " - " + res.getMessage());
			}
		}
		catch (Exception x) {
			System.out.println(table + " bad file: " + source.getName() + " - " + x);
			mcallback.countDown();
		}
	}

}
