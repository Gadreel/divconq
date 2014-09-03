dctTest ; 
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
 ; #    See the dctLicense.txt file in the project's top-level directory for details.
 ; #
 ; #  Authors:
 ; #    * Andy White
 ; #
 ; ----------------------------------------------------------------------------
 ;
 ; setup global data for this example
 ;
setup ;
 d cleanup
 ;
 s ^dctData("People",10000,"Name")="Sally"
 s ^dctData("People",10000,"Age")=8
 s ^dctData("People",10000,"Toys",1)="Legos"
 s ^dctData("People",10000,"Toys",2)="Puzzle"
 s ^dctData("People",10000,"Friends",1)=10001
 s ^dctData("People",10000,"Friends",2)=10002
 ;
 s ^dctIndex("Friends",10001,10000)=1
 s ^dctIndex("Friends",10002,10000)=1
 ;
 s ^dctIndex("Toys","Legos",10000)=1
 s ^dctIndex("Toys","Puzzle",10000)=1
 ;
 s ^dctData("People",10001,"Name")="Chad"
 s ^dctData("People",10001,"Age")=8
 s ^dctData("People",10001,"Toys",1)="Kite"
 s ^dctData("People",10001,"Toys",2)="Playdough"
 s ^dctData("People",10001,"Friends",1)=10000
 s ^dctData("People",10001,"Friends",2)=10002
 ;
 s ^dctIndex("Friends",10000,10001)=1
 s ^dctIndex("Friends",10002,10001)=1
 ;
 s ^dctIndex("Toys","Kite",10001)=1
 s ^dctIndex("Toys","Playdough",10001)=1
 ;
 s ^dctData("People",10002,"Name")="Ginger"
 s ^dctData("People",10002,"Age")=9
 s ^dctData("People",10002,"Toys",1)="Bike"
 s ^dctData("People",10002,"Toys",2)="Softball"
 s ^dctData("People",10002,"Friends",1)=10003
 s ^dctData("People",10002,"Friends",2)=10004
 s ^dctData("People",10002,"Friends",2)=10005
 ;
 s ^dctIndex("Friends",10003,10002)=1
 s ^dctIndex("Friends",10004,10002)=1
 s ^dctIndex("Friends",10005,10002)=1
 ;
 s ^dctIndex("Toys","Bike",10002)=1
 s ^dctIndex("Toys","Softball",10002)=1
 ;
 s ^dctData("People",10003,"Name")="Kyle"
 s ^dctData("People",10003,"Age")=10
 s ^dctData("People",10003,"Toys",1)="Softball Bat"
 ;
 s ^dctIndex("Toys","Softball Bat",10003)=1
 ;
 s ^dctData("People",10004,"Name")="Betty"
 s ^dctData("People",10004,"Age")=9
 s ^dctData("People",10004,"Toys",1)="Jump Rope"
 s ^dctData("People",10004,"Toys",2)="Softball Glove"
 s ^dctData("People",10004,"Friends",1)=10002
 s ^dctData("People",10004,"Friends",2)=10005
 ;
 s ^dctIndex("Friends",10002,10004)=1
 s ^dctIndex("Friends",10005,10004)=1
 ;
 s ^dctIndex("Toys","Jump Rope",10004)=1
 s ^dctIndex("Toys","Softball Glove",10004)=1
 ;
 s ^dctData("People",10005,"Name")="Mike"
 s ^dctData("People",10005,"Age")=9
 s ^dctData("People",10005,"Toys",1)="Frisbee"
 s ^dctData("People",10005,"Friends",1)=10003
 ;
 s ^dctIndex("Friends",10003,10005)=1
 ;
 s ^dctIndex("Toys","Frisbee",10005)=1
 ;
 quit
 ;
 ; remove all global data stored for this example
 ;
cleanup k ^dctData,^dctIndex
 quit
 ;
 ; ========== List People ==========
 ; Retrive a list of person names.  Optionally search for and return 
 ; only people within an age range (default to between 0 and 200 inclusive).
 ;
 ; d local^dcConn("QUERY dctListPeople")
 ; 
 ; Parameters
 ; {
 ;    "MinAge": |optional, int, minimal age to allow in the results|,
 ;    "MaxAge": |optional, int, maximum age to allow in the results|
 ; }
 ;
 ; Return
 ; [
 ;    |string, person name|
 ; ]
 ;
listPeople n id,minage,maxage 
 s minage=Params("MinAge"),maxage=Params("MaxAge")
 s:minage="" minage=0
 s:maxage="" maxage=200
 ;
 w StartList		; start list of people
 ;
 f  s id=$o(^dctData("People",id)) q:id=""  d
 . i (^dctData("People",id,"Age")<minage)!(^dctData("People",id,"Age")>maxage) q
 . ;
 . w ScalarStr_^dctData("People",id,"Name")
 ;
 w EndList		; end list of people
 ;
 quit
 ;
 ; ========== List Toys ==========
 ; Retrive a list of toy names.  Optionally search for and return 
 ; only toys containing a key word.
 ;
 ; d local^dcConn("QUERY dctListToys")
 ; 
 ; Parameters
 ; {
 ;    "Search": |optional, string, search key word|
 ; }
 ;
 ; Return
 ; [
 ;    |string, toy name|
 ; ]
 ;
listToys n toy,search s search=Params("Search")
 ;
 w StartList		; start list of toys
 ;
 f  s toy=$o(^dctIndex("Toys",toy)) q:toy=""  d
 . i (search'="")&(toy'[search) q
 . ;
 . w ScalarStr_toy
 ;
 w EndList		; end list of toys
 ;
 quit
 ;
 ; ========== Table People ==========
 ; Retrive data on all people using a tabular data result.
 ;
 ; Tabular data holds columns:
 ;    Name, Age, Toy Count, Friends Count, Friended By Count
 ;
 ; d local^dcConn("QUERY dctTablePeople")
 ; 
 ; Parameters
 ; None
 ;
 ; Return
 ; [
 ;     [
 ;        |int, person id|,
 ;        |string, person name|,
 ;        |int, person age|,
 ;	     |int, toy count|
 ;	     |int, friend count|
 ;	     |int, friendedby count|
 ;     ]
 ; ]
 ;
tablePeople n id,tnum,fnum,temp,fld
 w StartList		; start table of (list holds rows of) people
 ;
 f  s id=$o(^dctData("People",id)) q:id=""  d
 . w StartList			; start column in this row
 . ;
 . ; write out columns for Id, Name and Age
 . w ScalarInt_id
 . w ScalarStr_^dctData("People",id,"Name")
 . w ScalarInt_^dctData("People",id,"Age")
 . ;
 . ; write out columns for Toys and Friends (calculated)
 . f fld="Toys","Friends" d
 . . s tnum=0
 . . f  s temp=$o(^dctData("People",id,fld,temp)) q:temp=""  s tnum=tnum+1
 . . ;
 . . w ScalarInt_tnum
 . ;
 . ; write out column for Friended By (calculated)
 . s tnum=0
 . f  s temp=$o(^dctIndex("Friends",id,temp)) q:temp=""  s tnum=tnum+1
 . ;
 . w ScalarInt_tnum
 . ;
 . w EndList		; end row
 ;
 w EndList		; end table of people
 ;
 quit
 ;
 ; ========== Get People ==========
 ; Retrive data on all people using a complex data form.
 ;
 ; d local^dcConn("QUERY dctGetPeople")
 ; 
 ; Parameters
 ; None
 ;
 ; Return
 ; [
 ;     {
 ;        "Id": |int, person id|,
 ;        "Name": |string, person name|,
 ;        "Age": |int, person age|,
 ;        "Toys": [
 ;	        |string, toy name|
 ;        ],
 ;        "Friends": [
 ;            {
 ;                "Name": |string, person name|,
 ;                "Age": |int, person age|
 ;            }
 ;        ],
 ;        "FriendedBy": [
 ;            {
 ;                "Name": |string, person name|,
 ;                "Age": |int, person age|
 ;            }
 ;        ]
 ;     }
 ; ]
 ;
getPeople n id,tnum,fnum,fid 
 w StartList		; start list of people
 ;
 f  s id=$o(^dctData("People",id)) q:id=""  d
 . w StartRec		; start person
 . ; 
 . w Field_"Id"_ScalarInt_id
 . w Field_"Name"_ScalarStr_^dctData("People",id,"Name")
 . w Field_"Age"_ScalarInt_^dctData("People",id,"Age")
 . ;
 . w Field_"Toys"_StartList		; toy list
 . f  s tnum=$o(^dctData("People",id,"Toys",tnum)) q:tnum=""  d
 . . w ScalarStr_^dctData("People",id,"Toys",tnum)
 . w EndList					; end toy list
 . ;
 . w Field_"Friends"_StartList	; friend list
 . f  s fnum=$o(^dctData("People",id,"Friends",fnum)) q:fnum=""  d
 . . s fid=^dctData("People",id,"Friends",fnum)
 . . ;
 . . w StartRec	; friend info
 . . w Field_"Name"_ScalarStr_^dctData("People",fid,"Name")
 . . w Field_"Age"_ScalarInt_^dctData("People",fid,"Age")
 . . w EndRec	; friend info
 . w EndList					; end friend list
 . ;
 . w Field_"FriendedBy"_StartList	; friended by list
 . f  s fnum=$o(^dctIndex("Friends",id,fnum)) q:fnum=""  d
 . . w StartRec	; friend info
 . . w Field_"Name"_ScalarStr_^dctData("People",fnum,"Name")
 . . w Field_"Age"_ScalarInt_^dctData("People",fnum,"Age")
 . . w EndRec	; friend info
 . w EndList					; end friended by list
 . ;
 . w EndRec		; end person
 ;
 w EndList		; end list of people
 ;
 quit
 ;
 ; ========== Add Person ==========
 ; Add a new person record to "People".  
 ;
 ; d local^dcConn("QUERY dctAddPerson")
 ; 
 ; Parameters
 ; {
 ;    "Name": |required, string, person name|,
 ;    "Age": |required, int, person age|,
 ;    "Toys": [
 ;		|optional, string, toy name|
 ;    ],
 ;    "Friends": [
 ;		|optional, int, friend id|
 ;    ]
 ; }
 ;
 ; Return
 ; {
 ;    "Id": |int, id allocated for this record|
 ; }
 ;
addPerson n fld,id,name,age s name=Params("Name"),age=Params("Age")
 i (name="")!(age="") d err^dcConn(90010) quit
 ;
 ; get the last id ( $o with -1 ) and then add 1
 s id=$o(^dctData("People",""),-1),id=id+1
 ;
 s ^dctData("People",id,"Name")=name
 s ^dctData("People",id,"Age")=age
 m ^dctData("People",id,"Toys")=Params("Toys")
 m ^dctData("People",id,"Friends")=Params("Friends")
 ;
 f fld="Toys","Friends" d
 . f  s temp=$o(Params(fld,temp)) q:temp=""  s ^dctIndex(fld,Params(fld,temp),id)=1
 ;
 w StartRec_Field_"Id"_ScalarInt_id_EndRec
 ;
 quit
 ;
 ;
tryAddPerson n Params
 s Params("Name")="Seth"
 s Params("Age")=8
 s Params("Toys",1)="Helicopter"
 s Params("Toys",2)="Basketball"
 s Params("Friends",1)=10002
 s Params("Friends",2)=10004
 ;
 d local^dcConn("UPDATE dctAddPerson")
 ;
 quit
 ;
 ;
