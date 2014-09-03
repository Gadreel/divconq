dcUser ; 
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
signIn n token,user,pass,code,uid,fnd,confirmed,recoverExpire,at,tags,tag,suspect
 i Replication s token=Params("Token"),uid=Params("Uid"),at=Params("At"),confirmed=Params("Confirmed") g storeToken
 ;
 s pass=Params("Password"),user=Params("UserName"),at=Params("At"),code=Params("Code"),recoverExpire=Params("RecoverExpire")   
 s suspect=Params("Suspect")		; do something with this if login fails - see hackTrack below
 ;
 i (user="")!((pass="")&(recoverExpire="")) d err^dcConn(123) quit			
 ;
 s uid=$$loopIndex^dcDb("dcUser","dcUserName",user)
 ;
 i uid="" d err^dcConn(123) quit			
 ; 
 s:pass=$$get3^dcDb("dcUser",uid,"dcPassword") fnd=1
 ;
 i 'fnd d    ; if we get here then recoverExpire must be set
 . i recoverExpire]]$$get1^dcDb("dcUser",uid,"dcRecoverAt") q
 . i (code'="")&($$get1^dcDb("dcUser",uid,"dcConfirmCode")=code) s fnd=1		; we won't clear out the code or rAt fields - they naturally expire
 ;
 i 'fnd d err^dcConn(123) quit
 ;
 i '$$get1^dcDb("dcUser",uid,"dcConfirmed") d
 . i (code'="")&($$get1^dcDb("dcUser",uid,"dcConfirmCode")=code) s Params("Confirmed")=1,confirmed=1 q
 . d err^dcConn(124) q			
 ;
 i Errors quit       ; if any violations then do not proceed
 ;
so2 s Params("Uid")=uid
 s token=$$allocateToken(),Params("Token")=token
 ;
storeToken l +^dcSession(token)
 s ^dcSession(token,"__LastAccess")=Stamp
 s ^dcSession(token,"User")=uid
 l -^dcSession(token)
 ;
 i confirmed d set1^dcDb("dcUser",uid,"dcConfirmed",1)  
 ;d set1^dcDb("dcUser",uid,"dcLastLogin",at)   TODO create some other logging
 ;
 i Replication quit
 ;
 w StartRec
 w Field_"UserId"_ScalarStr_uid
 w Field_"FirstName"_ScalarStr_$$get3^dcDb("dcUser",uid,"dcFirstName")
 w Field_"LastName"_ScalarStr_$$get3^dcDb("dcUser",uid,"dcLastName")
 w Field_"Email"_ScalarStr_$$get3^dcDb("dcUser",uid,"dcEmail")
 w Field_"Locale"_ScalarStr_$$get1^dcDb("dcUser",uid,"dcLocale")
 w Field_"Chronology"_ScalarStr_$$get1^dcDb("dcUser",uid,"dcChronology")
 w Field_"AuthToken"_ScalarStr_token
 w Field_"AuthorizationTags"_StartList
 ;
 d loadUserTags(uid,.tags)
 f  s tag=$o(tags(tag)) q:tag=""  w ScalarStr_tag
 ;
 w EndList
 w EndRec
 ;
 quit
 ;
 ;
startSess n uid,user,token,tags,at
 s user=Params("UserName"),at=Params("At"),uid=Params("UserId")
 ;
 i (user="")&(uid="") d err^dcConn(130) quit			
 ;
 s:uid="" uid=$$loopIndex^dcDb("dcUser","dcUserName",user)
 ;
 i uid="" d err^dcConn(131) quit
 ;
 d so2
 ;
 quit
 ;
 ;
acctTaken n user,uid,sid s sid=$o(Params("Fields","dcUserName","")) 
 s:sid'="" user=Params("Fields","dcUserName",sid,"Data",0)
 i (user="") quit    ; during update, if no user name given that's fine
 ;
 s uid=$$loopIndex^dcDb("dcUser","dcUserName",user)
 i (uid'="")&(uid'=id) d err^dcConn(122) 		
 ;
 quit
 ;
 ;
verify n token,uid,tags,tag,fnd s token=Params("AuthToken"),uid=Params("UserId")
 i (token="")!(uid="") d err^dcConn(120) quit			
 ;
 l +^dcSession(token)
 s:(uid=^dcSession(token,"User")) fnd=1 
 s:fnd ^dcSession(token,"__LastAccess")=Stamp   ; touch the token, will not expire for 30 minutes
 l -^dcSession(token)
 ;
 i 'fnd d err^dcConn(121) quit
 ;
 w StartRec
 w Field_"AuthorizationTags"_StartList
 ;
 d loadUserTags(uid,.tags)
 f  s tag=$o(tags(tag)) q:tag=""  w ScalarStr_tag
 ;
 w EndList
 w EndRec
 ;
 quit
 ;
 ;
allocateToken() n tid
 ;
 l +^dcHub("Token")
 s tid=^dcHub("Token")+1
 i tid>999999999999998 s tid=0
 s ^dcHub("Token")=tid
 l -^dcHub("Token")
 ;
 q ^dcHub("Id")_"_"_$$now^dcTimeUtil()_"_"_$$lpad^dcStrUtil(tid,"0",15)_"_"_$$buildCode^dcStrUtil(12)
 ;
 ;
 ;
signOut n token,uid
 s token=Params("AuthToken")
 i (token="") d err^dcConn(117) quit		
 ;
 l +^dcSession(token)
 k ^dcSession(token)
 l -^dcSession(token)
 ;
 quit
 ;
 ;
recovery n user,code,uid,at
 s user=Params("UserName"),at=Params("At"),code=Params("Code")
 ;
 i (user="") d err^dcConn(128) quit			
 ;
 s uid=$$loopIndex^dcDb("dcUser","dcUserName",user)
 ;
 i uid="" d err^dcConn(129) quit			
 ; 
 d set1^dcDb("dcUser",uid,"dcConfirmCode",code)  
 d set1^dcDb("dcUser",uid,"dcRecoverAt",at)   
 ;
 quit
 ;
 ;
unameLookup n user,uid s user=Params("UserName")
 ;
 i (user="") d err^dcConn(118) quit			
 ;
 s uid=$$loopIndex^dcDb("dcUser","dcUserName",user)
 ;
 i (uid="") d err^dcConn(119) quit
 ;
 w StartRec
 w Field_"UserId"_ScalarStr_uid
 w EndRec
 ;
 quit
 ;
 ;
 ;
cleanup n token,cid,pid,expire,expire2
 s expire=Params("ExpireThreshold"),expire2=Params("LongExpireThreshold")
 i (expire="")!(expire2="") d err^dcConn(115) quit			
 ;
 ; TODO enhance so that it does not loop all sessions, does not scale nicely if thousands of concurrent sessions
 f  s token=$o(^dcSession(token)) q:token=""  d
 . i expire2]]^dcSession(token,"__LastAccess") d		; only lock if seems probable
 . . l +^dcSession(token)
 . . k:expire2]]^dcSession(token,"__LastAccess") ^dcSession(token)   ; check to be sure, kill expired sessions
 . . l -^dcSession(token)
 ;
 f  s cid=$o(^dcCache(cid)) q:cid=""  d
 . i expire]]^dcCache(cid,"__LastAccess") d		; only lock if seems probable
 . . l +^dcCache(cid)
 . . k:expire]]^dcCache(cid,"__LastAccess") ^dcCache(cid)   ; check to be sure, kill expired sessions
 . . l -^dcCache(cid)
 ;
 f  s pid=$o(^dcParams(pid)) q:pid=""  d
 . i expire]]^dcParams(pid,"__LastAccess") d		; only lock if seems probable
 . . l +^dcParams(pid)
 . . k:expire]]^dcParams(pid,"__LastAccess") ^dcParams(pid)   ; check to be sure, kill expired sessions
 . . l -^dcParams(pid)
 ;
 quit
 ;
 ;
 ;
 ; record composer
recAuthTags n tags,tag d loadUserTags(id,.tags)
 w StartList
 f  s tag=$o(tags(tag)) q:tag=""  w ScalarStr_tag
 w EndList
 quit
 ;
 ;
 ; select composer
selAuthTags() n tags,tag,ret d loadUserTags(id,.tags)
 s ret=StartList
 f  s tag=$o(tags(tag)) q:tag=""  s ret=ret_ScalarStr_tag
 s ret=ret_EndList
 quit ret
 ;
 ;
 ; where composer
whrAuthTags(lst) n tags,tag,i d loadUserTags(id,.tags)
 f i=1:1 s tag=$o(tags(tag)) q:tag=""  s lst(i)=tag
 quit 
 ;
 ;
 ;
loadUserTags(uid,tags,when) n tlist,tag,glist,gi,gid,i
 ;
 quit:'$$isCurrent^dcDb("dcUser",uid,when) 
 ;
 ; if you are a real user record, and not retired, then you are a User
 s tags("User")=1		
 ;
 d get^dcDb(.tlist,"dcUser",uid,"dcAuthorizationTag",when)
 f  s i=$o(tlist(i)) q:i=""  s:tlist(i)'="" tags(tlist(i))=1
 ;
 d get^dcDb(.glist,"dcUser",uid,"dcGroup",when)
 ;
 f  s gi=$o(glist(gi)) q:gi=""  d
 . s gid=glist(gi) k tlist
 . q:'$$isCurrent^dcDb("dcGroup",gid,when) 
 . d get^dcDb(.tlist,"dcGroup",gid,"dcAuthorizationTag",when)
 . f  s i=$o(tlist(i)) q:i=""  s:tlist(i)'="" tags(tlist(i))=1
 ;
 quit
 ;
 ;
 ; ###############  MISC  ###############
 ;
seed w StartRec
 w Field_"Seed"_ScalarStr_^dcHub("Seed")
 w EndRec
 quit
 ;
 ;
 ; ^dcTracker("Ip",Origin,"Score")=[level of mis-trust, where > 10 or so is too much]
 ;                             ,"BadAttempts",[username/pwhash combo])=[stamp - expires after 1 month, decrease score, never less than 0]
 ; ^dcTracker("User",[id],"Score")=[level of mis-trust, where > 10 or so is too much]
 ;                             ,"BadAttempts",[Origin/pwhash combo])=[stamp - expires after 1 month, decrease score, never less than 0]
 ;
 ;
hackTrack quit
 ;
 ;
 ;@@@dcAllocateDomain
 ; d local^dcConn("QUERY dcAllocateDomain")
 ;
 ; must be run from root domain
 ;
allocDomain n id,fields   ; TODO make ready for replication
 s id=$$allocId^dcDb("dcDomain")
 ;
 s fields("dcDomainIndex",id,"Data",0)=id
 d set^dcDb("dcDomain","00000_000000000000001",.fields)
 ;
 w StartRec_Field_"Id"_ScalarStr_id_EndRec
 ;
 quit
 ;
 ;
 ;@@@dcLoadDomains
 ; d local^dcConn("QUERY dcLoadDomains")
 ;
 ; must be run from root domain
 ;
loadDomains n now,id,select,domains
 s now=$$whenNow^dcTimeUtil()
 ;
 w StartList
 ;
 d buildSelect^dcDbQuery(.select,"Id")
 d buildSelect^dcDbQuery(.select,"dcTitle","Title")
 d buildSelect^dcDbQuery(.select,"dcName","Names")
 d buildSelect^dcDbQuery(.select,"dcObscureClass","ObscureClass")
 d buildSelect^dcDbQuery(.select,"dcObscureSeed","ObscureSeed")
 ;
 d writeRec^dcDbQuery("dcDomain","00000_000000000000001",now,.select,3,1)  ; 3 levels, 1 compact
 ;
 ; get pairs for domains
 d getKeys2^dcDb(.domains,"dcDomain","00000_000000000000001","dcDomainIndex")
 ;
 f  s id=$o(domains(id))  q:id=""  d
 . n Domain s Domain=id   ; switch domains temparily
 . i $$isRetired^dcDb("dcDomain",id) q
 . d writeRec^dcDbQuery("dcDomain",id,now,.select,3,1)  ; 3 levels, 1 compact
 ;
 w EndList  
 ;
 quit
 ;