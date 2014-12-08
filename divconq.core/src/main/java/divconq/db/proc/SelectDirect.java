package divconq.db.proc;

import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.lang.op.OperationResult;

public class SelectDirect implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		// TODO
		
		task.complete();
	}
	
	/*
 ; Request
 ;
 ; d local^dcConn("QUERY dcSelectDirect")
 ;
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
 ;
select n id,table,when,select,where,collector,cstate,cname,historical
 s table=Params("Table"),when=Params("When"),historical=Params("Historical")
 i (table="") d err^dcConn(50013) quit
 s:when="" when=$$whenNow^dcTimeUtil()
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 s:$d(Params("Select"))=0 Params("Select")="*"
 ;
 m select=Params("Select")
 m where=Params("Where")
 m collector=Params("Collector")
 ;
 i select="*" d  
 . s select(0,"Field")="Id",select(0,"Name")="Id"
 . f i=1:1  s fname=$o(^dcSchema(table,"Fields",fname)) q:fname=""  d
 . . s select(i,"Field")=fname,select(i,"Name")=fname
 ;
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
 ;
 f  s id=$o(^dcRecord(table,id)) q:id=""  d  q:Errors
 . d:$$check^dcDbSelect(table,id,when,.where,historical) writeRec(table,id,when,.select,5,1,0,historical)
 ;
 w EndList
 ;
 quit
 
 ;
 ;				"Field"			name
 ;				"Format"		format of field if scalar
 ;				"Name"			display name of field
 ;				"ForeignField"	value field in fk relationship
 ;				"Composer"		composer script to generate content
 ;
buildSelect(select,field,name,format,foreign,composer,full) n pos s pos=$o(select(pos),-1) 
 i pos="" s pos=0
 e  s pos=pos+1
 s select(pos,"Field")=field
 s:name'="" select(pos,"Name")=name
 s:format'="" select(pos,"Format")=format
 s:foreign'="" select(pos,"ForeignField")=foreign
 s:composer'="" select(pos,"Composer")=composer
 s:full'="" select(pos,"Full")=full
 quit
 ;
 ;
 ;
select2(table,select,when,where,collector,historical) n Params
 ;
 s Params("Table")=table
 s:when'="" Params("When")=when
 s:historical'="" Params("Historical")=historical
 ;
 m:$d(select)>0 Params("Select")=select 
 m:$d(where)>0 Params("Where")=where 
 m:$d(collector)>0 Params("Collector")=collector
 ;
 d select^dcDbQuery
 ;
 quit
 ;
 ;  
 
 
 	 * 
	 */
}
