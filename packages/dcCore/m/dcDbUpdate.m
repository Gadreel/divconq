dcDbUpdate ; 
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
insertRec n id,fields
 ;
 d runFilter("Insert") quit:Errors  ; if any violations in filter then do not proceed
 ;
 m fields=Params("Fields")
 d set^dcDb(Params("Table"),.id,.fields) quit:Errors
 ;
 s Params("Id")=id 
 ;
 w StartRec
 w Field_"Id"_ScalarStr_id
 w EndRec
 ;
 quit 
 ;
 ;
 ;@@@dcUpdateRecord
 ;
 ; d local^dcConn("UPDATE dcUpdateRecord")
 ;
updateRec n id,fields s id=Params("Id") i (id="") d err^dcConn(50007) quit
 ;
 d runFilter("Update") quit:Errors  ; if any violations in filter then do not proceed
 ;
 m fields=Params("Fields")
 d set^dcDb(Params("Table"),id,.fields) quit:Errors
 ;
 quit 
 ;
 ;
dupRec n id,nid,fields s id=Params("Id") i (id="") d err^dcConn(50007) quit
 ;
 d runFilter("Insert") quit:Errors  ; if any violations in filter then do not proceed
 ;
 d loadAll^dcDb(Params("Table"),id,.fields) quit:Errors
 ;
 d set^dcDb(Params("Table"),.nid,.fields) quit:Errors
 ;
 s Params("Id")=nid 
 m Params("Fields")=fields		; for replication
 ;
 w StartRec
 w Field_"Id"_ScalarStr_id
 w EndRec
 ;
 quit 
 ;
 ;
 ; --------------------------------------
 ;
 ;	^dcParams(Pid,"Table") = [table]
 ;	^dcParams(Pid,"Id") = [id]
 ;	^dcParams(Pid,"Field") = [name]
 ;	^dcParams(Pid,"Filter") = [name]
 ;	^dcParams(Pid,"Extra") = [struct]
 ;
 ; StaticScalar handling
 ;   ^dcParams(Pid,"Content","Data",0) = [value]
 ;   ^dcParams(Pid,"Content","Tags") = [value]
 ;
 ; StaticList handling
 ;   ^dcParams(Pid,"Content",sid,"Data",0) = [value]
 ;   ^dcParams(Pid,"Content",sid,"Tags") = [value]			|value1|value2|etc...
 ;
 ; DynamicScalar handling
 ;   ^dcParams(Pid,"Content",sid,"Data",0) = [value]
 ;   ^dcParams(Pid,"Content",sid,"From") = [value]			null means always was
 ;   ^dcParams(Pid,"Content",sid,"Tags") = [value]
 ;
 ; DynamicList handling
 ;   ^dcParams(Pid,"Content",sid,"Data",0) = [value]
 ;   ^dcParams(Pid,"Content",sid,"From") = [value]			null means always was
 ;   ^dcParams(Pid,"Content",sid,"To") = [value]				null means always will be
 ;   ^dcParams(Pid,"Content",sid,"Tags") = [value]
 ; --------------------------------------
 ;
updateFld n id,field,table,fschema,sid s id=^dcParams(Pid,"Id"),table=^dcParams(Pid,"Table"),field=^dcParams(Pid,"Field")
 i (id="")!(field="")!(table="") d err^dcConn(50011) quit
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 d runFilter("Update") quit:Errors  ; if any violations in filter then do not proceed
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
 i $$isDeleted^dcDb(table,id) l -^dcRecord(table,id) d err^dcConn(50009) quit			
 ;
 i $d(^dcSchema($p(table,"#"),"Fields",field))=0 l -^dcRecord(table,id) d err^dcConn(50010) quit
 ;
 m fschema=^dcSchema($p(table,"#"),"Fields",field)
 ;
 i 'fschema("List")&'fschema("Dynamic") d  i 1
 . i Audit=1 k ^dcRecord(table,id,field,Stamp)
 . m ^dcRecord(table,id,field,Stamp)=^dcParams(Pid,"Content") 
 e  d
 . s sid=$o(^dcParams(Pid,"Content","")) q:sid=""  
 . i Audit=1 k ^dcRecord(table,id,field,sid,Stamp)
 . m ^dcRecord(table,id,field,sid,Stamp)=^dcParams(Pid,"Content",sid)
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
retireRec n id,table s id=Params("Id"),table=Params("Table")
 i (table="") d err^dcConn(50003) quit
 i (id="") d err^dcConn(50004) quit
 ;
 d runFilter("Retire") quit:Errors  ; if any violations in filter then do not proceed
 ;
 d set1^dcDb(table,id,"Retired",1)  
 ;
 quit
 ;
 ;
reviveRec n id,table s id=Params("Id"),table=Params("Table")
 i (table="") d err^dcConn(50003) quit
 i (id="") d err^dcConn(50004) quit
 ;
 d runFilter("Retire") quit:Errors  ; if any violations in filter then do not proceed
 ;
 d set1^dcDb(table,id,"Retired",0)  
 ;
 quit
 ;
 ;
runFilter(mode) n filter,table,id s filter=Params("Filter"),table=Params("Table"),id=Params("Id")
 i table="" s filter=^dcParams(Pid,"Filter"),table=^dcParams(Pid,"Table"),id=^dcParams(Pid,"Id")
 i table="" d err^dcConn(50005) quit	
 i (filter'="")&(^dcProg("recfilter",filter)'="") x "d "_^dcProg("recfilter",filter)
 quit
 ;
 ; incoming
 ;
 ; Original contains the original text that was indexed but with out any markup, it is 
 ; clean.  Also words longer than 125 chars are removed.
 ;
 ; Analyzed contains a pipe separated list of words - these are the stems (trimed characters, lower case, no stop words).
 ; A word for a given table/field/id/sid only appears once and so the score given is a composite for all the occurances
 ; of that word in this context.
 ;
 ; ^dcParams(Pid,"Table") = [table]
 ; ^dcParams(Pid,"Id") = [id]
 ; ^dcParams(Pid,"Fields",field,sid,"Original",0) = [value break on word boundries]
 ; ^dcParams(Pid,"Fields",field,sid,"Original",1) = [value break on word boundries]
 ;
 ; ^dcParams(Pid,"Fields",field,sid,"Analyzed",0) = |word:score:pos,pos,...|word:score:pos|
 ;		- use w $l("a",",") to get number of hits
 ;		- break on word boundaries
 ;		- pos is 0 based
 ;
 ; ^dcTextRecord(table#domain,id,field,sid,"Original",0)= NNNN
 ; ^dcTextRecord(table#domain,id,field,sid,"Analyzed",0)= NNNN
 ;
 ; ^dcTextIndex(table#domain,field,word,score,id,sid)=pos,pos,...
 ;
 ;
updateTxt n id,field,table,sid,cid,apos,entry,word,line,lp,ch,score
 s id=^dcParams(Pid,"Id"),table=^dcParams(Pid,"Table")
 i (id="")!(table="") d err^dcConn(50011) quit
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 d runFilter("Update") quit:Errors  ; if any violations in filter then do not proceed
 ;
 ; we'll need storage for old stems from previous indexing.  allocate an id from the 
 ; cache global
 ;
 s cid=$$getCacheId^dcDbSelect()
 l +^dcTextRecord(table,id)
 ;
 f  s field=$o(^dcParams(Pid,"Fields",field))  q:field=""  d
 . f  s sid=$o(^dcParams(Pid,"Fields",field,sid))  q:sid=""  d  
 . . ;
 . . ; ^dcCache(cid,word)=score - first fill from existing document, then remove those in new doc - remaining need to be removed
 . . ;
 . . k entry
 . . ;
 . . f  s apos=$o(^dcTextRecord(table,id,field,sid,"Analyzed",apos))  q:apos=""  d  
 . . . s line=^dcTextRecord(table,id,field,sid,"Analyzed",apos)
 . . . f lp=1:1:$l(line) d
 . . . . s ch=$e(line,lp)
 . . . . i ch'="|" s entry=entry_ch q
 . . . . i $l(entry)=0 q
 . . . . s word=$p(entry,":",1)
 . . . . s score=$p(entry,":",2)
 . . . . s ^dcCache(cid,word)=score		; for this table, id, field, sid collect the words
 . . . . k entry
 . . ;
 . . ; do indexing and prune cache
 . . ;
 . . k entry
 . . ;
 . . f  s apos=$o(^dcParams(Pid,"Fields",field,sid,"Analyzed",apos))  q:apos=""  d  
 . . . s line=^dcParams(Pid,"Fields",field,sid,"Analyzed",apos)
 . . . f lp=1:1:$l(line) d
 . . . . s ch=$e(line,lp)
 . . . . i ch'="|" s entry=entry_ch q
 . . . . i $l(entry)=0 q
 . . . . s word=$p(entry,":",1)
 . . . . s score=$p(entry,":",2)
 . . . . s entry=$p(entry,":",3)
 . . . . ;
 . . . . k:^dcCache(cid,word)=score ^dcCache(cid,word)    ; don't need to clean this word	
 . . . . ;
 . . . . s ^dcTextIndex(table,field,word,score,id,sid)=entry
 . . . . k entry
 . . ;
 . . ; remove hanging words
 . . ;
 . . k word  f  s word=$o(^dcCache(cid,word))  q:word=""  d
 . . . k ^dcTextIndex(table,field,word,^dcCache(cid,word),id,sid)		
 . . ;
 . . d clearCache^dcDbSelect(cid)
 . . ;
 . . k ^dcTextRecord(table,id,field,sid)
 . . m ^dcTextRecord(table,id,field,sid)=^dcParams(Pid,"Fields",field,sid)
 ;
 l -^dcTextRecord(table,id)
 d killCache^dcDbSelect(cid)
 ;
 quit 
 ;
 ;
 ; Params("Sources",[table name],"Title")=[field name]
 ; Params("Sources",[table name],"Body")=[field name]
 ; Params("Sources",[table name],"Extras",[field name])=1  
 ;
 ; Params("AllowedSids",[table name],[field name],[sid])=1					- if field name not present then assume all
 ;
 ; Params("RequiredWords",[word],"Term")=1
 ;                              ,"Exact")=[exact word for final match in Original]
 ;
 ; Params("AllowedWords",[word],"Term")=1
 ;                              ,"Exact")=[exact word for final match in Original]
 ;
 ; Params("ProhibitedWords",[word],"Term")=1
 ;                              ,"Exact")=[exact word for final match in Original]
 ;
 ; returns top results first, returns only 1000 
 ;   and no more than 100 or so per field...that is all one should ever need
 ;
 ; RETURN = [
 ;		{
 ;			Table: N,
 ;			Id: N,
 ;			Score: N,
 ;			TitlePositions: [ N, n ],		// relative to Title, where 0 is first character
 ;			Title: SSSS
 ;			BodyPositions: [ N, n ],		// relative to Body, where 0 is first character
 ;			Body: SSSS
 ;		}
 ; ]
 ;
srchTxt n params2,table,field,ret,match,score,id,ttitle,tbody,tab,sid,word,plist,i
 ;
 ; srchTxt2 is the heart of the searching, this routine just formats those results
 ; so we need to prep some parameters for srchTxt2
 ;
 m params2("RequiredWords")=Params("RequiredWords")
 m params2("AllowedWords")=Params("AllowedWords")
 m params2("ProhibitedWords")=Params("ProhibitedWords")
 m params2("AllowedSids")=Params("AllowedSids")
 ;
 ; convert sources to the srchTxt2 structure
 ;
 f  s table=$o(Params("Sources",table))  q:table=""  d
 . s ttitle=Params("Sources",table,"Title")
 . s:ttitle'="" params2("Sources",table,ttitle)=1
 . ;
 . s tbody=Params("Sources",table,"Body")
 . s:tbody'="" params2("Sources",table,tbody)=1
 . ;
 . k field  f  s field=$o(Params("Sources",table,"Extras",field))  q:field=""  d
 . . s params2("Sources",table,field)=1
 ;
 ; collect search results
 ;
 d srchTxt2(.params2,.ret)
 ;
 ; return the results
 ;
 w StartList
 ;
 f  s score=$o(ret(score),-1)  q:score=""  d
 . f  s table=$o(ret(score,table))  q:table=""  d
 . . s tab=$p(table,"#",1)
 . . f  s id=$o(ret(score,table,id))  q:id=""  d
 . . . w StartRec
 . . . w Field_"Table"_ScalarStr_tab
 . . . w Field_"Id"_ScalarStr_id
 . . . w Field_"Score"_ScalarInt_score
 . . . ;
 . . . s ttitle=Params("Sources",tab,"Title")
 . . . s tbody=Params("Sources",tab,"Body")
 . . . ;
 . . . w Field_"Title"_ScalarStr
 . . . s sid=$o(^dcTextRecord(table,id,ttitle,""))
 . . . ;
 . . . i sid'="" d
 . . . . w ^dcTextRecord(table,id,ttitle,sid,"Original",0)		; titles are no more than one line
 . . . . ;
 . . . . i $d(ret(score,table,id,ttitle,sid))>0  d
 . . . . . w Field_"TitlePositions"_StartList
 . . . . . ;
 . . . . . f  s word=$o(ret(score,table,id,ttitle,sid,word))  q:word=""  d
 . . . . . . s plist=ret(score,table,id,ttitle,sid,word)
 . . . . . . f i=1:1:$l(plist,",") w ScalarInt_$p(plist,",",i)
 . . . . . ;
 . . . . . w EndList
 . . . . ;
 . . . s tbody=Params("Sources",tab,"Body")
 . . . ;
 . . . w Field_"Body"_ScalarStr
 . . . k sid  f  s sid=$o(^dcTextRecord(table,id,tbody,sid))  q:sid=""  d
 . . . . ;
 . . . . ;i $d(ret(score,table,id,tbody,sid))>0  d  q
 . . . . ;. ; TODO find the positions and cut out parts
 . . . . ;
 . . . . ; if we get here then we are just writing the top 30 words
 . . . . s sentence=^dcTextRecord(table,id,tbody,sid,"Original",0)
 . . . . k wcnt  f i=1:1:$l(sentence) q:wcnt=30  i $e(sentence,i)=" " s wcnt=wcnt+1
 . . . . w $e(sentence,1,i-1)
 . . . ;
 . . . w EndRec
 ;
 w EndList
 ;
 quit
 ;
 ;
 ; Params("Sources",[table name],[field name])=1  
 ;
 ; Params("AllowedSids",[table name],[field name],[sid])=1					- if field name not present then assume all
 ;				TODO AllowedSids not yet coded!!
 ; Params("RequiredWords",[word],"Term")=1
 ;                              ,"Exact")=[exact word for final match in Original]
 ;
 ; Params("AllowedWords",[word],"Term")=1
 ;                              ,"Exact")=[exact word for final match in Original]
 ;
 ; Params("ProhibitedWords",[word],"Term")=1
 ;                              ,"Exact")=[exact word for final match in Original]
 ;
 ; ret
 ;		ret(score,table,id,field,sid,word)=[comma list of positions]
 ;
srchTxt2(params,ret) n score,table,field,id,sid,pos,word,sources,find,fnd,sscore,tabled
 n exact,lnum,matches,fmatch,ismatch,word2,entry,term,nxtscore,collect
 ;
 ; create one list of all words we are searching for
 ;
 f  s word=$o(params("RequiredWords",word))  q:word=""  s find(word)=1,lnum=lnum+1
 f  s word=$o(params("AllowedWords",word))  q:word=""  s find(word)=1,lnum=lnum+1
 ;
 s lnum=$s(lnum>5:20,lnum>3:50,1:100)    ; limit how many partial matches we look at if we have many words
 ;
 ; prime the sources array - we want the top scoring word for each table and field
 ; we'll then use this array to figure the top score of all.  as we loop the sources
 ; we'll keep adding more top matches so we find the next top scoring
 ;
 f  s table=$o(params("Sources",table))  q:table=""  d
 . i (table'["#")&(Domain'="") s tabled=table_"#"_Domain     ; support table instances
 . e  s tabled=table
 . ;
 . f  s field=$o(params("Sources",table,field))  q:field=""  d
 . . f  s word=$o(find(word))  q:word=""  d
 . . . s score=$o(^dcTextIndex(tabled,field,word,""),-1)  
 . . . i score'="" s sources(score,word,tabled,field)=1  						; sources will get filled out further down
 . . . ;
 . . . i (params("RequiredWords",word,"Exact")'="")!(params("AllowedWords",word,"Exact")'="") q
 . . . k matches 
 . . . s word2=word
 . . . f  s word2=$o(^dcTextIndex(tabled,field,word2))  q:word2=""  d  q:matches>(lnum-1) 
 . . . . i $f(word2,word)'=($l(word)+1) s matches=lnum q	; if not starting with the original word then stop looking
 . . . . s score=$o(^dcTextIndex(tabled,field,word2,""),-1)  
 . . . . i score'="" s sources(score,word2,tabled,field)=1,matches=matches+1
 ;
 ; find our top scoring fields/words and then use the text index to find possible
 ; record matches.
 ;
 k score,matches
 f  s score=$o(sources(score),-1)  q:score=""  d  q:matches>999
 . f  s word=$o(sources(score,word))  q:word=""  d
 . . f  s table=$o(sources(score,word,table))  q:table=""  d
 . . . k field
 . . . f  s field=$o(sources(score,word,table,field))  q:field=""!(fmatch(table,field)>99)  d
 . . . . k id
 . . . . f  s id=$o(^dcTextIndex(table,field,word,score,id))  q:id=""!(fmatch(table,field)>99)  d
 . . . . . k sid
 . . . . . f  s sid=$o(^dcTextIndex(table,field,word,score,id,sid))  q:sid=""!(fmatch(table,field)>99)  d
 . . . . . . ; check exact matches - if a required or allowed word is to have an exact match
 . . . . . . ;
 . . . . . . k ismatch
 . . . . . . ;
 . . . . . . i params("RequiredWords",word) d  i 1
 . . . . . . . s exact=params("RequiredWords",word,"Exact")  i exact="" s ismatch=1 q
 . . . . . . . k lnum  f  s lnum=$o(^dcTextRecord(table,id,field,sid,"Original",lnum))  q:lnum=""  d  q:ismatch
 . . . . . . . . i ^dcTextRecord(table,id,field,sid,"Original",lnum)[exact s ismatch=1 
 . . . . . . ; 
 . . . . . . e  i params("AllowedWords",word) d  i 1
 . . . . . . . s exact=params("AllowedWords",word,"Exact")  i exact="" s ismatch=1 q
 . . . . . . . k lnum  f  s lnum=$o(^dcTextRecord(table,id,field,sid,"Original",lnum))  q:lnum=""  d  q:ismatch
 . . . . . . . . i ^dcTextRecord(table,id,field,sid,"Original",lnum)[exact s ismatch=1 
 . . . . . . ;
 . . . . . . e  s ismatch=1
 . . . . . . ;
 . . . . . . q:'ismatch
 . . . . . . ;
 . . . . . . ; check prohibited - see if a prohibited word is in a match
 . . . . . . ; 
 . . . . . . k word2  f  s word2=$o(params("ProhibitedWords",word2))  q:word2=""  d  q:'ismatch
 . . . . . . . s exact=params("ProhibitedWords",word2,"Exact")  
 . . . . . . . s entry=$s(exact="":"Analyzed",1:"Original"),term=$s(exact="":"|"_word2_":",1:word2)
 . . . . . . . k lnum  f  s lnum=$o(^dcTextRecord(table,id,field,sid,entry,lnum))  q:lnum=""  d  q:'ismatch
 . . . . . . . . i ^dcTextRecord(table,id,field,sid,term,lnum)[term s ismatch=0 
 . . . . . . ;
 . . . . . . q:'ismatch
 . . . . . . ;
 . . . . . . s collect(table,id,field,sid,word)=score		; collect contains the values we need for ordering our results
 . . . . . . ;
 . . . . . . s fmatch(table,field)=fmatch(table,field)+1,matches=matches+1
 . . . . ; 
 . . . . s nxtscore=$o(^dcTextIndex(table,field,word,score),-1)  q:nxtscore=""
 . . . . s sources(nxtscore,word,table,field)=1									; filling out sources 
 ;
 ; build return value - we now have enough words, just want to put them in the right order
 ;
 k table,field,id,sid,word,matches
 ;
 f  s table=$o(collect(table))  q:table=""  d  q:matches>249
 . f  s id=$o(collect(table,id))  q:id=""  d  q:matches>249
 . . s ismatch=1
 . . ;
 . . ; ensure all required are present - unlike prohibited, which we can check above,
 . . ; we have to check required across all potential fields for a given record
 . . ; 
 . . k word2  f  s word2=$o(params("RequiredWords",word2))  q:word2=""  d  q:'ismatch
 . . . q:word=word2  ; already checked
 . . . s exact=params("RequiredWords",word2,"Exact"),fnd=0  
 . . . s entry=$s(exact="":"Analyzed",1:"Original"),term=$s(exact="":"|"_word2_":",1:word2)
 . . . ;
 . . . ; check all fields/sids
 . . . ; 
 . . . k field  f  s field=$o(params("Sources",$p(table,"#",1),field))  q:field=""  d  q:fnd
 . . . . k sid  f  s sid=$o(^dcTextRecord(table,id,field,sid))  q:sid=""  d  q:fnd
 . . . . . k lnum  f  s lnum=$o(^dcTextRecord(table,id,field,sid,entry,lnum))  q:lnum=""  d  q:fnd
 . . . . . . i ^dcTextRecord(table,id,field,sid,entry,lnum)[term s fnd=1 
 . . . ;
 . . . s:'fnd ismatch=0
 . . ;
 . . q:'ismatch 
 . . ;
 . . ; compute score for the record
 . . s score=0
 . . ;
 . . k field  f  s field=$o(collect(table,id,field))  q:field=""  d  
 . . . k sid  f  s sid=$o(collect(table,id,field,sid))  q:sid=""  d
 . . . . k word  f  s word=$o(collect(table,id,field,sid,word))  q:word=""  d
 . . . . . s sscore=collect(table,id,field,sid,word)
 . . . . . ;
 . . . . . ; bonus if the word we are scoring matches one of the original words
 . . . . . i params("AllowedWords",word)!params("RequiredWords",word) d
 . . . . . . s lnum=collect(table,id,field,sid,word)
 . . . . . . s sscore=sscore+($l(^dcTextIndex(table,field,word,lnum,id,sid),",")*2)           ; bonus for each word occurance
 . . . . . ;
 . . . . . s score=score+sscore
 . . ;
 . . ; we now have the score for the record
 . . ;
 . . k field  f  s field=$o(collect(table,id,field))  q:field=""  d   
 . . . k sid  f  s sid=$o(collect(table,id,field,sid))  q:sid=""  d
 . . . . k word  f  s word=$o(collect(table,id,field,sid,word))  q:word=""  d
 . . . . . s lnum=collect(table,id,field,sid,word)
 . . . . . s ret(score,table,id,field,sid,word)=^dcTextIndex(table,field,word,lnum,id,sid)
 . . . ;
 . . s matches=matches+1    ; record matches, not word matches
 ;
 quit
 ;
 ;
 ;
 ;
