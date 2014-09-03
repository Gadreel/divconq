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
 ; d local^dcConn("QUERY testProc1")
 ;
testProc1 n data,tnum,fnum
 ;
 s data("Name")="Sally"
 s data("Age")=5
 s data("Toys",1)="Legos"
 s data("Toys",2)="Puzzle"
 s data("Friends",1,"Name")="Chad"
 s data("Friends",1,"Age")=5
 s data("Friends",2,"Name")="Ginger"
 s data("Friends",2,"Age")=6
 ;
 w StartRec
 w Field_"Name"_ScalarStr_data("Name")
 w Field_"Age"_ScalarInt_data("Age")
 ;
 w Field_"Toys"_StartList
 f  s tnum=$o(data("Toys",tnum)) q:tnum=""  d
 . w ScalarStr_data("Toys",tnum)
 w EndList
 ;
 w Field_"Friends"_StartList
 f  s fnum=$o(data("Friends",fnum)) q:fnum=""  d
 . w StartRec
 . w Field_"Name"_ScalarStr_data("Friends",fnum,"Name")
 . w Field_"Age"_ScalarInt_data("Friends",fnum,"Age")
 . w EndRec
 w EndList
 ;
 w EndRec
 ;
 quit
 ;
 ;
 ; d local^dcConn("QUERY testProc2")
 ;
 ; pass in a Toy list - Params(1)="Truck", Params(2)="Paints", etc
 ; will present error if a toy in list is not allowed
 ; otherwise will process the list (names to upper)
 ;
testProc2 n tnum,toy
 ;
 ; validate your inputs, if toy list is not there do not continue
 i $d(Params)<10 d errMsg^dcConn("Missing toy list") quit
 ;
 ; continue input validation, check for undesirable toys
 f  s tnum=$o(Params(tnum)) q:tnum=""  d
 . s toy=Params(tnum)
 . s toy=$$toUpper^dcStrUtil(toy)
 . i toy["GUN" d errMsg^dcConn("Gun not allowed in toy list") q
 . i toy["CANDY" d errMsg^dcConn("Candy not allowed in toy list") q
 . i toy["HAMMER" d warnMsg^dcConn("A hammer may not be safe for this age") q
 ;
 ; if any errors occured (Gun or Candy) then quit, don't even return data
 i Errors quit
 ;
 ; if only warnings or no validation messages then proceed
 w StartList
 f  s tnum=$o(Params(tnum)) q:tnum=""  d
 . s toy=Params(tnum)
 . s toy=$$toUpper^dcStrUtil(toy)
 . w ScalarStr_toy
 w EndList
 ;
 quit
 ;
 ; testProc3
 ;
 ; d local^dcConn("QUERY testProc3")
 ;
 ; same as testProc2 only using international translations
 ;
testProc3 n tnum,toy,utoy
 ;
 ; validate your inputs, if toy list is not there do not continue
 i $d(Params)<10 d err^dcConn(90000) quit
 ;
 ; continue input validation, check for undesirable toys
 f  s tnum=$o(Params(tnum)) q:tnum=""  d
 . s toy=Params(tnum)
 . s utoy=$$toUpper^dcStrUtil(toy)
 . i (utoy["GUN")!(utoy["CANDY") d err^dcConn(90001,toy) q
 . i utoy["HAMMER" d warn^dcConn(90002,toy) q
 ;
 ; if any errors occured (Gun or Candy) then quit, don't even return data
 i Errors quit
 ;
 ; if only warnings or no validation messages then proceed
 w StartList
 f  s tnum=$o(Params(tnum)) q:tnum=""  d
 . s toy=Params(tnum)
 . s toy=$$toUpper^dcStrUtil(toy)
 . w ScalarStr_toy
 w EndList
 ;
 quit
 ;
 ;
 ;

