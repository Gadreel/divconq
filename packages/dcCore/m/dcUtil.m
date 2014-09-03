dcUtil ; 
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
gd n input
 w "Global Display Utility"
prompt w !,"Name: "
 r input  quit:input=""
 d show(input)
 g prompt
 ;
show(gname) n qv,ev
 i gname'["(" s qv=gname,ev=gname
 e  d
 . i $e(gname,$l(gname))=")" s qv=gname,ev=$e(gname,1,$l(gname)-1)
 . e  s ev=gname,qv=gname_")"
 ;
 i ($d(@qv)=1)!($d(@qv)=11) w !,qv,"=",@qv
 f  s qv=$q(@qv) q:$e(qv,1,$l(ev))'=ev  w !,qv,"=",@qv
 ;
 quit
 ;
 ;
 ;
compile n input,%ZR,currRou
 w "Compile Names (*): "
 r input
 ;
 i input="" s input="*"
 ;
 d SILENT^%RSEL(input)
 ;
 f  s currRou=$o(%ZR(currRou)) q:currRou=""  d  
 . i currRou="dcCompile" q  ; do not compile self
 . i %ZR(currRou)["/r/" d
 . . w !,"Compiling: "_currRou
 . . zl currRou
 ;
 w !
 ;
 quit 
 ;
 ;@@@dcExportDomain
 ; d local^dcConn("QUERY dcExportDomain","00100_000000000000002")
 ;
export n table,cstate,id,select,now
 s now=$$whenNow^dcTimeUtil()
 ;
 w StartList
 ;
 f  s table=$o(^dcSchema(table))  q:table=""  d
 . w StartRec
 . w Field_"Table"_ScalarStr_table
 . w Field_"Records"_StartList
 . ;
 . k select s select(0,"Field")="Id"
 . f i=1:1  s fname=$o(^dcSchema(table,"Fields",fname))  q:fname=""  s select(i,"Field")=fname
 . ;
 . k cstate
 . f  s id=$$loopTable^dcDb(table,.cstate,now)  q:id=""  d
 . . d writeRec^dcDbQuery(table,id,now,.select,1,1,0,0)  ; 1 levels, 1 compact, 0 write Rec, 0 historical
 . . ;
 . . ;w StartRec
 . . ;w Field_"Id"_ScalarStr_id
 . . ;
 . . ;w EndRec
 . ;
 . w EndList   ; end of records
 . w EndRec
 ;
 w EndList   ; end of tables
 ;
 quit
 ;
