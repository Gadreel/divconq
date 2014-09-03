dcStrUtil ; 
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
lpad(str,with,num) ;
 f  q:$l(str)'<num  d 
 . s str=with_str
 quit str
 ;
 ;
lalign(str,with,num) ;
 f  q:$l(str)'<num  d 
 . s str=with_str
 quit $e(str,$l(str)-num+1,$l(str))
 ;
 ;
toLower(pString)
 quit $tr(pString,"ABCDEFGHIJKLMNOPQRSTUVWXYZ","abcdefghijklmnopqrstuvwxyz")
 ;
toUpper(pString)
 quit $tr(pString,"abcdefghijklmnopqrstuvwxyz","ABCDEFGHIJKLMNOPQRSTUVWXYZ")
 ;
isNumeric(val) n i,err s err=0
 f i=1:1:$l(val) d  q:err 
 . s c=$e(val,i)
 . i c="." q
 . i c<"0" q
 . i c>"9" q
 . s err=1
 quit 'err
 ;
 ;
 ; TODO recode or review
replaceAll(InText,FromStr,ToStr) ; Replace all occurrences of a substring
 ;
 n p
 ;
 s p=InText
 i ToStr[FromStr d  QUIT p
 . n i,stop,tempText,tempTo
 . s stop=0
 . f i=0:1:255 d  q:stop
 . . q:InText[$c(i)
 . . q:FromStr[$c(i)
 . . q:ToStr[$c(i)
 . . s stop=1
 . s tempTo=$c(i)
 . s tempText=$$replaceAll(InText,FromStr,tempTo)
 . s p=$$replaceAll(tempText,tempTo,ToStr)
 f  q:p'[FromStr  S p=$$replace(p,FromStr,ToStr)
 quit p
 ;
 ; TODO recode or review
replace(InText,FromStr,ToStr) ; replace old with new in string
 ;
 n np,p1,p2
 ;
 i InText'[FromStr q InText
 s np=$l(InText,FromStr)+1
 s p1=$p(InText,FromStr,1),p2=$p(InText,FromStr,2,np)
 quit p1_ToStr_p2
 ;
 ; build a security code
 ;
buildCode(size) n code,num
 f i=1:1:size d
 . s num=$r(62)
 . s num=$s(num<26:num+65,num<36:num+48-26,1:num+97-36)
 . s code=code_$c(num)
 quit code
 ;
 ;
format(value,pattern) n parts,i,ch,liton,pstr,pcnt quit:(value="")!(pattern="") value
 s pcnt=0
 f i=1:1:$l(pattern) d
 . s ch=$e(pattern,i)
 . i ch="'" s liton=1-liton 
 . i liton!(ch'=":") s pstr=pstr_ch q
 . i ch=":" s parts(pcnt)=pstr,pcnt=pcnt+1,pstr="" q
 ;
 i pstr'="" s parts(pcnt)=pstr
 ;
 ;	Time:dfmt:tfmt:chron:group
 i parts(0)="Time" quit $$fmtDate^dcTimeUtil(value,parts(1),parts(2),parts(3),parts(4))
 ;
 quit value
 ;
 ;
 ;
tr(token,p1,p2,p3,p4,p5,p6) i token="" quit ""
 n lc,val s lc=Locale,val=^dcLocale(lc,token)
 i val="" s lc=$p(Locale,"_",1),val=^dcLocale(lc,token)
 i val="" quit ""
 ;
 i p1'="" s val=$$replaceAll(val,"{$1}",p1)
 i p2'="" s val=$$replaceAll(val,"{$2}",p2)
 i p3'="" s val=$$replaceAll(val,"{$3}",p3)
 i p4'="" s val=$$replaceAll(val,"{$4}",p4)
 i p5'="" s val=$$replaceAll(val,"{$5}",p5)
 i p6'="" s val=$$replaceAll(val,"{$6}",p6)
 ;
 quit val
 ;
 ; plural check on first parameter
 ;
trp(pluraltoken,singular,p1,p2,p3,p4,p5,p6) i p1'=1 quit $$tr(pluraltoken,p1,p2,p3,p4,p5,p6)
 quit $$tr(singular,p1,p2,p3,p4,p5,p6)
 ;
 ; 

