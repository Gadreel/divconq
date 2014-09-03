dcDbQuery ;
 ;
 ; ----------------------------------------------------------------------------
 ; #
 ; #  DivConq
 ; #
 ; #  http://divconq.com/
 ; #
 ; #  Copyright:
 ; #    Copyright 2012 eTimeline, LLC. All rights reserved.
 ; #
 ; #  License:
 ; #    See the dcLicense.txt file in the project's top-level directory for details.
 ; #
 ; #  Authors:
 ; #    * Andy White
 ; #
 ; ----------------------------------------------------------------------------
 ;
 ; Request
 ;
 ; d local^dcConn("QUERY dcLoadRecord")
 ;
 ;  Table	name
 ;  Id		record id
 ;  Filter	filter name
 ;  Extra	parameters for filter
 ;
 ;  Select	list of fields to query
 ;			0,
 ;				"Field"			name
 ;				"Format"		format of field if scalar
 ;				"Name"			display name of field
 ;				"ForeignField"	value field in fk relationship
 ;				"Composer"		composer script to generate content
 ;				"Select"		list of fields to query, see above
 ;				"Full"			load full data (sid) even though most fields are compact
 ;			1...
 ;
 ;	When	"" = all
 ;			[stamp] = on or before this time
 ;
 ;	Compact	true / false
 ;
 ;	Historical	true / false
 ;
 ; Result
 ;
 ;		========== return compact============
 ;		{
 ;			{ssfield}: {value},
 ;			{slfield}: [
 ;				{value},
 ;				{value}
 ;			],
 ;			{dsfield}: {value},				if not When = ""
 ;			{dsfield}: [					if When = ""
 ;				{value},
 ;				{value}
 ;			],
 ;			{dlfield}: [
 ;				{value},
 ;				{value}
 ;			]
 ;		}
 ;
 ;
 ;		========== return full ============
 ;		{
 ;			{ssfield}: {
 ;				Data: {value},
 ;				Tags: {tags}
 ;			},
 ;			{slfield}: [
 ;				{
 ;					Sid: {sid},
 ;					Data: {value},
 ;					Tags: {tags}
 ;				},
 ;				{
 ;					Sid: {sid},
 ;					Data: {value},
 ;					Tags: {tags}
 ;				}
 ;			],
 ;			{dsfield}:  {					if not When = ""
 ;					Sid: {sid},
 ;					From: {from},
 ;					Data: {value},
 ;					Tags: {tags}
 ;			},
 ;			{dsfield}: [					if When = ""
 ;				{
 ;					Sid: {sid},
 ;					From: {from},
 ;					Data: {value},
 ;					Tags: {tags}
 ;				},
 ;				{
 ;					Sid: {sid},
 ;					From: {from},
 ;					Data: {value},
 ;					Tags: {tags}
 ;				}
 ;			],
 ;			{dlfield}: [
 ;				{
 ;					Sid: {sid},
 ;					From: {from},
 ;					To: {to},
 ;					Data: {value},
 ;					Tags: {tags}
 ;				},
 ;				{
 ;					Sid: {sid},
 ;					From: {from},
 ;					To: {to},
 ;					Data: {value},
 ;					Tags: {tags}
 ;				}
 ;			]
 ;		}
 ;
 ;
 ;	========== FK =============
 ;  normally a {value} is a scalar, unless an FK is invloved - if so then a subquery result may be present:
 ;
 ; scalar w/o FK
 ; 
 ;		{ssfield}: {value},
 ;		{slfield}: [
 ;			{value},
 ;			{value}
 ;		],
 ;
 ; scalar w/ FK to scalar
 ; 
 ;		{ssfield}: {
 ;			{field}: {value},
 ;			etc
 ;		},
 ;		{slfield}: [
 ;			{
 ;				{field}: {value},
 ;				etc
 ;			},
 ;			{
 ;				{field}: {value},
 ;				etc
 ;			}
 ;		],
 ;
 ;
loadRec n id,table,when,compact,select,historical
 s id=Params("Id"),table=Params("Table"),when=Params("When")
 s compact=Params("Compact"),historical=Params("Historical")
 i (id="")!(table="") d err^dcConn(50012) quit
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 d runFilter^dcDbUpdate("Query") quit:Errors  ; if any violations in filter then do not proceed
 ;
 s:$d(Params("Select"))=0 Params("Select")="*"
 m select=Params("Select")
 ;
 d writeRec(table,id,when,.select,20,compact,0,historical) 
 ;
 quit 
 ;
 ;
 ; skrec = skip record output, for when write Rec is already handled
writeRec(table,id,when,select,lvl,compact,skrec,historical) n fname,i,spos,field,stamp,composer,retired
 i id="" quit
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 i '$$isCurrent^dcDb(table,id,when,historical) quit
 ;
 ; create the select array ("Id" is a special field, even though not in fields list):
 ;   select([position],"Field")=[field name(s), colon separated]
 ;   select([position],"Format")=[formatting]
 ;   select([position],"Name")=[return name]
 ;
 i select="*" d  
 . s select(0,"Field")="Id",select(0,"Name")="Id"
 . f i=1:1  s fname=$o(^dcSchema($p(table,"#"),"Fields",fname)) q:fname=""  d
 . . s select(i,"Field")=fname,select(i,"Name")=fname
 ;
 i 'skrec w StartRec
 ;
 f  s spos=$o(select(spos))  q:spos=""  d
 . w Field_$s(select(spos,"Name")'="":select(spos,"Name"),1:select(spos,"Field"))
 . i compact&(select(spos,"Full")'=1) d writeCompactFld(table,id,when,.select,spos,lvl,historical) q
 . d writeFullFld(table,id,when,.select,spos,lvl,historical)
 ;
 i 'skrec w EndRec	
 ;
 quit
 ;
 ;
writeCompactFld(table,id,when,select,spos,lvl,historical) n field,stamp,fschema,islist,pos,subquery,sid,to,from,tidx,spid,tbl,kfld
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 i $d(select(spos,"Composer")) d  quit
 . s composer=select(spos,"Composer")
 . i (composer="")!(^dcProg("reccomposer",composer)="") w ScalarNull
 . x "d "_^dcProg("reccomposer",composer)
 ;
 s field=select(spos,"Field")
 i field="" w ScalarNull quit
 ;
 i field="Id" d  quit
 . i $d(select(spos,"Table")) d  q
 . . s tbl=select(spos,"Table"),kfld=select(spos,"KeyField")
 . . i (tbl'["#")&(Domain'="") s tbl=tbl_"#"_Domain     ; support table instances
 . . ;
 . . m fschema=^dcSchema($p(tbl,"#"),"Fields",kfld)
 . . ;
 . . s subquery(0,"Field")=select(spos,"ForeignField")
 . . s subquery(0,"Format")=select(spos,"Format")
 . . ;
 . . w StartList
 . . ;
 . . i 'fschema("List")&'fschema("Dynamic") d  i 1
 . . . f  s spid=$o(^dcIndex1(tbl,kfld,id,spid)) q:spid=""  d
 . . . . q:'$$isCurrent^dcDb(tbl,spid,when,historical)
 . . . . d writeCompactFld(tbl,spid,when,.subquery,0,1,historical) 
 . . e  f  s spid=$o(^dcIndex2(tbl,kfld,id,spid)) q:spid=""  d
 . . . q:'$$isCurrent^dcDb(tbl,spid,when,historical)
 . . . d writeCompactFld(tbl,spid,when,.subquery,0,1,historical) 
 . . ;
 . . w EndList
 . ;
 . w $$format(table,field,id,select(spos,"Format")) 
 ;
 m fschema=^dcSchema($p(table,"#"),"Fields",field)
 i $d(fschema)=0 w ScalarNull quit
 ;
 ; DynamicList, StaticList - also DynamicStatic when All (when="")
 ;
 i ((when="")&fschema("Dynamic"))!fschema("List") d  quit
 . w StartList
 . ;
 . f  s sid=$o(^dcRecord(table,id,field,sid)) q:sid=""  d
 . . s stamp=$$getStamp4^dcDb(table,id,field,sid,when) q:stamp="" 
 . . ;
 . . s spid=$s($d(fschema("ForeignKey")):^dcRecord(table,id,field,sid,stamp,"Data",0),1:"")
 . . i (spid'="")&$$writeCompactForeign(fschema("ForeignKey"),spid,when,.select,spos,lvl,historical) q
 . . ;
 . . f  s pos=$o(^dcRecord(table,id,field,sid,stamp,"Data",pos)) q:pos=""  d
 . . . i pos=0 w $$format(table,field,^dcRecord(table,id,field,sid,stamp,"Data",pos),select(spos,"Format")) q
 . . . w ^dcRecord(table,id,field,sid,stamp,"Data",pos)
 . ;
 . w EndList
 ;
 ; DynamicStatic
 ;
 i fschema("Dynamic") d  quit
 . s sid=$$get3sid^dcDb(table,id,field,when,"",historical) 
 . s stamp=$$getStamp3^dcDb(table,id,field,sid,when,historical)
 . i stamp="" w ScalarNull q
 . ;
 . s spid=$s($d(fschema("ForeignKey")):^dcRecord(table,id,field,sid,stamp,"Data",0),1:"")
 . i (spid'="")&$$writeCompactForeign(fschema("ForeignKey"),spid,when,.select,spos,lvl,historical) q
 . ;
 . f  s pos=$o(^dcRecord(table,id,field,sid,stamp,"Data",pos)) q:pos=""  d
 . . i pos=0 w $$format(table,field,^dcRecord(table,id,field,sid,stamp,"Data",pos),select(spos,"Format")) q
 . . w ^dcRecord(table,id,field,sid,stamp,"Data",pos)
 ;
 ; StaticScalar
 ;
 s stamp=$$getStamp1^dcDb(table,id,field) i stamp="" w ScalarNull quit
 ;
 s spid=$s($d(fschema("ForeignKey")):^dcRecord(table,id,field,stamp,"Data",0),1:"")
 i (spid'="")&$$writeCompactForeign(fschema("ForeignKey"),spid,when,.select,spos,lvl,historical) quit
 ;
 f  s pos=$o(^dcRecord(table,id,field,stamp,"Data",pos)) q:pos=""  d
 . i pos=0 w $$format(table,field,^dcRecord(table,id,field,stamp,"Data",pos),select(spos,"Format")) q
 . w ^dcRecord(table,id,field,stamp,"Data",pos)
 ;
 quit
 ;
 ;
 ; return true if the value is consumed (written or ignored)
writeCompactForeign(table,id,when,select,spos,lvl,historical) n subquery
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 q:'$$isCurrent^dcDb(table,id,when,historical) 1
 ;
 i (lvl>1)&$d(select(spos,"Select")) d  quit 1
 . m subquery=select(spos,"Select")
 . d writeRec(table,id,when,.subquery,lvl-1,1,0,historical) 
 ;
 i $d(select(spos,"ForeignField")) d  quit 1
 . s subquery(0,"Field")=select(spos,"ForeignField")
 . s subquery(0,"Format")=select(spos,"Format")
 . d writeCompactFld(table,id,when,.subquery,0,1,historical) 
 ;
 quit 0
 ;
 ;
 ;
writeFullFld(table,id,when,select,spos,lvl,historical) n field,stamp,fschema,islist,pos,subquery,sid,to,from,tidx,tbl,spid,kfld
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 i $d(select(spos,"Composer")) d  quit
 . s composer=select(spos,"Composer")
 . i (composer="")!(^dcProg("reccomposer",composer)="") w ScalarNull
 . x "d "_^dcProg("reccomposer",composer)
 ;
 s field=select(spos,"Field")
 i field="" w ScalarNull quit
 ;
 i field="Id" d  quit
 . i $d(select(spos,"Table")) d  q
 . . s tbl=select(spos,"Table"),kfld=select(spos,"KeyField")
 . . i (tbl'["#")&(Domain'="") s tbl=tbl_"#"_Domain     ; support table instances
 . . ;
 . . m fschema=^dcSchema($p(tbl,"#"),"Fields",kfld)
 . . ;
 . . s subquery(0,"Field")=select(spos,"ForeignField")
 . . s subquery(0,"Format")=select(spos,"Format")
 . . ;
 . . w StartList
 . . ;
 . . i 'fschema("List")&'fschema("Dynamic") d  i 1
 . . . f  s spid=$o(^dcIndex1(tbl,kfld,id,spid)) q:spid=""  d
 . . . . q:'$$isCurrent^dcDb(tbl,spid,when,historical)
 . . . . d writeFullFld(tbl,spid,when,.subquery,0,1,historical) 
 . . e  f  s spid=$o(^dcIndex2(tbl,kfld,id,spid)) q:spid=""  d
 . . . q:'$$isCurrent^dcDb(tbl,spid,when,historical)
 . . . d writeFullFld(tbl,spid,when,.subquery,0,1,historical) 
 . . ;
 . . w EndList
 . ;
 . w StartRec_Field_"Data"_$$format(table,field,id,select(spos,"Format"))_EndRec
 ;
 m fschema=^dcSchema($p(table,"#"),"Fields",field)
 i $d(fschema)=0 quit
 ;
 ; DynamicList, StaticList - also DynamicStatic when All (when="")
 ;
 i ((when="")&fschema("Dynamic"))!fschema("List") d  quit
 . w StartList
 . ;
 . f  s sid=$o(^dcRecord(table,id,field,sid)) q:sid=""  d
 . . s stamp=$$getStamp4^dcDb(table,id,field,sid,when) q:stamp="" 
 . . ;
 . . s spid=$s($d(fschema("ForeignKey")):^dcRecord(table,id,field,sid,stamp,"Data",0),1:"")
 . . i (spid'="")&$$writeFullForeign(fschema("ForeignKey"),spid,when,.select,spos,lvl,historical) i 1
 . . e  d writeFull2(table,id,field,sid,stamp,fschema("Type"),select(spos,"Format"))
 . ;
 . w EndList
 ;
 ; DynamicStatic
 ;
 i fschema("Dynamic") d  quit
 . s sid=$$get3sid^dcDb(table,id,field,when,"",historical) 
 . s stamp=$$getStamp3^dcDb(table,id,field,sid,when,historical)
 . i stamp="" w ScalarNull q
 . ;
 . s spid=$s($d(fschema("ForeignKey")):^dcRecord(table,id,field,sid,stamp,"Data",0),1:"")
 . i (spid'="")&$$writeFullForeign(fschema("ForeignKey"),spid,when,.select,spos,lvl,historical) i 1
 . e  d writeFull2(table,id,field,sid,stamp,fschema("Type"),select(spos,"Format"))
 ;
 ; StaticScalar
 ;
 s stamp=$$getStamp1^dcDb(table,id,field) i stamp="" w ScalarNull quit
 ;
 s spid=$s($d(fschema("ForeignKey")):^dcRecord(table,id,field,stamp,"Data",0),1:"")
 i (spid'="")&$$writeFullForeign(fschema("ForeignKey"),spid,when,.select,spos,lvl,historical) quit
 ;
 w StartRec
 w:$d(^dcRecord(table,id,field,stamp,"Tags")) Field_"Tags"_ScalarStr_^dcRecord(table,id,field,stamp,"Tags")
 w Field_"Data"
 ;
 f  s pos=$o(^dcRecord(table,id,field,stamp,"Data",pos)) q:pos=""  d
 . i pos=0 w $$format(table,field,^dcRecord(table,id,field,stamp,"Data",pos),select(spos,"Format")) q
 . w ^dcRecord(table,id,field,stamp,"Data",pos)
 ;
 w EndRec
 ;
 quit
 ;
 ; return true if the value is consumed (written or ignored)
writeFullForeign(table,id,when,select,spos,lvl,historical) n subquery
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 q:'$$isCurrent^dcDb(table,id,when,historical) 1
 ;
 i (lvl>1)&$d(select(spos,"Select")) d  quit 1
 . m subquery=select(spos,"Select")
 . d writeRec(table,id,when,.subquery,lvl-1,0,0,historical) 
 ;
 i $d(select(spos,"ForeignField")) d  quit 1
 . s subquery(0,"Field")=select(spos,"ForeignField")
 . s subquery(0,"Format")=select(spos,"Format")
 . d writeFullFld(table,id,when,.subquery,0,1,historical) 
 ;
 quit 0
 ;
 ;
writeFull2(table,id,field,sid,stamp,type,format) n pos
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 w StartRec
 w Field_"Sid"_ScalarStr_sid
 w:$d(^dcRecord(table,id,field,sid,stamp,"Tags")) Field_"Tags"_ScalarStr_^dcRecord(table,id,field,sid,stamp,"Tags")
 w:$d(^dcRecord(table,id,field,sid,stamp,"To")) Field_"To"_ScalarBigDateTime_^dcRecord(table,id,field,sid,stamp,"To")
 w:$d(^dcRecord(table,id,field,sid,stamp,"From")) Field_"From"_ScalarBigDateTime_^dcRecord(table,id,field,sid,stamp,"From")
 w Field_"Data"
 ;
 f  s pos=$o(^dcRecord(table,id,field,sid,stamp,"Data",pos)) q:pos=""  d
 . i pos=0 w $$format(table,field,^dcRecord(table,id,field,sid,stamp,"Data",pos),format) q
 . w ^dcRecord(table,id,field,sid,stamp,"Data",pos)
 ;
 w EndRec
 quit
 ;
 ;
 ;
format(table,field,val,format) i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 quit:format="" $$getTypeFor(^dcSchema($p(table,"#"),"Fields",field,"Type"))_val
 quit:format="Tr" ScalarStr_$$tr^dcStrUtil("_enum_"_table_"_"_field_"_"_val)
 ; TODO support date and number formatting, maybe str padding
 quit ScalarStr_$$format^dcStrUtil(val,format)
 ;
 ;
 ;
getTypeFor(type) quit:type="Time" ScalarTime
 quit:type="Date" ScalarDate
 quit:type="DateTime" ScalarDateTime
 quit:type="Id" ScalarId
 quit:type="Integer" ScalarInt 
 quit:type="Json" ScalarJson
 quit:type="Decimal" ScalarDec
 quit:type="BigInteger" ScalarBigInt
 quit:type="BigDecimal" ScalarBigDec
 quit:type="Number" ScalarNum
 quit:type="Boolean" ScalarBool
 quit:type="Binary" ScalarBin
 quit:type="BigDateTime" ScalarBigDateTime
 quit ScalarStr
 ;
 ;
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
 ;
 ; Request
 ;
 ; d local^dcConn("QUERY dcListDirect")
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
list n id,table,when,select,where,collector,cstate,cname,historical
 s table=Params("Table"),when=Params("When"),historical=Params("Historical")
 i (table="") d err^dcConn(50013) quit
 s:when="" when=$$whenNow^dcTimeUtil()
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 s:$d(Params("Select"))=0 Params("Select","Field")="Id"
 ;
 m select(0)=Params("Select")
 m where=Params("Where")
 m collector=Params("Collector")
 ;
 i collector("Name")'="" d  quit
 . s cname=collector("Name")		; "cstate" is a variable available to the collector for state across calls
 . i ^dcProg("collector",cname)="" q
 . w StartList
 . f  x "s id=$$"_^dcProg("collector",cname)_"()" q:id=""  d  q:Errors
 . . d:$$check^dcDbSelect(table,id,when,.where,historical) writeCompactFld(table,id,when,.select,0,5,historical)
 . w EndList
 ;
 i collector("Field")'="" d  quit
 . i $d(collector("Values")) d  q
 . . n values m values=collector("Values")
 . . w StartList
 . . f  s id=$$loopValues^dcDb(table,collector("Field"),.values,.cstate,when,historical) q:id=""  d
 . . . d:$$check^dcDbSelect(table,id,when,.where,historical) writeCompactFld(table,id,when,.select,0,5,historical)
 . . w EndList
 . ;
 . w StartList
 . f  s id=$$loopRange^dcDb(table,collector("Field"),collector("From"),collector("To"),.cstate,when,historical) q:id=""  d 
 . . d:$$check^dcDbSelect(table,id,when,.where,historical) writeCompactFld(table,id,when,.select,0,5,historical)
 . w EndList
 ;
 w StartList
 ;
 f  s id=$o(^dcRecord(table,id)) q:id=""  d  q:Errors
 . d:$$check^dcDbSelect(table,id,when,.where,historical) writeCompactFld(table,id,when,.select,0,5,historical)
 ;
 w EndList
 ;
 quit
 ;
 ;
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
 ;