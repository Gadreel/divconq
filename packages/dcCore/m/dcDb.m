dcDb ; 
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
 ; Static Scalar
set1(table,id,field,value) n fields
 s fields(field,"Data",0)=value
 d set(table,.id,.fields)
 quit
 ;
 ; 
 ; Static List
set2(table,id,field,sid,value) n fields
 s fields(field,sid,"Data",0)=value
 d set(table,.id,.fields)
 quit
 ;
 ;
 ; Dynamic Scalar
set3(table,id,field,sid,value,from) n fields
 s fields(field,sid,"Data",0)=value
 s:from'="" fields(field,sid,"From")=from
 d set(table,.id,.fields)
 quit
 ;
 ; 
 ; Dynamic List
set4(table,id,field,sid,value,from,to) n fields 
 s fields(field,sid,"Data",0)=value
 s:from'="" fields(field,sid,"From")=from
 s:to'="" fields(field,sid,"To")=to
 d set(table,.id,.fields)
 quit
 ;
 ;
 ; 
set(table,id,fields) n field,val,fschema,schtab
 ;
 i table="" d err^dcConn(50005) quit	
 ;
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 s schtab=$p(table,"#")
 i $d(^dcSchema(schtab))=0 d err^dcConn(50006) quit
 i $d(fields)<10 d err^dcConn(50008) quit
 ;
 i Errors quit
 ;
 i id="" d
 . n hid s hid=^dcHub("Id")
 . ; use common Id's across all domains so that merge works and so that 
 . ; sys domain records (users) can be reused across domains
 . l +^dcRecordMeta(schtab,"Id",hid)
 . s id=^dcRecordMeta(schtab,"Id",hid)+1,^dcRecordMeta(schtab,"Id",hid)=id
 . l -^dcRecordMeta(schtab,"Id",hid)
 . s id=hid_"_"_$$lpad^dcStrUtil(id,"0",15)
 ;
 ; works with replication, only counts once
 ;
 i $d(^dcRecord(table,id))=0 d
 . l +^dcRecordMeta(table,"Count")
 . s ^dcRecordMeta(table,"Count")=^dcRecordMeta(table,"Count")+1
 . l -^dcRecordMeta(table,"Count")
 ;
 l +^dcRecord(table,id)
 ;
 ; if marked for delete then skip, records cannot be undeleted and id cannot be reused
 i $$isDeleted(table,id) l -^dcRecord(table,id) d err^dcConn(50009) quit			
 ;
 ; TODO start transaction, roll back if any errors - should be no errors at this point
 ; be sure we don't use increments in indexing if we do this...
 ;
 ; once we start setting values, don't stop on errors - there is no 
 ; undoing of the previous sets so just do our best
 f  s field=$o(fields(field)) q:field=""  d
 . i $d(^dcSchema(schtab,"Fields",field))=0 d warn^dcConn(50010) q
 . ;
 . k fschema m fschema=^dcSchema(schtab,"Fields",field)
 . n oval,lstamp,sid
 . ;
 . ; --------------------------------------
 . ; StaticScalar handling
 . ;   fields([field name],"Data",0) = [value]
 . ;   fields([field name],"Tags") = [value]
 . ;   fields([field name],"Retired") = [value]
 . ; --------------------------------------
 . ;
 . i 'fschema("List")&'fschema("Dynamic") d  q
 . . i Audit=1 s oval=^dcRecord(table,id,field,Stamp,"Data",0) k ^dcRecord(table,id,field,Stamp)
 . . ;
 . . m ^dcRecord(table,id,field,Stamp)=fields(field)
 . . ;
 . . q:'fschema("Indexed") 
 . . ;
 . . ; --------------------------------------
 . . ; StaticScalar indexing
 . . ; --------------------------------------
 . . ;
 . . ; the value for indexing, index on only the first 64 bytes because of M limitations
 . . s val=$$val2Ndx(fields(field,"Data",0))
 . . ;
 . . ; if already set (equal value) then skip
 . . ;
 . . i val'="" q:^dcIndex1(table,field,val,id) 
 . . ;
 . . i Audit>1 d  q:lstamp]]Stamp 
 . . . s lstamp=$o(^dcRecord(table,id,field,""),-1)
 . . . ;
 . . . ; is there a more recent value, if so then skip
 . . . ;
 . . . q:lstamp]]Stamp 
 . . . ;
 . . . ; is there an older value, if so then check if equal
 . . . ;
 . . . s lstamp=$o(^dcRecord(table,id,field,Stamp),-1)   
 . . . i lstamp'="" s oval=^dcRecord(table,id,field,lstamp,"Data",0)
 . . ;
 . . ; remove old value from index
 . . ;
 . . i oval'="" d
 . . . s oval=$$val2Ndx(oval)
 . . . l +^dcIndex1(table,field,oval)
 . . . n ocnt s ocnt=^dcIndex1(table,field,oval)-1
 . . . i ocnt<1 k ^dcIndex1(table,field,oval)
 . . . e  s ^dcIndex1(table,field,oval)=ocnt k ^dcIndex1(table,field,oval,id)
 . . . l -^dcIndex1(table,field,oval)
 . . ;
 . . ; don't index null
 . . i val="" q
 . . ;
 . . ; if not already set in index (which should not be by now in logic)
 . . ;
 . . l +^dcIndex1(table,field,val)
 . . s ^dcIndex1(table,field,val,id)=1
 . . s ^dcIndex1(table,field,val)=^dcIndex1(table,field,val)+1
 . . l -^dcIndex1(table,field,val)
 . ;
 . ; --------------------------------------
 . ;
 . ; Handling for other types
 . ;
 . ; StaticList handling
 . ;   fields([field name],sid,"Data",0) = [value]
 . ;   fields([field name],sid,"Tags") = [value]			|value1|value2|etc...
 . ;   fields([field name],sid,"Retired") = [value]			|value1|value2|etc...
 . ;
 . ; DynamicScalar handling
 . ;   fields([field name],sid,"Data",0) = [value]
 . ;   fields([field name],sid,"From") = [value]			null means always was
 . ;   fields([field name],sid,"Tags") = [value]
 . ;   fields([field name],sid,"Retired") = [value]			|value1|value2|etc...
 . ;
 . ; DynamicList handling
 . ;   fields([field name],sid,"Data",0) = [value]
 . ;   fields([field name],sid,"From") = [value]			null means always was
 . ;   fields([field name],sid,"To") = [value]				null means always will be
 . ;   fields([field name],sid,"Tags") = [value]
 . ;   fields([field name],sid,"Retired") = [value]			|value1|value2|etc...
 . ; --------------------------------------
 . ;
 . f  s sid=$o(fields(field,sid)) q:sid=""  d
 . . i Audit=1 s oval=^dcRecord(table,id,field,sid,Stamp,"Data",0) k ^dcRecord(table,id,field,sid,Stamp)
 . . ;
 . . m ^dcRecord(table,id,field,sid,Stamp)=fields(field,sid)
 . . ;
 . . q:'fschema("Indexed")
 . . ;
 . . ; --------------------------------------
 . . ; StaticList and Dynamic indexing
 . . ; --------------------------------------
 . . ; the value for indexing, index on only the first 64 bytes because of M limitations
 . . s val=$$val2Ndx(fields(field,sid,"Data",0))    
 . . ;
 . . ; if already set (equal value) then skip
 . . ;
 . . i val'="" q:^dcIndex2(table,field,val,id,sid) 
 . . ;
 . . i Audit>1 d  q:lstamp]]Stamp 
 . . . s lstamp=$o(^dcRecord(table,id,field,sid,""),-1)
 . . . ;
 . . . ; is there a more recent value, if so then skip
 . . . ;
 . . . q:lstamp]]Stamp 
 . . . ;
 . . . ; is there an older value, if so then check if equal
 . . . ;
 . . . s lstamp=$o(^dcRecord(table,id,field,sid,Stamp),-1)    
 . . . i lstamp'="" s oval=^dcRecord(table,id,field,sid,lstamp,"Data",0)
 . . ;
 . . ; remove old value from index
 . . ;
 . . i oval'="" d
 . . . s oval=$$val2Ndx(oval)
 . . . l +^dcIndex2(table,field,oval)
 . . . n ocnt s ocnt=^dcIndex2(table,field,oval)-1
 . . . i ocnt<1 k ^dcIndex2(table,field,oval)
 . . . e  s ^dcIndex2(table,field,oval)=ocnt k ^dcIndex2(table,field,oval,id,sid)
 . . . l -^dcIndex2(table,field,oval)
 . . ;
 . . ; don't index null
 . . i val="" q
 . . ;
 . . ; set new value in index 
 . . ;
 . . l +^dcIndex2(table,field,val)
 . . s ^dcIndex2(table,field,val,id,sid)=1
 . . s ^dcIndex2(table,field,val)=^dcIndex2(table,field,val)+1
 . . l -^dcIndex2(table,field,val)
 ;
 ; --------------------------------------
 ; auditing TODO
 ; --------------------------------------
 ;
 ;i TaskId'="" s ^dcRecordTasks(table,id,TaskId)=1
 ;
 l -^dcRecord(table,id)
 ;
 quit 
 ;
 ;
 ; 
allocId(table) n fschema,schtab,id,hid
 ;
 i table="" d err^dcConn(50005) quit	
 ;
 s schtab=$p(table,"#")
 i $d(^dcSchema(schtab))=0 d err^dcConn(50006) quit
 ;
 s hid=^dcHub("Id")
 ; use common Id's across all domains so that merge works and so that 
 ; sys domain records (users) can be reused across domains
 l +^dcRecordMeta(schtab,"Id",hid)
 s id=^dcRecordMeta(schtab,"Id",hid)+1,^dcRecordMeta(schtab,"Id",hid)=id
 l -^dcRecordMeta(schtab,"Id",hid)
 s id=hid_"_"_$$lpad^dcStrUtil(id,"0",15)
 ;
 quit id
 ;
 ; don't run when Java connector is active
indexTable(table) n field
 f  s field=$o(^dcSchema($p(table,"#"),"Fields",field)) q:field=""  d indexField(table,field)
 quit
 ;
 ; don't run when Java connector is active
indexField(table,field) n val,id,stamp,sid,fschema
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 m fschema=^dcSchema($p(table,"#"),"Fields",field)
 ;
 quit:'fschema("Indexed") 
 ;
 i 'fschema("List")&'fschema("Dynamic") k ^dcIndex1(table,field)
 e  k ^dcIndex2(table,field)
 ;
 f  s id=$o(^dcRecord(table,id)) q:id=""  d
 . i 'fschema("List")&'fschema("Dynamic") d  q
 . . s stamp=$o(^dcRecord(table,id,field,""),-1) q:stamp=""   
 . . s val=^dcRecord(table,id,field,stamp,"Data",0)
 . . s val=$$val2Ndx(val)    
 . . ;
 . . ; don't index null
 . . i val="" q
 . . ;
 . . s ^dcIndex1(table,field,val,id)=1
 . . s ^dcIndex1(table,field,val)=^dcIndex1(table,field,val)+1
 . ;
 . f  s sid=$o(^dcRecord(table,id,field,sid),-1) q:sid=""  d
 . . s stamp=$o(^dcRecord(table,id,field,sid,""),-1) q:stamp="" 	
 . . ;
 . . s val=^dcRecord(table,id,field,sid,stamp,"Data",0)
 . . s val=$$val2Ndx(val)    
 . . ;
 . . ; don't index null
 . . i val="" q
 . . ;
 . . s ^dcIndex2(table,field,val,id,sid)=1
 . . s ^dcIndex2(table,field,val)=^dcIndex2(table,field,val)+1
 ;
 quit
 ;
 ;
loadFieldSch(fschema,table,fld)
 m fschema=^dcSchema($p(table,"#"),"Fields",fld)
 quit
 ;
 ; check not only retired, but if this record was active during the period of time
 ; indicated by "when".  If a record has no From then it is considered to be 
 ; active indefinately in the past, prior to To.  If there is no To then record
 ; is active current and since From.
isCurrent(table,id,when,historical) n from,to
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 quit:$$isRetired(table,id) 0
 i when="" quit 1
 ; 
 i 'historical s to=$$get1(table,id,"To") quit:(to'="")&(to']]when) 0
 ;
 s from=$$get1(table,id,"From")
 quit:(from'="")&(from]]when) 0
 ;
 quit 1
 ;
 ;
 ; Retired, Deleted or Missing - all return true
isRetired(table,id) i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 quit $$get1(table,id,"Retired")!(^dcRecord(table,id)=-1)!($d(^dcRecord(table,id))=0)
 ;
 ;
 ;
isDeleted(table,id) i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 quit (^dcRecord(table,id)=-1)
 ;
 ;
 ;
val2Ndx(val) quit $e(val,1,64)
 ;
 ;
 ; for Static Scalar
get1(table,id,field,format) n stmp,val quit:field="Id" id
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 s stmp=$$getStamp1(table,id,field) quit:stmp="" ""
 ;
 s val=^dcRecord(table,id,field,stmp,"Data",0)
 i format'="" s val=$$format^dcDbQuery(table,field,val,format)
 ;
 quit val
 ;
 ;
 ; for Static Scalar
getDeep1(ret,pos,table,id,field) n stmp 
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 s stmp=$$getStamp1(table,id,field) 
 m:stmp'="" ret(pos)=^dcRecord(table,id,field,stmp)
 quit 
 ;
 ;
 ; Static Scalar
getStamp1(table,id,field) n stamp
 i (table="")!(id="")!(field="") quit ""
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 s stamp=$o(^dcRecord(table,id,field,""),-1) quit:stamp="" ""
 i ^dcRecord(table,id,field,stamp,"Retired") quit ""
 quit stamp
 ;
 ;
 ; for Static List
get2(table,id,field,sid,format) n stmp,val
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 s stmp=$$getStamp2(table,id,field,sid) quit:stmp="" ""
 ;
 s val=^dcRecord(table,id,field,sid,stmp,"Data",0)
 i format'="" s val=$$format^dcDbQuery(table,field,val,format)
 ;
 quit val
 ;
 ;
 ; for Static List
getDeep2(ret,pos,table,id,field,sid) n stmp
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 s stmp=$$getStamp2(table,id,field,sid) 
 m:stmp'="" ret(pos)=^dcRecord(table,id,field,sid,stmp)
 quit 
 ;
 ;
 ; for Static List
getKeys2(ret,table,id,field) n stmp,sid
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 f  s sid=$o(^dcRecord(table,id,field,sid)) q:sid=""  d
 . s stmp=$o(^dcRecord(table,id,field,sid,""),-1) q:stmp="" 
 . i ^dcRecord(table,id,field,sid,stmp,"Retired") q
 . s ret(sid)=^dcRecord(table,id,field,sid,stmp,"Data",0)
 quit 
 ;
 ;
 ; Static List
getStamp2(table,id,field,sid) n stamp
 i (table="")!(id="")!(field="")!(sid="") quit ""
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 s stamp=$o(^dcRecord(table,id,field,sid,""),-1) quit:stamp="" ""
 i ^dcRecord(table,id,field,sid,stamp,"Retired") quit ""
 quit stamp
 ;
 ;
 ; for Dynamic Scalar
get3(table,id,field,when,format,historical) n stamp,val,sid
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 s sid=$$get3sid(table,id,field,when,format,historical) quit:sid="" ""
 s stamp=$$getStamp3(table,id,field,sid,when,historical) quit:stamp="" ""
 ;
 s val=^dcRecord(table,id,field,sid,stamp,"Data",0)
 i format'="" s val=$$format^dcDbQuery(table,field,val,format)
 ;
 quit val
 ;
 ;
 ; for Dynamic Scalar
getDeep3(ret,pos,table,id,field,when,format,historical) n stamp,sid
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 s sid=$$get3sid(table,id,field,when,format,historical) quit:sid="" 
 s stamp=$$getStamp3(table,id,field,sid,when,historical) quit:stamp="" 
 m ret(pos)=^dcRecord(table,id,field,sid,stamp)
 quit 
 ;
 ;
 ; for Dynamic Scalar, get the matching subid
get3sid(table,id,field,when,format,historical) n stamp,sid,to,from,tidx
 i (table="")!(id="")!(field="") quit ""
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 s:when="" when=$$whenNow^dcTimeUtil()
 ;
 i 'historical s to=$$get1(table,id,"To") i (to'="")&(to']]when) quit ""
 ;
 f  s sid=$o(^dcRecord(table,id,field,sid)) q:sid=""  d
 . s stamp=$o(^dcRecord(table,id,field,sid,""),-1) q:stamp="" 
 . i ^dcRecord(table,id,field,sid,stamp,"Retired") q
 . s from=^dcRecord(table,id,field,sid,stamp,"From")
 . s:from="" from=$$get1(table,id,"From")   ; default to record's from
 . s:from="" from=1  ; fallback to "forever"
 . s tidx(from)=sid
 ;
 s sid=tidx(when)
 i sid="" s sid=tidx($o(tidx(when),-1))
 ;
 quit sid
 ;
 ;
 ; Dynamic Scalar
getStamp3(table,id,field,sid,when,historical) n stamp,to,from
 i (table="")!(id="")!(field="")!(sid="") quit ""
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 s stamp=$o(^dcRecord(table,id,field,sid,""),-1) quit:stamp="" ""
 i ^dcRecord(table,id,field,sid,stamp,"Retired") quit ""
 ;
 quit:when="" stamp
 ;
 ; TODO resolve if we want this, I think not
 ;s:'historical to=^dcRecord(table,id,field,sid,stamp,"To")
 ;i 'historical&(to="") s to=$$get1(table,id,"To")
 ;i 'historical&(to'="")&(to']]when) quit ""
 ;
 s from=^dcRecord(table,id,field,sid,stamp,"From")
 i (from="") s from=$$get1(table,id,"From")
 i (from'="")&(from]]when) quit ""
 ;
 quit stamp
 ;
 ;
 ; for Dynasmic List (will work for Static List, just less effecient)
get4(table,id,field,sid,when,format) n stmp,val
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 s stmp=$$getStamp4(table,id,field,sid,when) quit:stmp="" ""
 ;
 s val=^dcRecord(table,id,field,sid,stmp,"Data",0)
 i format'="" s val=$$format^dcDbQuery(table,field,val,format)
 ;
 quit val
 ;
 ;
 ; for Dynamic List (will work for Static List, just less effecient)
getDeep4(ret,pos,table,id,field,sid,when,format) n stmp
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 s stmp=$$getStamp4(table,id,field,sid,when) quit:stmp="" 
 m ret(pos)=^dcRecord(table,id,field,sid,stmp)
 quit 
 ;
 ;
 ; Dynamic List  (will work for Static List, just less effecient)
getStamp4(table,id,field,sid,when) n stamp,to,from
 i (table="")!(id="")!(field="")!(sid="") quit ""
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 s stamp=$o(^dcRecord(table,id,field,sid,""),-1) quit:stamp="" ""
 i ^dcRecord(table,id,field,sid,stamp,"Retired") quit ""
 ;
 quit:when="" stamp
 ;
 s to=^dcRecord(table,id,field,sid,stamp,"To")
 i (to="") s to=$$get1(table,id,"To")
 i (to'="")&(to']]when) quit ""
 ;
 s from=^dcRecord(table,id,field,sid,stamp,"From")
 i (from="") s from=$$get1(table,id,"From")
 i (from'="")&(from]]when) quit ""
 ;
 quit stamp
 ;
 ;
 ; for any type, returns in list (with Scalar that is a list of 1)
get(ret,table,id,field,when,format,historical) i (table="")!(id="")!(field="") quit
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 n fschema m fschema=^dcSchema($p(table,"#"),"Fields",field)
 i 'fschema("List")&'fschema("Dynamic") s ret(0)=$$get1(table,id,field,format) quit
 i 'fschema("List")&fschema("Dynamic") s ret(0)=$$get3(table,id,field,when,format,historical) quit
 ;
 n sid,i,v s i=0
 f  s sid=$o(^dcRecord(table,id,field,sid)) q:sid=""  d
 . s v=$$get4(table,id,field,sid,when,format)
 . i v'="" s ret(i)=v,i=i+1
 ;
 quit
 ;
 ;
 ; for any type, returns in list (with Scalar that is a list of 1)
getDeep(ret,table,id,field,when,historical) i (table="")!(id="")!(field="") quit
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 n fschema m fschema=^dcSchema($p(table,"#"),"Fields",field)
 i 'fschema("List")&'fschema("Dynamic") d getDeep1(.ret,0,table,id,field) quit
 i 'fschema("List")&fschema("Dynamic") d getDeep3(.ret,0,table,id,field,when,historical) quit
 ;
 n sid,i
 f i=0:1  s sid=$o(^dcRecord(table,id,field,sid)) q:sid=""  d
 . d getDeep4(.ret,i,table,id,field,sid,when)
 ;
 quit
 ;
 ;
 ; check to see if value is current with the given when+historical settings
has(value,table,id,field,when,format,historical) i (table="")!(id="")!(field="") quit 0
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 n fschema m fschema=^dcSchema($p(table,"#"),"Fields",field)
 i 'fschema("List")&'fschema("Dynamic") quit ($$get1(table,id,field,format)=value)
 i 'fschema("List")&fschema("Dynamic") quit ($$get3(table,id,field,when,format,historical)=value)
 ;
 n sid,fnd
 f  s sid=$o(^dcRecord(table,id,field,sid)) q:sid=""  d  q:fnd
 . s:($$get4(table,id,field,sid,when,format)=value) fnd=1
 ;
 quit fnd
 ;
 ;
 ; get all (unique) id's associated with a given value-field pair
loopTable(table,cstate,when,historical) i (table="") quit ""
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 n fschema,id,fnd,nval
 ;
 s id=cstate("Id")
 ;
 f  d  q:fnd!(id="")
 . ; get next 
 . s id=$o(^dcRecord(table,id))
 . ; skip if none found
 . q:id=""
 . ; do not include records that are not current to 'when'
 . q:'$$isCurrent(table,id,when,historical)
 . ;
 . s fnd=1
 ;
 s cstate("Id")=id
 ;
 quit id
 ;
 ;
 ; get all (unique) id's associated with a given value-field pair
loopIndex(table,field,value,cstate,when,historical) i (table="")!(value="")!(field="") quit ""
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 n fschema,id,fnd,nval
 m fschema=^dcSchema($p(table,"#"),"Fields",field)
 quit:fschema("Indexed")'=1 ""		; if not indexed, cannot use
 ;
 s nval=$$val2Ndx(value)
 s id=cstate("Id")
 ;
 f  d  q:fnd!(id="")
 . ; get next id for static scalar
 . i 'fschema("List")&'fschema("Dynamic") s id=$o(^dcIndex1(table,field,nval,id)) i 1    
 . ; else get next id for all others
 . e  s id=$o(^dcIndex2(table,field,nval,id))     
 . ; skip if none found
 . q:id=""
 . ; do not include records that are not current to 'when'
 . q:'$$isCurrent(table,id,when,historical)
 . ; check to see that a value is visible for when+historical, if not then don't add it
 . ; also, since nval may be a truncated version, 
 . s:$$has^dcDb(value,table,id,field,when,"",historical) fnd=1
 ;
 s cstate("Id")=id
 ;
 quit id
 ;
 ;
 ; get all (non-unique) id's associated with a range of values
loopRange(table,field,from,to,cstate,when,historical) i (table="")!(field="") quit "" 
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 n fschema,cv,id,fnd,done
 m fschema=^dcSchema($p(table,"#"),"Fields",field)
 quit:fschema("Indexed")'=1 ""		; if not indexed, cannot use
 ;
 s from=$$val2Ndx(from)
 s to=$$val2Ndx(to)
 ;
 s cv=cstate("Current")
 ;
 f  d  q:fnd!done
 . ; if id is null in state then we need to go to next value
 . i cstate("Id")="" d
 . . i (cv="")&(from'="") s cv=from
 . . e  i 'fschema("List")&'fschema("Dynamic") s cv=$o(^dcIndex1(table,field,cv))
 . . e  s cv=$o(^dcIndex2(table,field,cv))
 . i (cv="")!((to'="")&(cv]]to)) s done=1,cv="" q   ; don't go past 'to'
 . s id=$$loopIndex(table,field,cv,.cstate,when,historical)
 . i id'="" s fnd=1
 ;
 s cstate("Current")=cv
 ;
 quit id
 ;
 ;
 ; get all (non-unique) id's associated with a range of values
loopValues(table,field,values,cstate,when,historical) i (table="")!(field="") quit "" 
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 n fschema,cv,id,fnd,done
 m fschema=^dcSchema($p(table,"#"),"Fields",field)
 quit:fschema("Indexed")'=1 ""		; if not indexed, cannot use
 ;
 s cv=cstate("Current")
 ;
 f  d  q:fnd!done
 . ; if id is null in state then we need to go to next value
 . i cstate("Id")="" s cv=$o(values(cv)) 
 . i (cv="") s done=1 q   
 . s id=$$loopIndex(table,field,values(cv),.cstate,when,historical)
 . i id'="" s fnd=1
 ;
 s cstate("Current")=cv
 ;
 quit id
 ;
 ;
 ; TODO skip fields with large data 
loadAll(table,id,fields) n field,stamp,sid
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 f  s field=$o(^dcRecord(table,id,field))  q:field=""  d
 . k fschema  
 . m fschema=^dcSchema($p(table,"#"),"Fields",field)
 . i 'fschema("List")&'fschema("Dynamic") d  q
 . . s stamp=$o(^dcRecord(table,id,field,""),-1)	; copy only the latest
 . . m fields(field)=^dcRecord(table,id,field,stamp)
 . ;
 . f  s sid=$o(^dcRecord(table,id,field,sid))  q:sid=""  d
 . . s stamp=$o(^dcRecord(table,id,field,sid,""),-1)	; copy only the latest
 . . m fields(field)=^dcRecord(table,id,field,sid,stamp)
 ;
 quit 
 ;
 ;