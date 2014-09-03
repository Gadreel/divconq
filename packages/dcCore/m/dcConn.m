dcConn ; 
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
 ; Special Variables List (informational)
 ;
 ; separators:	StartRec,EndRec,StartList,EndList,Field,Scalar,End,EmptyRec,EmptyList,
 ;             	ScalarStr,ScalarJson,ScalarDec,ScalarInt,ScalarFalse,ScalarTrue,
 ;				ScalarNull,ScalarTime,ScalarDate,ScalarDateTime,ScalarId,ScalarBigInt,ScalarBigDec
 ;				ScalarNum,ScalarBool,ScalarBin
 ; ops:		Cmd,Params
 ;			Errors: 1 = true
 ;			StateMsgs: [ { Level: lvl, Code: code, Message: msg }, ... ]  
 ; context:	TaskId,HubId,UserId,Stamp,DebugLevel,GroupIds,Locale,TimeZone,Origin,
 ; 			Replication,Pid,Domain,RootDomain
 ;
 n Cmd
 n StateMsgs,Errors,TaskId,HubId,UserId,Stamp,DebugLevel,GroupIds,Locale,TimeZone,Replication
 n Origin,Pid,Audit,Domain,RootDomain
 ;
 n StartRec,EndRec,StartList,EndList,Field,Scalar,End,EmptyRec,EmptyList
 n ScalarStr,ScalarJson,ScalarDec,ScalarInt,ScalarFalse,ScalarTrue,ScalarNull
 n ScalarTime,ScalarDate,ScalarDateTime,ScalarId,ScalarBigInt,ScalarBigDec
 n ScalarNum,ScalarBool,ScalarBin,ScalarBigDateTime
 ;
 s StartRec=$c(30)
 s EndRec=$c(31)
 s EmptyRec=StartRec_EndRec
 s StartList=$c(28)
 s EndList=$c(29)
 s EmptyList=StartList_EndList
 s Field=$c(26)
 s Scalar=$c(25)
 s End=$c(23)
 ;
 s ScalarStr=Scalar_"`"
 s ScalarJson=Scalar_"*"
 s ScalarDec=Scalar_"("
 s ScalarInt=Scalar_")"
 s ScalarBigDec=Scalar_"("
 s ScalarBigInt=Scalar_")"
 s ScalarNum=Scalar_"("
 s ScalarId=Scalar_"&"
 s ScalarBool=Scalar_"!"
 s ScalarBin=Scalar_"%"
 s ScalarFalse=Scalar_"false"
 s ScalarTrue=Scalar_"true"
 s ScalarNull=Scalar_"null"
 s ScalarTime=Scalar_"`"
 s ScalarDate=Scalar_"`"
 s ScalarDateTime=Scalar_"@"
 s ScalarBigDateTime=Scalar_"$"
 ;
 s Replication=0       ; start context with replication off
 ;
 s Audit=^dcHub("Audit")    ; 1 = none, use stamp of "1", 2 = use stamps normally but don't audit, 3 = audit update, 4 = audit query also
 ;
 s $zt="d ztrap^dcConn"		; set the exception handler
 use $P:(NOECHO:WIDTH=0)
 ;
 w "HELLO"_End			; tell Java/client we are ready for commands
 ;
loop
 r Cmd 
 ;
 ; TODO add logging
 ;
 i Cmd="" g loop
 ;
 i Cmd="POLLSTATE" d  g loop
 . w StartList_StateMsgs_EndList_End
 . i Pid]]"" k ^dcParams(Pid)
 . k Params,TaskId,HubId,UserId,Stamp,DebugLevel,GroupIds,Locale,TimeZone,Errors,StateMsgs,Origin,Pid,Domain,RootDomain
 ;
 i Cmd="HALT" halt
 ;
 ; if there was an error or any other message, don't do anything else until the user issues a POLLSTATE or HALT
 ;
 i StateMsgs g loop
 ;
 i Cmd="LPARAMS" d localParams() g loop
 i Cmd="BPARAMS" d bulkParams() g loop
 i $e(Cmd,1,8)="CONTEXT " d context($e(Cmd,9,$l(Cmd))) g loop
 i $e(Cmd,1,7)="UPDATE " d execute($e(Cmd,8,$l(Cmd))) g loop
 i $e(Cmd,1,6)="QUERY " d query($e(Cmd,7,$l(Cmd))) g loop
 ;
 d err(50000,Cmd)	
 w End
 g loop
 ;
 ; support console calls for testing/debugging of procs
 ;
local(Cmd,domain) i Cmd="" w "Missing command."  quit
 n StateMsgs,Errors,TaskId,HubId,UserId,Stamp,DebugLevel,GroupIds,Locale,TimeZone,Replication
 n StartRec,EndRec,StartList,EndList,Field,Scalar,End,EmptyRec,EmptyList,Origin,Pid,Audit
 n ScalarStr,ScalarJson,ScalarDec,ScalarInt,ScalarFalse,ScalarTrue,ScalarNull,Domain,RootDomain
 n ScalarTime,ScalarDate,ScalarDateTime,ScalarId,ScalarBigInt,ScalarBigDec,ScalarBigDateTime
 n ScalarNum,ScalarBool,ScalarBin
 ;
 s Audit=^dcHub("Audit")
 ;
 ; dummy context for testing
 d makeContext("00000_000000000000001","","",domain)
 ;
 s StartRec=":{ "
 s EndRec="} "
 s EmptyRec=StartRec_EndRec
 s StartList=":[ "
 s EndList="] "
 s EmptyList=StartList_EndList
 s Field=", "
 s Scalar=": "
 s End="!"
 ;
 s ScalarStr=Scalar
 s ScalarJson=Scalar
 s ScalarDec=Scalar
 s ScalarInt=Scalar
 s ScalarBigDec=Scalar
 s ScalarBigInt=Scalar
 s ScalarNum=Scalar
 s ScalarId=Scalar
 s ScalarBool=Scalar
 s ScalarBin=Scalar
 s ScalarFalse=Scalar_"false"
 s ScalarTrue=Scalar_"true"
 s ScalarNull=Scalar_"null"
 s ScalarTime=Scalar
 s ScalarDate=Scalar
 s ScalarDateTime=Scalar
 s ScalarBigDateTime=Scalar
 ;
 w !,"Data:",!
 i $e(Cmd,1,7)="UPDATE " d execute($e(Cmd,8,$l(Cmd))) i 1
 i $e(Cmd,1,6)="QUERY " d query($e(Cmd,7,$l(Cmd))) 
 ;
 w !,"Messages:"
 w !,StartList_StateMsgs_EndList_End
 quit
 ;
 ;
ztrap i Cmd="" halt
 ;
 s $zt="d zth^dcConn"		; prevent infinite traps
 ;
 d err(50001,Cmd)
 ;
 n lvar,tnum
 s ^dcXLog=^dcXLog+1,tnum=^dcXLog
 zsh "V":lvar
 zsh "D":lvar
 zsh "L":lvar
 zsh "S":lvar
 m ^dcXLog(tnum,"zshow")=lvar
 ;
 w StateMsgs_End
zth halt
 ;
 ; collect the parameters
 ;
localParams() n data,subs
contLocal r subs#32500
 i subs="!" quit
 r data#32500
 ;
 x "s Params("_subs_")="_data
 ;
 g contLocal
 ;
 quit
 ;
 ;
 ; collect the parameters
 ;
bulkParams() n data,subs
 l +^dcParams 
 s Pid=^dcParams+1
 i Pid>999999999 s Pid=0
 s ^dcParams=Pid
 l -^dcParams
 k ^dcParams(Pid)
 s ^dcParams(Pid,"__LastAccess")=$$now^dcTimeUtil()
 ; 
contBulk r subs#32500
 i subs="!" quit
 r data#32500
 ;
 x "s ^dcParams("_Pid_","_subs_")="_data
 ;
 g contBulk
 ;
 quit
 ;
 ;  TODO fix or remove
log(level,msg,tags) quit:input=""
 n hid,lm,tag 
 ;
 s hid=^dcHub("Id")
 ;s lm=$o(^dcAudit(TaskId,Stamp,hid,"Msgs",""),-1)+1 
 ;s ^dcAudit(TaskId,Stamp,hid,"Msgs",lm,"Level")=level
 ;s ^dcAudit(TaskId,Stamp,hid,"Msgs",lm,"Msg")=msg
 ;
 ;f  s tag=$o(tags(tag))  q:tag=""  s:tags(tag)'="" ^dcAudit(TaskId,Stamp,hid,"Tags",tags(tag))=1
 ;
 quit
 ;
 ;
execute(ProcName) i (ProcName="") w End d err(50002,Cmd) quit	
 ;
 n FuncName s FuncName=^dcProg("proc",ProcName)
 i (HubId="")!(TaskId="")!(UserId="")!(Stamp="")!(FuncName="") w End d err(50002,Cmd) quit
 ;
 ; run the function
 x "d "_FuncName
 ;
 ; TODO if audit level 'None' or 'Minimal' then skip audit goto execute2
 ;
 ;n hid s hid=^dcHub("Id")
 ;
 ;s ^dcAudit(TaskId,Stamp,hid,"Operation")=$s(Errors+0=0:"Call",1:"FailedCall")
 ;m ^dcAudit(TaskId,Stamp,hid,"Params")=Params
 ;s ^dcAudit(TaskId,Stamp,hid,"Execute")=FuncName
 ;s ^dcAudit(TaskId,Stamp,hid,"UserId")=UserId
 ;s ^dcAudit(TaskId,Stamp,hid,"HubId")=HubId
 ;
 ;s ^dcAuditTime(Stamp,hid)=TaskId     ; TODO add user index?
 ;
 ;n latestTs
 ;lock +^dcReplication("Local",hid)
 ;s latestTs=^dcReplication("Local",hid)
 ;i Stamp]]latestTs s ^dcReplication("Local",hid)=Stamp
 ;lock -^dcReplication("Local",hid)
 ;
execute2 w End
 ;
 quit
 ;
 ;
query(ProcName) i (ProcName="") d err(50002,Cmd) quit		
 ;
 n FuncName s FuncName=^dcProg("proc",ProcName)
 i (HubId="")!(TaskId="")!(UserId="")!(Stamp="")!(FuncName="") w End d err(50002,Cmd) quit
 ;
 ; run the function
 x "d "_FuncName
 ;
 ; TODO if audit level 'None' or 'Minimal' or 'Update' then skip audit 
 ;
 ;n hid s hid=^dcHub("Id")
 ;
 ;s ^dcAudit(TaskId,Stamp,hid,"Operation")="Query"
 ;m ^dcAudit(TaskId,Stamp,hid,"Params")=Params
 ;s ^dcAudit(TaskId,Stamp,hid,"Query")=FuncName
 ;s ^dcAudit(TaskId,Stamp,hid,"UserId")=UserId
 ;s ^dcAudit(TaskId,Stamp,hid,"HubId")=HubId
 ;
 ;s ^dcAuditTime(Stamp,hid)=TaskId     ; TODO add user index?
 ;
 ;n latestTs
 ;lock +^dcReplication("Local",hid)
 ;s latestTs=^dcReplication("Local",hid)
 ;i Stamp]]latestTs s ^dcReplication("Local",hid)=Stamp
 ;lock -^dcReplication("Local",hid)
 ;
 w End
 ;
 quit
 ;
 ; CONTEXT [hub request comes from] [task id] [user id] [time stamp] [debug level] [locale] [timezone] [Origin] [domain]
context(data) ;
 s HubId=$p(data," ",1)
 s TaskId=$p(data," ",2)
 s UserId=$p(data," ",3)
 ;
 i ^dcHub("Audit")=1 s Stamp=1 
 e  s Stamp=$p(data," ",4) i Stamp="" s Stamp=$$allocateStamp()
 ;
 s DebugLevel=$p(data," ",5)
 i DebugLevel="" s DebugLevel=^dcHub("DebugLevel")
 ;
 s Locale=$p(data," ",6)
 i Locale="" s Locale=^dcHub("Locale")
 ;
 s TimeZone=$p(data," ",7)
 i TimeZone="" s TimeZone=^dcHub("TimeZone")
 ;
 s Origin=$p(data," ",8)
 i Origin="" s Origin="hub:"
 ;
 s Domain=$p(data," ",9)
 i Domain="00000_000000000000001" s Domain="",RootDomain=1    ; in M the root doman is null, see getDomainId to get the id
 ;i Domain'="" s Domain="#"_Domain
 quit
 ;
 ; BACKUP [timestamp]
backup n stamp,fname,script
 s stamp=Params("Stamp")
 ;
 i (stamp="") d err(50100,Cmd) quit			
 ;
 s fname=^dcHub("BackupDir")_stamp_".dat"
 s file="/usr/local/gtm/mupip backup ""*"" "_fname
 ;
 w StartRec
 w Field_"File"_ScalarStr_fname 
 w Field_"StdOut"_ScalarStr	; output from the console will be caught in our field as long as it does not contain any of the special 7 characters
 ;
 zsy file
 ;
 w EndRec
 ;
 quit
 ;
 ; dcPing
ping w StartRec_Field_"Text"_ScalarStr_"PONG"_EndRec
 quit
 ;
 ; dcEcho
echo w StartRec_Field_"Text"_ScalarStr_Params("Text")_EndRec
 quit
 ;
 ; dcFailResultTest
fail w StartRec_EndList quit
 ;
 ; dcFailHungUpTest
hungup h 1200 quit
 ; 
 ; does not help with UserId, sets to system
 ;
makeContext(uid,doff,soff,domain) ;
 ;
 s HubId=^dcHub("Id")
 s TaskId=$$allocateTaskId()
 ;
 i ^dcHub("Audit")=1 s Stamp=1 
 e  s Stamp=$$allocateStamp(doff,soff)
 s DebugLevel=^dcHub("DebugLevel")
 ;
 s UserId=uid
 i UserId="" s UserId="00000_000000000000001"
 ;
 s Locale=^dcHub("Locale")
 s TimeZone=^dcHub("TimeZone")
 ;
 s Origin="hub:"
 ;
 i domain="00000_000000000000001" s Domain="",RootDomain=1    ; in M the root doman is null, see getDomainId to get the id
 e  i domain="" s RootDomain=1 
 e  s Domain=domain
 ;
 ; TODO
 ;k GroupIds
 ;d get^dcDb(.GroupIds,"User",UserId,"Groups")
 ;
 quit
 ;
 ;
allocateTaskId() ;
 n tid,now s now=$h
 ;
 l +^dcHub("NextTask")
 s tid=^dcHub("NextTask"),^dcHub("NextTask")=tid+1
 l -^dcHub("NextTask")
 ;
 ; TODO adujst for UTC
 ;
 quit ^dcHub("Id")_"_"_$$now^dcTimeUtil(1)_"_"_$$lpad^dcStrUtil(tid,"0",15)
 ;
 ;
allocateStamp(doff,soff) n stmp
 ;
 l +^dcHub("NextStampSeq")
 s stmp=^dcHub("NextStampSeq")
 i stmp>9999 h 1 s stmp=0		; probably won't happen, but if so, rarely - make sure 1 sec has passed
 s ^dcHub("NextStampSeq")=stmp+1
 l -^dcHub("NextStampSeq")
 ;
 quit $$add^dcTimeUtil(doff,soff,1)_$$lpad^dcStrUtil(stmp,"0",4)
 ;
 ; when in root domain, there is a special (reserved) domain id to use for config
 ;
getDomainId() i Domain'="" quit Domain
 quit:RootDomain "00000_000000000000001"  
 quit "00000_000000000000000"  
 ;
 ; TODO call "log" from these 8 functions
 ;
trace(code,p1,p2,p3,p4,p5,p6) ;i (DebugLevel'="Trace")!(code="") quit
 n msg s msg=$$tr^dcStrUtil("_code_"_code,p1,p2,p3,p4,p5,p6)
 s StateMsgs=StateMsgs_StartRec_Field_"Level"_ScalarStr_"Trace"_Field_"Code"_ScalarInt_code_Field_"Occurred"_ScalarStr_$$now^dcTimeUtil(1)_Field_"Message"_ScalarStr_msg_EndRec
 quit
 ;
 ;
info(code,p1,p2,p3,p4,p5,p6) ;i (DebugLevel'="Trace")&(DebugLevel'="Info")!(code="") quit
 n msg s msg=$$tr^dcStrUtil("_code_"_code,p1,p2,p3,p4,p5,p6)
 s StateMsgs=StateMsgs_StartRec_Field_"Level"_ScalarStr_"Info"_Field_"Code"_ScalarInt_code_Field_"Occurred"_ScalarStr_$$now^dcTimeUtil(1)_Field_"Message"_ScalarStr_msg_EndRec
 quit
 ;
 ;
warn(code,p1,p2,p3,p4,p5,p6) i code="" s code=2
 n msg s msg=$$tr^dcStrUtil("_code_"_code,p1,p2,p3,p4,p5,p6)
 s StateMsgs=StateMsgs_StartRec_Field_"Level"_ScalarStr_"Warn"_Field_"Code"_ScalarInt_code_Field_"Occurred"_ScalarStr_$$now^dcTimeUtil(1)_Field_"Message"_ScalarStr_msg_EndRec
 quit
 ;
 ;
err(code,p1,p2,p3,p4,p5,p6) i code="" s code=1
 n msg s msg=$$tr^dcStrUtil("_code_"_code,p1,p2,p3,p4,p5,p6)
 s StateMsgs=StateMsgs_StartRec_Field_"Level"_ScalarStr_"Error"_Field_"Code"_ScalarInt_code_Field_"Occurred"_ScalarStr_$$now^dcTimeUtil(1)_Field_"Message"_ScalarStr_msg_EndRec,Errors=1
 quit
 ;
 ;
traceMsg(msg,code) ;i DebugLevel'="Trace" quit
 i code="" s code=0
 s StateMsgs=StateMsgs_StartRec_Field_"Level"_ScalarStr_"Trace"_Field_"Code"_ScalarInt_code_Field_"Occurred"_ScalarStr_$$now^dcTimeUtil(1)_Field_"Message"_ScalarStr_msg_EndRec
 quit
 ;
 ;
infoMsg(msg,code) ;i (DebugLevel'="Trace")&(DebugLevel'="Info") quit
 i code="" s code=0
 s StateMsgs=StateMsgs_StartRec_Field_"Level"_ScalarStr_"Info"_Field_"Code"_ScalarInt_code_Field_"Occurred"_ScalarStr_$$now^dcTimeUtil(1)_Field_"Message"_ScalarStr_msg_EndRec
 quit
 ;
 ;
warnMsg(msg,code) i code="" s code=2
 s StateMsgs=StateMsgs_StartRec_Field_"Level"_ScalarStr_"Warn"_Field_"Code"_ScalarInt_code_Field_"Occurred"_ScalarStr_$$now^dcTimeUtil(1)_Field_"Message"_ScalarStr_msg_EndRec
 quit
 ;
 ;
errMsg(msg,code) i code="" s code=1
 s StateMsgs=StateMsgs_StartRec_Field_"Level"_ScalarStr_"Error"_Field_"Code"_ScalarInt_code_Field_"Occurred"_ScalarStr_$$now^dcTimeUtil(1)_Field_"Message"_ScalarStr_msg_EndRec,Errors=1
 quit
 ;
 ;
