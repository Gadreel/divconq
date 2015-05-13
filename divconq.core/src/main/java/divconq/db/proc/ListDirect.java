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

public class ListDirect extends LoadRecord {
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
	 ;  Select	field to query
	 ;			"Field"			name
	 ;			"Format"		format of field if scalar
	 ;			"ForeignField"	value field in fk relationship
	 ;			"Composer"		composer script to generate content
	 ;			"Table"			for reverse foreign
	 ;			"Select"		list of fields to query, see above
	 ;					0,
	 ;					1...
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
	 ;		List of values, content based on Select
	 ;
	 */
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		
		String table = params.getFieldAsString("Table");
		BigDateTime when = params.getFieldAsBigDateTime("When");
		boolean compact = params.hasField("Compact") ? params.getFieldAsBooleanOrFalse("Compact") : true;
		boolean historical = params.getFieldAsBooleanOrFalse("Historical");	
		ListStruct select = new ListStruct(params.getFieldAsRecord("Select"));
		RecordStruct where = params.getFieldAsRecord("Where");
		
		if (when == null)
			when = BigDateTime.nowDateTime();
		
		BigDateTime fwhen = when;
		
		// TODO add db filter option
		//d runFilter("Query") quit:Errors  ; if any violations in filter then do not proceed
		
		TablesAdapter db = new TablesAdapter(conn, task); 
		ICompositeBuilder out = task.getBuilder();
		
		/*
		// TODO support collector
		*/
	
		try {
			out.startList();
			
			db.traverseRecords(table, when, historical, new Consumer<Object>() {				
				@Override
				public void accept(Object t) {
					String id = t.toString();
					
					db.checkSelect(table, id, fwhen, where, historical);
	
					try {
						ListDirect.this.writeField(conn, task, log, out, db, table, id, fwhen, select.getItemAsRecord(0),
								historical, compact);
					}
					catch (Exception x) {
						log.error("Issue with select list: " + x);
					}
				}
			});
	
			out.endList();
		}
		catch (Exception x) {
			log.error("Issue with select list: " + x);
		}
		
		task.complete();
	}
}
