package divconq.db.proc;

import java.util.function.Consumer;

import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.TablesAdapter;
import divconq.lang.BigDateTime;
import divconq.lang.op.OperationResult;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.builder.ICompositeBuilder;

public class SelectDirect extends LoadRecord {
	/*
	 ;  Table			name
	 ;	When			"" = now
	 ;					[stamp] = on or before this time
	 ;
	 ;  Where
	 ;			"Name"				expression name: "And", "Or", "Equal", etc.  -or-  field name  -or-  "Filter"
	 ;			"A"					value to compare against, or param to Filter
	 ;				"Field"			if from database
	 ;				"Format"
	 ;				"Value"			if literal
	 ;				"Composer"		composer script to generate content
	 ;			"B"					value to compare against, or param to Filter
	 ;			"C"					value to compare against, or param to Filter
	 ;			"Children"
	 ;					0,"Name"
	 ;					1...
	 ;
	 ;  Select	list of fields to query
	 ;			0,
	 ;				"Field"			name
	 ;				"Format"		format of field if scalar
	 ;				"Name"			display name of field
	 ;				"ForeignField"	value field in fk relationship
	 ;				"Table"			for reverse foreign
	 ;				"Composer"		composer script to generate content
	 ;				"Select"		list of fields to query, see above
	 ;						0,
	 ;						1...
	 ;			1...
	 ;
	 ;  Collector
	 ;			"Name"				code to execute to get collection
	 ;			"Values"				
	 ;					0 			value to match
	 ;					1...
	 ;			"From"				value to start at, inclusive
	 ;			"To"				value to end at, exclusive
	 ;			"Field"				if not using Code to get collection, use a Field instead
	 ;
	 ;	Historical	true / false
	 ;
	 ; Result
	 ;		List of records, content based on Select
	 */
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		
		String table = params.getFieldAsString("Table");
		BigDateTime when = params.getFieldAsBigDateTime("When");
		boolean compact = params.hasField("Compact") ? params.getFieldAsBooleanOrFalse("Compact") : true;
		boolean historical = params.getFieldAsBooleanOrFalse("Historical");	
		ListStruct select = params.getFieldAsList("Select");
		RecordStruct where = params.getFieldAsRecord("Where");
		
		// TODO add db filter option
		//d runFilter("Query") quit:Errors  ; if any violations in filter then do not proceed
		
		TablesAdapter db = new TablesAdapter(conn, task); 
		ICompositeBuilder out = task.getBuilder();
		
		/*
		// TODO support collector
		// m collector=Params("Collector")
		 * 
		 i collector("Name")'="" d  quit
		 . s cname=collector("Name")		; "cstate" is a variable available to the collector for state across calls
		 . i ^dcProg("collector",cname)="" q
		 . w StartList
		 . f  x "s id=$$"_^dcProg("collector",cname)_"()" q:id=""  d  q:Errors
		 . . d:$$check^dcDbSelect(table,id,when,.where,historical) writeRec(table,id,when,.select,5,1,0,historical)
		 . w EndList
		 ;
		 i collector("Field")'="" d  quit
		 . i $d(collector("Values")) d  q
		 . . n values m values=collector("Values")
		 . . w StartList
		 . . f  s id=$$loopValues^dcDb(table,collector("Field"),.values,.cstate,when,historical) q:id=""  d
		 . . . d:$$check^dcDbSelect(table,id,when,.where,historical) writeRec(table,id,when,.select,5,1,0,historical)
		 . . w EndList
		 . ;
		 . w StartList
		 . f  s id=$$loopRange^dcDb(table,collector("Field"),collector("From"),collector("To"),.cstate,when,historical) q:id=""  d 
		 . . d:$$check^dcDbSelect(table,id,when,.where,historical) writeRec(table,id,when,.select,5,1,0,historical)
		 . w EndList
		 ;
		 w StartList		
		*/
		
		try {
			out.startList();
	
			db.traverseRecords(table, when, historical, new Consumer<Object>() {				
				@Override
				public void accept(Object t) {
					try {
						String id = t.toString();
						
						if (db.checkSelect(table, id, when, where, historical))
							SelectDirect.this.writeRecord(conn, task, log, out, db, table, id, when, select, compact, false, historical);
					}
					catch (Exception x) {
						log.error("Issue with select direct: " + x);
					}
				}
			});
			
			out.endList();
		}
		catch (Exception x) {
			log.error("Issue with select direct: " + x);
		}
		
		task.complete();
	}
}
