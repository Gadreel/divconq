package divconq.db.proc;

import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.lang.op.OperationResult;

public class SelectAdvanced implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		// TODO
		
		task.complete();
	}
	
	/*
dcDbSelect ;
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
 ;
 ; Request
 ;
 ; d local^dcConn("QUERY dcSelect")
 ;
 ;  Table			name
 ;  Offset			start to load at record N, default 0
 ;  PageSize		size of page to load, default 100
 ;  CacheId			id of the already loaded and ordered subset of data (expires after 5 minutes of non-use)
 ;  CacheEnabled	true/false
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
 ;  Order	list of fields/names to organize results by
 ;			0,
 ;				"Field"			name
 ;				"Format"		format of field if scalar
 ;				"Name"			display name of field
 ;				"ForeignField"	value field in fk relationship
 ;				"Composer"		composer script to generate content
 ;				"Direction"		direction to order in
 ;			1...
 ;
 ;  Collector
 ;			"Name"				code to execute to get collection
 ;			"Field"				if not using Code to get collection, use a Field instead
 ;			"Values"				
 ;					0 			value to match, params for script
 ;					1...
 ;			"From"				value to start at, inclusive
 ;			"To"				value to end at, exclusive
 ;
 ;	Historical	true / false
 ;
 ; Result
 ;		List of records, content based on Select
 ;
select n id,table,when,offset,psize,cid,select,where,order,collector,usecache,cstate,cname,kfld,ct,cv,sid,historical
 s table=Params("Table"),when=Params("When"),offset=Params("Offset"),psize=Params("PageSize")
 s cid=Params("CacheId"),usecache=Params("CacheEnabled"),historical=Params("Historical")
 i (table="") d err^dcConn(50013) quit
 s:offset="" offset=0
 s:psize="" psize=100
 s:when="" when=$$whenNow^dcTimeUtil()
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 s:$d(Params("Select"))=0 Params("Select")="*"
 ;
 m select=Params("Select")
 m where=Params("Where")
 m order=Params("Order")
 m collector=Params("Collector")
 ;
 i select="*" d  
 . s select(0,"Field")="Id",select(0,"Name")="Id"
 . f i=1:1  s fname=$o(^dcSchema($p(table,"#"),"Fields",fname)) q:fname=""  d
 . . s select(i,"Field")=fname,select(i,"Name")=fname
 ;
 s x=$o(order(""),-1) s x=$s(x="":0,1:x+1)
 s order(x,"Field")="Id"
 ;
 i usecache&(cid'="")&$$touchCache(cid) g writeRes
 ;
 s cid=$$getCacheId()
 ;
 i collector("Name")'="" d  g writeRes
 . s cname=collector("Name")		; "cstate" is a variable available to the collector for state across calls
 . i ^dcProg("collector",cname)="" q
 . f  x "s id=$$"_^dcProg("collector",cname)_"()" q:id=""  d  q:Errors
 . . d:$$check(table,id,when,.where,historical) addRec(cid,table,id,when,.select,.order,historical)
 ;
 i collector("Field")'="" d  g writeRes
 . i $d(collector("Values")) d  q
 . . n values m values=collector("Values")
 . . f  s id=$$loopValues^dcDb(table,collector("Field"),.values,.cstate,when,historical) q:id=""  d
 . . . d:$$check(table,id,when,.where,historical) addRec(cid,table,id,when,.select,.order,historical)
 . ;
 . f  s id=$$loopRange^dcDb(table,collector("Field"),collector("From"),collector("To"),.cstate,when,historical) q:id=""  d 
 . . d:$$check(table,id,when,.where,historical) addRec(cid,table,id,when,.select,.order,historical)
 ;
 f  s id=$o(^dcRecord(table,id)) q:id=""  d  q:Errors
 . d:$$check(table,id,when,.where,historical) addRec(cid,table,id,when,.select,.order,historical)
 ;
writeRes d:'Errors writeRecords(cid,.order,offset,psize)   
 ;
 k:'usecache ^dcCache(cid)   ; clear if cache will not be reused
 ;
 quit
 ;
 ;
 ; Request
 ;
 ; d local^dcConn("QUERY dcList")
 ;
 ;  Table			name
 ;  Offset			start to load at record N, default 0
 ;  PageSize		size of page to load, default 100
 ;  CacheId			id of the already loaded and ordered subset of data (expires after 5 minutes of non-use)
 ;  CacheEnabled	true/false
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
 ;			"Table"			for reverse foreign
 ;			"Composer"		composer script to generate content
 ;			"Select"		list of fields to query, see above
 ;					0,
 ;					1...
 ;
 ;  Order	list of fields/names to organize results by
 ;			0,
 ;				"Field"			name
 ;				"Format"		format of field if scalar
 ;				"Name"			display name of field
 ;				"ForeignField"	value field in fk relationship
 ;				"Composer"		composer script to generate content
 ;				"Direction"		direction to order in
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
 ;		List of values, content based on Select
 ;
list n id,table,when,offset,psize,cid,select,where,order,collector,usecache,cstate,cname,historical
 s table=Params("Table"),when=Params("When"),offset=Params("Offset"),psize=Params("PageSize")
 s cid=Params("CacheId"),usecache=Params("CacheEnabled"),historical=Params("Historical")
 i (table="") d err^dcConn(50013) quit
 s:offset="" offset=0
 s:psize="" psize=100
 s:when="" when=$$whenNow^dcTimeUtil()
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 s:$d(Params("Select"))=0 Params("Select","Field")="Id"
 ;
 m select(0)=Params("Select")
 m where=Params("Where")
 m order=Params("Order")
 m collector=Params("Collector")
 ;
 s x=$o(order(""),-1) s x=$s(x="":0,1:x+1)
 s order(x,"Field")="Id"
 ;
 i usecache&(cid'="")&$$touchCache(cid) g writeRes2
 ;
 s cid=$$getCacheId()
 ;
 i collector("Name")'="" d  g writeRes2
 . s cname=collector("Name")		; "cstate" is a variable available to the collector for state across calls
 . i ^dcProg("collector",cname)="" q
 . f  x "s id=$$"_^dcProg("collector",cname)_"()" q:id=""  d  q:Errors
 . . d:$$check(table,id,when,.where,historical) addFld(cid,table,id,when,.select,.order,historical)
 ;
 i collector("Field")'="" d  g writeRes
 . i $d(collector("Values")) d  q
 . . n values m values=collector("Values")
 . . f  s id=$$loopValues^dcDb(table,collector("Field"),.values,.cstate,when,historical) q:id=""  d
 . . . d:$$check(table,id,when,.where,historical) addFld(cid,table,id,when,.select,.order,historical)
 . ;
 . f  s id=$$loopRange^dcDb(table,collector("Field"),collector("From"),collector("To"),.cstate,when,historical) q:id=""  d 
 . . d:$$check(table,id,when,.where,historical) addFld(cid,table,id,when,.select,.order,historical)
 ;
 f  s id=$o(^dcRecord(table,id)) q:id=""  d  q:Errors
 . d:$$check(table,id,when,.where,historical) addFld(cid,table,id,when,.select,.order,historical)
 ;
writeRes2 d:'Errors writeRecords(cid,.order,offset,psize)   
 ;
 k:'usecache ^dcCache(cid)   ; clear if cache will not be reused
 ;
 quit
 ;
 ;
 ; get the recs in correct order
 ;
writeRecords(cid,order,offset,count) n cnt,oroot,oprts,i,fss,lss,done,end
 f  s i=$o(order(i))  q:i=""  s oprts(i)=""
 s oroot=$na(^dcCache(cid,"Data")),oroot=$e(oroot,1,$l(oroot)-1),fss=$o(order("")),lss=$o(order(""),-1)
 s end=offset+count
 ;
 w StartRec
 w Field_"CacheId"_ScalarInt_cid
 w Field_"Total"_ScalarInt_(^dcCache(cid,"Found")+0)    ; make sure we return a number
 w Field_"Offset"_ScalarInt_offset
 w Field_"PageSize"_ScalarInt_count
 w Field_"Data"_StartList
 ;
 f cnt=1:1:end d  q:done
 . n oi,tstr,ostr,iss s iss=lss
 . ; 
 . f  s oi=$o(oprts(oi)) q:oi=""  d  q:done  
 . . i (oprts(oi)="")!(oi=iss)!(oi=lss) d  i 1
 . . . s tstr=oroot_ostr_","_$s(oprts(oi)+0=oprts(oi):oprts(oi),1:""""_oprts(oi)_"""")_")"
 . . . s oprts(oi)=$o(@tstr,$s(order(oi,"Direction")="Descending":-1,1:1))
 . . ;
 . . i (oprts(oi)="")&(oi=fss) s done=1 q
 . . i oprts(oi)="" s iss=oi-1,oi="",ostr="" q       ; go try again, tell level up to $o
 . . ;
 . . s ostr=ostr_","_$s(oprts(oi)+0=oprts(oi):oprts(oi),1:""""_oprts(oi)_"""")
 . i ostr="" s done=1 q
 . s ostr=oroot_ostr_")"
 . i cnt>offset w @ostr
 ;
 w EndList_EndRec
 ;
 quit
 ;
 ;
check(table,id,when,where,historical) n res,a,b,c,aa,bb,cc,child,sub,name,x,filter,composer
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 q:'$$isCurrent^dcDb(table,id,when,historical) 0
 ;
 s res=1,name=where("Name")
 q:name="" res
 ;
 ; Complex: And | Or | Not
 ; Simple:  Equal | NotEqual | In | Any | Filter | Is | IsNot
 ;			LessThan | GreaterThan | LessThanOrEqual | GreaterThanOrEqual | Between
 ;			StartsWith | Contains 
 ;
 i name="And" d  quit res
 . f  s child=$o(where("Children",child)) q:child=""  d  q:res=0
 . . k sub m sub=where("Children",child)
 . . s res=$$check(table,id,when,.sub,historical)  
 ;
 i name="Or" d  quit res
 . f  s child=$o(where("Children",child)) q:child=""  d  q:res=1
 . . k sub m sub=where("Children",child)
 . . s res=$$check(table,id,when,.sub,historical)  
 ;
 i name="Not" d  quit res
 . f  s child=$o(where("Children",child)) q:child=""  d  q		; first child only
 . . k sub m sub=where("Children",child)
 . . s res='$$check(table,id,when,.sub,historical)  
 ;
 i name="Filter" d  quit res
 . s filter=where("Filter")
 . i (filter'="")&(^dcProg("wherefilter",filter)'="") x "s res=$$"_^dcProg("wherefilter",filter)_"()"
 ;
 ;
 i $d(where("A","Field")) d get^dcDb(.aa,table,id,where("A","Field"),when,where("A","Format")) i 1
 e  i $d(where("A","Composer")) d  i 1
 . s composer=where("A","Composer")
 . i (composer'="")&(^dcProg("wherecomposer",composer)'="") x "d "_^dcProg("wherecomposer",composer)_"(.aa)"
 e  s aa(0)=where("A","Value")
 ;
 i $d(where("B","Field"))  d get^dcDb(.bb,table,id,where("B","Field"),when,where("B","Format")) i 1 
 e  i $d(where("B","Composer")) d  i 1
 . s composer=where("B","Composer")
 . i (composer'="")&(^dcProg("wherecomposer",composer)'="") x "d "_^dcProg("wherecomposer",composer)_"(.bb)"
 e  s bb(0)=where("B","Value")
 ;
 i $d(where("C","Field"))  d get^dcDb(.cc,table,id,where("C","Field"),when,where("C","Format")) i 1
 e  i $d(where("C","Composer")) d  i 1
 . s composer=where("C","Composer")
 . i (composer'="")&(^dcProg("wherecomposer",composer)'="") x "d "_^dcProg("wherecomposer",composer)_"(.cc)"
 e  s cc(0)=where("C","Value")
 ;
 i name="Equal" d  quit res
 . f  s a=$o(aa(a)) q:a=""  s:aa(a)'=bb(a) res=0 q:res=0
 ;
 i name="NotEqual" d  quit res
 . f  s a=$o(aa(a)) q:a=""  s:aa(a)=bb(a) res=0 q:res=0
 ;
 i name="Is" d  quit res
 . f  s a=$o(aa(a)) q:a=""  s:'aa(a) res=0 q:res=0
 ;
 i name="IsNot" d  quit res
 . f  s a=$o(aa(a)) q:a=""  s:aa(a) res=0 q:res=0
 ;
 i name="LessThan" s:bb(0)']]aa(0) res=0 quit res
 ;
 i name="GreaterThan" s:aa(0)']]bb(0) res=0 quit res
 ;
 i name="LessThanOrEqual" s:aa(0)]]bb(0) res=0 quit res
 ;
 i name="GreaterThanOrEqual" s:bb(0)]]aa(0) res=0 quit res
 ;
 ; greater than or equal b, less than c
 i name="Between" s:bb(0)]]aa(0) res=0 s:cc(0)']]aa(0) res=0 quit res
 ;
 s res=0
 ;
 i name="Any" d  quit res
 . f  s a=$o(aa(a)) q:a=""  d  q:res
 . . f  s b=$o(bb(b)) q:b=""  d  q:res
 . . . s:aa(a)=bb(b) res=1
 ;
 i name="In" d  quit res
 . f  s a=$o(aa(a)) q:a=""  d  q:res
 . . s x="|"_aa(a)_"|"
 . . i bb(0)[x s res=1
 ;
 i name="StartsWith" d  quit res
 . f  s a=$o(aa(a)) q:a=""  d  q:res
 . . s x=$e(aa(a),1,$l(bb(0)))
 . . i x=bb(0) s res=1
 ;
 i name="Contains" d  quit res
 . f  s a=$o(aa(a)) q:a=""  d  q:res
 . . i aa(a)[bb(0) s res=1
 ;
 quit res
 ;
 ;
addRec(cid,table,id,when,select,order,historical) n data,val,odata,ostr,spos,rdata,fname,cstr
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 f  s spos=$o(select(spos))  q:spos=""  d
 . s val=$$getFld(table,id,when,.select,spos,5,historical)    
 . s fname=select(spos,"Name")
 . s:fname="" fname=select(spos,"Field")
 . i fname'="" s data=data_Field_fname_val,rdata(fname)=val 
 ;
 f  s spos=$o(order(spos))  q:spos=""  d
 . s fname=order(spos,"Name")		; when using name, uses select format also
 . ;
 . i (fname'="")&$d(rdata(fname)) s val=rdata(fname) 
 . e  s val=$$getFld(table,id,when,.order,spos,5,historical)
 . ;
 . s val=$$stripType(val)  ; only works on scalars, if we have a rec or list it goes all funny
 . ;
 . s:val="" val=0
 . i val+0=val s ostr=ostr_$s($l(ostr)>0:",",1:"")_val q
 . ;
 . i $l(val)>64 s val=$e(val,1,64)
 . s ostr=ostr_$s($l(ostr)>0:",",1:"")_""""_val_""""
 ;
 s cstr="^dcCache(cid,""Data"","_ostr_")"
 q:$d(@cstr)    ; already added
 ;
 s ^dcCache(cid,"Found")=^dcCache(cid,"Found")+1
 s ostr="odata("_ostr_")"
 ;
 i $zl(ostr)+$zl(data)>4000 d err^dcConn(50014) quit
 ;
 s @ostr=StartRec_data_EndRec
 m ^dcCache(cid,"Data")=odata
 ;
 quit
 ;
 ;
 ;
addFld(cid,table,id,when,select,order,historical) n data,val,odata,ostr,spos,fname
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 s ^dcCache(cid,"Found")=^dcCache(cid,"Found")+1
 s data=$$getFld(table,id,when,.select,0,5,historical)    
 ;
 f  s spos=$o(order(spos))  q:spos=""  d
 . s fname=order(spos,"Name")		; when using name, uses select format also
 . s val=$$getFld(table,id,when,.order,spos,5,historical)
 . s val=$$stripType(val)  ; only works on scalars, if we have a rec or list it goes all funny
 . ;
 . s:val="" val=0
 . i val+0=val s ostr=ostr_$s($l(ostr)>0:",",1:"")_val q
 . ;
 . i $l(val)>64 s val=$e(val,1,64)
 . s ostr=ostr_$s($l(ostr)>0:",",1:"")_""""_val_""""
 ;
 s ostr="odata("_ostr_")"
 ;
 i $l(ostr)+$l(data)>4000 d err^dcConn(50014) quit
 ;
 s @ostr=data
 m ^dcCache(cid,"Data")=odata
 ;
 quit
 ;
 ;
 ;
getCacheId() n cid l +^dcCache
 s cid=^dcCache+1
 i cid>999999999 s cid=0
 s ^dcCache=cid
 l -^dcCache
 k ^dcCache(cid)
 s ^dcCache(cid,"__LastAccess")=$$now^dcTimeUtil()
 quit cid
 ;
 ;
touchCache(cid) n res s res=0
 l +^dcCache(cid)
 i $d(^dcCache(cid)) s ^dcCache(cid,"__LastAccess")=$$now^dcTimeUtil(),res=1
 l -^dcCache(cid)
 quit res
 ;
 ; regardless of when last access, just kill it
 ;
clear n cid s cid=Params("CacheId")
 l +^dcCache(cid)
 k ^dcCache(cid)
 l -^dcCache(cid)
 quit
 ;
 ;
clearCache(cid) ;
 l +^dcCache(cid)
 k ^dcCache(cid)
 s ^dcCache(cid,"__LastAccess")=$$now^dcTimeUtil()
 l -^dcCache(cid)
 quit
 ;
killCache(cid) ;
 l +^dcCache(cid)
 k ^dcCache(cid)
 l -^dcCache(cid)
 quit
 ;
 ;
getRec(table,id,when,select,lvl,historical) n fname,i,spos,field,stamp,composer,retired,ret
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 i (id="")!'$$isCurrent^dcDb(table,id,when,historical) quit ScalarNull
 ;
 ; create the select array ("Id" is a special field, even though not in fields list):
 ;
 i select="*" d  
 . s select(0,"Field")="Id",select(0,"Name")="Id"
 . f i=1:1  s fname=$o(^dcSchema($p(table,"#"),"Fields",fname)) q:fname=""  d
 . . s select(i,"Field")=fname,select(i,"Name")=fname
 ;
 s ret=StartRec
 ;
 f  s spos=$o(select(spos))  q:spos=""  d
 . s ret=ret_Field_$s(select(spos,"Name")'="":select(spos,"Name"),1:select(spos,"Field"))
 . s ret=ret_$$getFld(table,id,when,.select,spos,lvl,historical) q
 ;
 s ret=ret_EndRec	
 ;
 quit ret
 ;
 ;
getFld(table,id,when,select,spos,lvl,historical) n field,stamp,fschema,subquery,sid,spid,ret,composer,kfld,tbl,fkx
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ; 
 i $d(select(spos,"Composer")) d  q ret
 . s composer=select(spos,"Composer")
 . i (composer'="")&(^dcProg("selcomposer",composer)'="") x "s ret=$$"_^dcProg("selcomposer",composer)_"()"
 ;
 s field=select(spos,"Field")
 i field="" quit ScalarNull
 ;
 i field="Id" d  quit ret
 . i $d(select(spos,"Table")) d  q
 . . s tbl=select(spos,"Table"),kfld=select(spos,"KeyField")
 . . i (tbl'["#")&(Domain'="") s tbl=tbl_"#"_Domain     ; support table instances
 . . ;
 . . m fschema=^dcSchema($p(tbl,"#"),"Fields",kfld)
 . . ;
 . . s subquery(0,"Field")=select(spos,"ForeignField")
 . . s subquery(0,"Format")=select(spos,"Format")
 . . ;
 . . s ret=StartList
 . . ;
 . . i 'fschema("List")&'fschema("Dynamic") d  i 1
 . . . f  s spid=$o(^dcIndex1(tbl,kfld,id,spid)) q:spid=""  d
 . . . . q:'$$isCurrent^dcDb(tbl,spid,when,historical)
 . . . . s ret=ret_$$getFld(tbl,spid,when,.subquery,0,1,historical) 
 . . e  f  s spid=$o(^dcIndex2(tbl,kfld,id,spid)) q:spid=""  d
 . . . q:'$$isCurrent^dcDb(tbl,spid,when,historical)
 . . . s ret=ret_$$getFld(tbl,spid,when,.subquery,0,1,historical) 
 . . ;
 . . s ret=ret_EndList
 . ;
 . s ret=$$format(table,field,id,select(spos,"Format")) 
 ;
 m fschema=^dcSchema($p(table,"#"),"Fields",field)
 i $d(fschema)=0 quit ScalarNull
 ;
 i table["#" s fkx="#"_$p(table,"#",2)     ; support table instances
 ;
 ; Any List
 ;
 i fschema("List") d  quit ret
 . s ret=StartList
 . ;
 . f  s sid=$o(^dcRecord(table,id,field,sid)) q:sid=""  d
 . . s stamp=$$getStamp4^dcDb(table,id,field,sid,when) q:stamp="" 
 . . ;
 . . s spid=$s($d(fschema("ForeignKey")):^dcRecord(table,id,field,sid,stamp,"Data",0),1:"")
 . . i (spid'="") d  i $e(ret,1)="+" s ret=$e(ret,2,$l(ret)) q
 . . . s ret=$$getForeign(fschema("ForeignKey")_fkx,spid,when,.select,spos,lvl,historical) 
 . . ;
 . . s ret=ret_$$format(table,field,^dcRecord(table,id,field,sid,stamp,"Data",0),select(spos,"Format"))
 . ;
 . s ret=ret_EndList
 ;
 ; DynamicStatic
 ;
 i fschema("Dynamic") d  quit ret
 . s sid=$$get3sid^dcDb(table,id,field,when,"",historical) 
 . s stamp=$$getStamp3^dcDb(table,id,field,sid,when,historical)
 . i stamp="" s ret=ScalarNull q
 . ;
 . s spid=$s($d(fschema("ForeignKey")):^dcRecord(table,id,field,sid,stamp,"Data",0),1:"")
 . i (spid'="") d  i $e(ret,1)="+" s ret=$e(ret,2,$l(ret)) q
 . . s ret=$$getForeign(fschema("ForeignKey")_fkx,spid,when,.select,spos,lvl,historical) 
 . ;
 . s ret=ret_$$format(table,field,^dcRecord(table,id,field,sid,stamp,"Data",0),select(spos,"Format"))
 ;
 ; StaticScalar
 ;
 s stamp=$$getStamp1^dcDb(table,id,field) quit:stamp="" ScalarNull
 ;
 s spid=$s($d(fschema("ForeignKey")):^dcRecord(table,id,field,stamp,"Data",0),1:"")
 i (spid'="") d  i $e(ret,1)="+" quit $e(ret,2,$l(ret))
 . s ret=$$getForeign(fschema("ForeignKey")_fkx,spid,when,.select,spos,lvl,historical) 
 ;
 s ret=ret_$$format(table,field,^dcRecord(table,id,field,stamp,"Data",0),select(spos,"Format"))
 ;
 quit ret
 ;
 ;
 ; return true if the value is consumed (written or ignored)
getForeign(table,id,when,select,spos,lvl,historical) n subquery,ret
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 q:'$$isCurrent^dcDb(table,id,when,historical) "+"_ScalarNull
 ;
 i (lvl>1)&$d(select(spos,"Select")) d  quit "+"_ret
 . m subquery=select(spos,"Select")
 . s ret=$$getRec(table,id,when,.subquery,lvl-1,historical) 
 ;
 i $d(select(spos,"ForeignField")) d  quit "+"_ret
 . s subquery(0,"Field")=select(spos,"ForeignField")
 . s subquery(0,"Format")=select(spos,"Format")
 . s ret=$$getFld(table,id,when,.subquery,0,1,historical) 
 ;
 quit "-"
 ;
 ;
 ;
format(table,field,val,format) i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 s val=$$format^dcDbQuery(table,field,val,format)
 i $l(val)>264 quit $e(val,1,264)_"..."
 quit val
 ;
 ;
 ;
 ;
stripType(val) quit $s(val=ScalarNull:"",val=ScalarTrue:1,val=ScalarFalse:0,1:$e(val,3,$l(val)))
 ;
 ;
 ;
	 * 
	 */
}
