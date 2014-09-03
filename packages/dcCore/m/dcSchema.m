dcSchema ; 
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
 ; Java utility uploads repository to M.  This function processes values from
 ; the repository that need to be shared by M. 
 ;
update k ^dcProg,^dcLocale,^dcSchema
 ;
 d minSchema^dcInstall		; in case something fails below, we still support min features
 ;
 n i,t,name
 f  s i=$o(^dcParams(Pid,"Procs",i))  q:i=""  d
 . s ^dcProg("proc",^dcParams(Pid,"Procs",i,"Name"))=^dcParams(Pid,"Procs",i,"Execute")
 ;
 f  s i=$o(^dcParams(Pid,"RecordFilters",i))  q:i=""  d
 . s ^dcProg("recfilter",^dcParams(Pid,"RecordFilters",i,"Name"))=^dcParams(Pid,"RecordFilters",i,"Execute")
 ;
 f  s i=$o(^dcParams(Pid,"WhereFilters",i))  q:i=""  d
 . s ^dcProg("wherefilter",^dcParams(Pid,"WhereFilters",i,"Name"))=^dcParams(Pid,"WhereFilters",i,"Execute")
 ;
 f  s i=$o(^dcParams(Pid,"RecordComposers",i))  q:i=""  d
 . s ^dcProg("reccomposer",^dcParams(Pid,"RecordComposers",i,"Name"))=^dcParams(Pid,"RecordComposers",i,"Execute")
 ;
 f  s i=$o(^dcParams(Pid,"SelectComposers",i))  q:i=""  d
 . s ^dcProg("selcomposer",^dcParams(Pid,"SelectComposers",i,"Name"))=^dcParams(Pid,"SelectComposers",i,"Execute")
 ;
 f  s i=$o(^dcParams(Pid,"WhereComposers",i))  q:i=""  d
 . s ^dcProg("wherecomposer",^dcParams(Pid,"WhereComposers",i,"Name"))=^dcParams(Pid,"WhereComposers",i,"Execute")
 ;
 f  s i=$o(^dcParams(Pid,"Collectors",i))  q:i=""  d
 . s ^dcProg("collector",^dcParams(Pid,"Collectors",i,"Name"))=^dcParams(Pid,"Collectors",i,"Execute")
 ;
 f  s i=$o(^dcParams(Pid,"Tables",i))  q:i=""  d addTable
 ;
 f  s i=$o(^dcParams(Pid,"Locales",i))  q:i=""  d
 . s name=^dcParams(Pid,"Locales",i,"Name")
 . f  s t=$o(^dcParams(Pid,"Locales",i,"Tokens",t))  q:t=""  d
 . . s ^dcLocale(name,^dcParams(Pid,"Locales",i,"Tokens",t,"Key"))=^dcParams(Pid,"Locales",i,"Tokens",t,"Value")
 ;
 quit
 ;
 ;
addTable n table,fi,field,fk
 s table=^dcParams(Pid,"Tables",i,"Name")
 ;
 ; all tables have the following...
 s ^dcSchema(table,"Fields","Retired","Name")="Retired"
 s ^dcSchema(table,"Fields","Retired","Type")="Boolean"
 ;
 s ^dcSchema(table,"Fields","From","Name")="From"
 s ^dcSchema(table,"Fields","From","Type")="BigDateTime"
 s ^dcSchema(table,"Fields","From","Indexed")=1
 ;
 s ^dcSchema(table,"Fields","To","Name")="To"
 s ^dcSchema(table,"Fields","To","Type")="BigDateTime"
 s ^dcSchema(table,"Fields","To","Indexed")=1
 ;
 s ^dcSchema(table,"Fields","Tags","Name")="Tags"
 s ^dcSchema(table,"Fields","Tags","Type")="String"
 s ^dcSchema(table,"Fields","Tags","TypeId")="dcTinyString"
 s ^dcSchema(table,"Fields","Tags","List")="True"
 ;
 f  s fi=$o(^dcParams(Pid,"Tables",i,"Fields",fi))  q:fi=""  d 
 . s field=^dcParams(Pid,"Tables",i,"Fields",fi,"Name")
 . m ^dcSchema(table,"Fields",field)=^dcParams(Pid,"Tables",i,"Fields",fi)
 . ;
 . s fk=^dcParams(Pid,"Tables",i,"Fields",fi,"ForeignKey")
 . s:fk'="" ^dcSchema(fk,"ForeignKey",table,field)=1
 ;
 ; TODO add support for triggers
 ; s ^dcSchema(table,"Triggers","Insert",name)=routine
 ; s ^dcSchema(table,"Triggers","Retire",name)=routine
 ; s ^dcSchema(table,"Triggers","Delete",name)=routine
 ;
 quit 
 ;
 ;
 ;

