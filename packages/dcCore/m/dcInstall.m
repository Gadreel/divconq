dcInstall ; 
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
 n input
 w "All data will be lost"
 w !,"Are you sure you want to install? "
 r input
 s input=$tr(input,"Y","y")
 i $e(input,1)'="y" q
 ;
 w !,"Hub Id (5 digits, zero pad): "
 r input			
 ;
 i input'?5N w !,"Invalid Id" quit
 i input="00000" w !,"Reserved Id" quit
 ;
 d install(input)
 w !,"OK",!,!,"Sync the schema, then run d root^dcInstall"
 quit
 ;
 ; start fresh but keep Hub settings
 ;
reinstall n input,hub
 s input=^dcHub("Id")
 d install(input)
 w !,"OK",!,!,"Sync the schema, then run d root^dcInstall"
 quit
 ;
install(id) n x
 ;
 ; clean out old
 ;
 d trunc
 ;
 ; setup install
 ;
 s ^dcHub("Id")=id
 s ^dcHub("Seed")=$$buildCode^dcStrUtil(64)
 ;s ^dcHub("BackupDir")="/home/gtmuser/backup/"
 s ^dcHub("DebugLevel")="Info"
 s ^dcHub("TimeZone")="Iso/America/New_York"		
 s ^dcHub("Locale")="en_US"
 ;
 w !,"Audit level (1-4): "				; TODO check inputs
 r x
 s:x="" x=1
 i (x<1)!(x>4) w !,"Invalid Level, using 1" s x=1
 ;
 s ^dcHub("Audit")=x
 ;
 d minSchema
 ;
 quit
 ;
 ;
 ;
minSchema ; core procedures
 s ^dcProg("proc","dcPing")="ping^dcConn"
 s ^dcProg("proc","dcEcho")="echo^dcConn"
 s ^dcProg("proc","dcSchemaUpdate")="update^dcSchema"
 s ^dcProg("proc","dcSeed")="seed^dcUser"
 s ^dcProg("proc","dcCleanup")="cleanup^dcUser"
 ;
 ; core translations
 s ^dcLocale("en","_code_50000")="'{$1}' not recognized"
 s ^dcLocale("en","_code_50001")="Exception in operation '{$1}'"
 s ^dcLocale("en","_code_50002")="Missing execute context for '{$1}'"
 s ^dcLocale("en","_code_50100")="Missing backup time stamp"
 ;
 quit 
 ;
 ;
 ;
uninstall n input
 w "All data will be lost"
 w !,"Are you sure you want to continue? "
 r input
 s input=$tr(input,"Y","y")
 i $e(input,1)="y" d trunc w !,"OK"
 quit
 ;
trunc ;
 k ^dcCache
 k ^dcParams
 k ^dcXLog
 k ^dcSession
 ;
 k ^dcTextIndex
 k ^dcTextRecord
 ;
 k ^dcIndex1
 k ^dcIndex2
 k ^dcRecord
 k ^dcRecordMeta
 ;
 k ^dcRecordTasks
 k ^dcAudit
 k ^dcAuditTime
 k ^dcLog
 k ^dcLogUser
 k ^dcLogTime
 k ^dcReplication
 ;
 k ^dcLocale
 k ^dcSchema
 k ^dcHub
 k ^dcProg
 k ^dcChron
 ;
 quit 
 ;
 ; TODO move to Java install code
root n input,fields,Domain
 d makeContext^dcConn()
 ;
 w "Setup the root domain"
 ;
 w !,"Title (defaults to 'Root'): "
 r input			
 ;
 i input="" s input="Root"
 ;
 s fields("dcTitle","Data",0)=input
 ;
 s fields("dcObscureSeed","Data",0)=$$buildCode^dcStrUtil(64)
 ;
 s fields("dcName","root","Data",0)="root"
 ;
root1 w !,"Domain Name (blank to stop entering): "
 r input			
 ;
 i input'="" s fields("dcName",input,"Data",0)=input g root1
 ;
 d set^dcDb("dcDomain","00000_000000000000001",.fields)
 ;
 quit
 ;
 ;