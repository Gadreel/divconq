dcTimeUtil ; 
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
 ; MUMPS process needs to be running with UTC timezone for these functions to work
 ;
 ; NOW as ISO string (UTC)
now(long) n n s n=$h    ; TODO research how to get ms 
 i long q $zd(n,"YEARMMDD")_"T"_$zd(n,"2460SS")_"000Z"
 quit $zd(n,"YEARMMDD")_"T"_$zd(n,"2460SS")_"Z"
 ;
 ;
 ; NOW plus days and seconds (UTC)
add(days,secs,long) n n s n=$h   ; TODO research how to get ms 
 ; TODO - consider addTime function
 s:days'="" $p(n,",",1)=$p(n,",",1)+days
 s:secs'="" $p(n,",",2)=$p(n,",",2)+secs
 ;
 i long q $zd(n,"YEARMMDD")_"T"_$zd(n,"2460SS")_"000Z"
 quit $zd(n,"YEARMMDD")_"T"_$zd(n,"2460SS")_"Z"
 ;
 ;
 ; NOW as internal string (UTC)
whenNow() n n s n=$h
 quit "t5000000"_$zd(n,"YEARMMDD")_$zd(n,"2460SS")
 ;
 ;
 ;
 ; format a big datetime or iso datetime - ISO (proleptic Gregorian) built-in
 ; other chronologies can be plugged in
 ;
 ; date = must be UTC zone in either big date or ISO 8601 format (YYYYMMDDTHHMMSSSSSZ)
 ;
fmtDate(date,dfmt,tfmt,zone,group) n fields i zone="" s zone=TimeZone
 ;
 i (dfmt="none") s dfmt=""
 i (tfmt="none") s tfmt=""
 ;
 i (dfmt="")&(tfmt="") quit date
 ;
 d decode(.fields,date,zone,(tfmt=""))
 ;
 i (dfmt="full")!(dfmt="long")!(dfmt="medium")!(dfmt="short") d
 . i 'fields("dom")&'fields("moy") s dfmt=$$tr^dcStrUtil("_cldr_cal_greg_yfmt_"_dfmt) q
 . i 'fields("dom") s dfmt=$$tr^dcStrUtil("_cldr_cal_greg_myfmt_"_dfmt) q
 . s dfmt=$$tr^dcStrUtil("_cldr_cal_greg_dfmt_"_dfmt) 
 ;
 i (tfmt="full")!(tfmt="long")!(tfmt="medium")!(tfmt="short") d
 . i 'fields("som")&'fields("moh") s tfmt=$$tr^dcStrUtil("_cldr_cal_greg_hfmt_"_tfmt) q
 . i 'fields("som") s tfmt=$$tr^dcStrUtil("_cldr_cal_greg_hmfmt_"_tfmt) q
 . s tfmt=$$tr^dcStrUtil("_cldr_cal_greg_tfmt_"_tfmt) 
 ;
 quit $$fmt(.fields,dfmt,tfmt,zone,group)
 ;
 ;  only "decode" and "fmt" are calendar agnostic - all other functions here down are 
 ;  ISO (proleptic Gregorian) centric functions
 ;
fmt(fields,dfmt,tfmt,zone,group) n ch,i,str,wch,wcnt,x,liton,lit,fmt
 i ($d(fields)<10)!(zone="") quit str
 ;
 i $e(zone)="/" s chron="Iso",zone="Iso"_zone
 e  s chron=$e(zone,1,$f(zone,"/")-2)
 ;
 i chron'="Iso" d  quit
 . ; TODO execute fmt script (indirection)
 ;
 ; TODO support RTL also
 i (dfmt'="") d fmt2(dfmt) 
 i (dfmt'="")&(tfmt'="") s str=str_" "
 i (tfmt'="") d fmt2(tfmt) 
 ;
 quit str
 ;
 ; private
fmt2(fmt) ;
 f i=1:1:$l(fmt)+1 d
 . s ch=$e(fmt,i)
 . ; deal with literals
 . i ch="'" d  q
 . . i 'liton d:wcnt fmtpat s liton=1,wch="",wcnt=0,lit="" q
 . . s liton=0,str=str_lit,lit=""
 . i liton s lit=lit_ch q
 . ; deal with patterns
 . i wch="" s wch=ch,wcnt=1 q
 . i ch'=wch d fmtpat s wch=ch,wcnt=1 q
 . s wcnt=wcnt+1
 quit
 ;
 ; private
fmtpat
 i wch=" " f x=1:1:wcnt s str=str_" "
 i wch="y" d  
 . i wcnt=1 s str=str_$$lalign^dcStrUtil(fields("year"),"0",1) q
 . i wcnt=2 s str=str_$$lalign^dcStrUtil(fields("year"),"0",2) q
 . ; TODO add grouping if "group"
 . s x=fields("year")-50000000000
 . i wcnt=3 s str=str_x q
 . ; TODO add grouping if "group"
 . i wcnt=4 s str=str_$$lpad^dcStrUtil(x,"0",4) q
 . ; TODO add grouping
 . i wcnt=5 s str=str_x q
 i wch="m" d  
 . i wcnt=1 s str=str_fields("moy") q
 . i wcnt=2 s str=str_$$lalign^dcStrUtil(fields("moy"),"0",2) q
 . s x=fields("moy")
 . i wcnt=3 s str=str_$$tr^dcStrUtil("_cldr_cal_greg_mon_"_x_"_narr") q
 . i wcnt=4 s str=str_$$tr^dcStrUtil("_cldr_cal_greg_mon_"_x_"_abbr") q
 . i wcnt=5 s str=str_$$tr^dcStrUtil("_cldr_cal_greg_mon_"_x_"_wide") q
 i wch="d" d  
 . i wcnt=1 s str=str_fields("dom") q
 . i wcnt=2 s str=str_$$lalign^dcStrUtil(fields("dom"),"0",2) q
 . s x=$$dayOfWeek(fields("year"),fields("moy"),fields("dom"))
 . i wcnt=3 s str=str_$$tr^dcStrUtil("_cldr_cal_greg_day_"_x_"_narr") q
 . i wcnt=4 s str=str_$$tr^dcStrUtil("_cldr_cal_greg_day_"_x_"_abbr") q
 . i wcnt=5 s str=str_$$tr^dcStrUtil("_cldr_cal_greg_day_"_x_"_wide") q
 i wch="g" d  
 . s x=$s(fields("year")>50000000000:"ce",1:"bce")
 . i wcnt=1 s str=str_$$tr^dcStrUtil("_cldr_cal_greg_era_"_x) q
 i wch="H" d  
 . s x=fields("hod"),x=$s(x>12:x-12,x>0:x,1:12)
 . i wcnt=1 s str=str_x q
 . i wcnt=2 s str=str_$$lalign^dcStrUtil(x,"0",2) q
 i wch="K" d  
 . s x=fields("hod"),x=$s(x>11:"pm",1:"am")
 . i wcnt=1 s str=str_$$tr^dcStrUtil("_cldr_cal_greg_per_"_x) q
 i wch="F" d  
 . i wcnt=2 s str=str_$$lalign^dcStrUtil(fields("hod"),"0",2) q
 i wch="M" d  
 . i wcnt=1 s str=str_fields("moh") q
 . i wcnt=2 s str=str_$$lalign^dcStrUtil(fields("moh"),"0",2) q
 i wch="S" d  
 . i wcnt=1 s str=str_fields("som") q
 . i wcnt=2 s str=str_$$lalign^dcStrUtil(fields("som"),"0",2) q
 i wch="L" d  
 . i wcnt=3 s str=str_$$lalign^dcStrUtil(fields("ms"),"0",3) q
 i wch="Z" d  
 . i wcnt=1 s str=str_fields("zname") q
 q
 ;
 ; date can be BigDateTime - start with 't'
 ;   or can be iso stamp - YYYYMMDD'T'hhmmsssss'Z'
 ; supress = do not adjust for zone, no time part requested
 ;
decode(fields,date,zone,supress) i (date="")!(zone="") quit
 n chron,year,moy,dom,hod,moh,som,ms,offset,ofields,sl,zname
 ;
 i $e(date)="t" d  i 1   ; big date time
 . s year=$e(date,2,12)
 . s moy=$e(date,13,14)
 . s dom=$e(date,15,16)
 . s hod=$e(date,17,18)
 . s moh=$e(date,19,20)
 . s som=$e(date,21,22)
 . s ms=$e(date,23,25)
 e  d                    ; iso time stamp
 . s sl=$l(date)
 . s year=50000000000+$e(date,1,4)
 . s moy=$e(date,5,6)
 . s dom=$e(date,7,8)
 . s:sl>10 hod=$e(date,10,11)
 . s:sl>12 moh=$e(date,12,13)
 . s:sl>14 som=$e(date,14,15)
 . s:sl>16 ms=$e(date,16,18)
 ;
 i $e(zone)="/" s chron="Iso",zone="Iso"_zone
 e  s chron=$e(zone,1,$f(zone,"/")-2)
 ;
 i chron'="Iso" d  quit
 . ; TODO execute decode script (indirection)
 ;
 ; treat as Iso if got this far
 s fields("year")=year+0
 s:$l(moy) fields("moy")=moy+0
 s:$l(dom) fields("dom")=dom+0
 ;
 i supress quit
 ;
 s:$l(hod) fields("hod")=hod+0
 s:$l(moh) fields("moh")=moh+0
 s:$l(som) fields("som")=som+0
 s:$l(ms) fields("ms")=ms+0
 ;
 s offset=$$calcIsoOffset(zone,year,moy+0,dom+0,hod+0,moh+0,.zname)
 ;
 i offset s ofields("minute")=offset d addTime(.fields,.ofields)
 i zname'="" s fields("zname")=zname
 ;
 quit
 ;
 ;
 ;
addTime(time,fields) n tadd,tcurr,tnew,dadd,dom,moy,year,mdays
 n xhod,xmoh,xsom,xms,fnd,madd
 s tadd=(fields("hour")*3600000)
 s tadd=tadd+(fields("minute")*60000)
 s tadd=tadd+(fields("second")*1000)
 s tadd=tadd+fields("ms")
 ;
 s:time("hod")'="" xhod=1,tcurr=(time("hod")*3600000)
 s:time("moh")'="" xmoh=1,tcurr=tcurr+(time("moh")*60000)
 s:time("som")'="" xsom=1,tcurr=tcurr+(time("som")*1000)
 s:time("ms")'="" xms=1,tcurr=tcurr+time("ms")
 ;
 i 'tcurr&'tadd g addt2    ; nothing to do with time fields
 ;
 i tadd<0 d    ; subtract time
 . s dadd=tadd\86400000
 . s tadd=-(-tadd#86400000)
 . ;
 . i (tcurr+tadd)'<0 s tnew=tcurr+tadd q
 . ;
 . s dadd=dadd-1
 . s tnew=86400000+(tcurr+tadd)
 ;
 i tadd>0 d    ; add time
 . s dadd=tadd\86400000
 . s tadd=tadd#86400000
 . ;
 . i (tcurr+tadd)<86400000 s tnew=tcurr+tadd q
 . ;
 . s dadd=dadd+1
 . s tnew=tadd-(86400000-tcurr)
 ;
 k time("hod"),time("moh"),time("som"),time("ms")
 ;
 s:tnew!xhod time("hod")=tnew\3600000
 s tnew=tnew#3600000
 ;
 s:tnew!xmoh time("moh")=tnew\60000
 s tnew=tnew#60000
 ;
 s:tnew!xsom time("som")=tnew\1000
 s tnew=tnew#1000
 ;
 s:tnew!xms time("ms")=tnew
 ;
addt2 s dadd=dadd+fields("day")
 s dom=time("dom"),moy=time("moy"),year=time("year")
 ;
 i 'dadd g addt3     ; no operations on day
 ;
 i dadd<0 d      ; subtract days
 . i (dom+dadd)>0 s dom=dom+dadd,dadd=0 q
 . ;
 . s dadd=dadd+dom   ; go back to start of last month
 . ;
 . f  d  q:fnd
 . . s moy=moy-1
 . . i moy<1 s year=year-1,moy=12
 . . s mdays=$$monthDays(year,moy)
 . . i (mdays+dadd)>0 s dom=mdays+dadd,dadd=0,fnd=1 q
 . . s dadd=dadd+mdays   ; go back to start of last month
 ;
 i dadd>0 d      ; add days
 . s mdays=$$monthDays(year,moy)
 . i (dom+dadd)'>mdays s dom=dom+dadd,dadd=0 q
 . ;
 . s dadd=dadd-(mdays-dom+1)   ; go to start of next month
 . ;
 . f  d  q:fnd
 . . s moy=moy+1
 . . i moy>12 s year=year+1,moy=1
 . . s mdays=$$monthDays(year,moy)
 . . i dadd<mdays s dom=dadd+1,dadd=0,fnd=1 q
 . . s dadd=dadd-mdays   ; go to start of next month
 ;
 s time("dom")=dom
 s time("moy")=moy
 s time("year")=year
 ;
addt3 s madd=fields("month")
 i 'madd g addt4     ; no operations on month
 ;
 i madd<0 d      ; subtract months
 . s year=year+(madd\12)
 . s moy=moy-(-madd#12)
 . i moy<1 s moy=12+moy,year=year-1 
 ;
 i madd>0 d      ; add months
 . s year=year+(madd\12)
 . s moy=moy+(madd#12)
 . i moy>12 s moy=moy#12,year=year+1 
 ;
 s time("moy")=moy
 ;
addt4 s year=year+fields("year")
 s time("year")=year
 ;
 s mdays=$$monthDays(year,moy)
 i dom>mdays s time("dom")=mdays   ; for non-leap years
 ;
 quit
 ;
 ; return in minutes
calcIsoOffset(zone,year,moy,dom,hod,moh,name) n offset
 i hod="" quit offset   ; do not offset if no time part
 ;
 n rule,rnum,onwhen,offwhen,ontime,offtime
 n onminoy,offminoy,minoy,onoff,offoff
 ;
 s offset=$$timeToMin(^dcChron("Zone",zone,"ZoneTime"))
 s rule=^dcChron("Zone",zone,"DstRule")
 s name=^dcChron("Zone",zone,"Name")
 ;
 i rule'="" d
 . s minoy=$$minuteOfYear(year,moy,dom,hod,moh)
 . ; TODO at present we just accept the latest rule, should check from/to/loop
 . s rnum=$o(^dcChron("Rule",rule,""),-1) q:rnum=""
 . ;
 . s onwhen=^dcChron("Rule",rule,rnum,"OnWhen")
 . s ontime=^dcChron("Rule",rule,rnum,"OnTime")
 . s onoff=$$timeToMin(^dcChron("Rule",rule,rnum,"OnOffset"))
 . ;
 . s offwhen=^dcChron("Rule",rule,rnum,"OffWhen")
 . s offtime=^dcChron("Rule",rule,rnum,"OffTime")
 . s offoff=$$timeToMin(^dcChron("Rule",rule,rnum,"OffOffset"))
 . ;
 . s onminoy=$$minuteForDst(year,onwhen,ontime,offset,offoff,offoff)
 . s offminoy=$$minuteForDst(year,offwhen,offtime,offset,offoff,onoff)
 . ;
 . i (offminoy<onminoy)&((minoy'<onminoy)!(minoy<offminoy)) d  q
 . . s offset=offset+onoff 
 . . s name=^dcChron("Zone",zone,"DstName")
 . i (onminoy<offminoy)&(minoy'<onminoy)&(minoy<offminoy) d  q
 . . s offset=offset+onoff 
 . . s name=^dcChron("Zone",zone,"DstName")
 . ;
 . s offset=offset+offoff
 ;
 quit offset
 ;
 ;
 ; soff = offset of standard
 ; roff = offset of other
minuteForDst(year,when,time,ztmin,soff,roff) ;
 n moy,dom,hod,moh,fdow,dow,ldom,i,fnd,sdom,min
 s moy=$p(when," ",1),dom=$p(when," ",2)
 ; if not a literal day of month, calculate it
 i dom'=(dom+0) d
 . s fdow=$$dayOfWeek(year,moy,1)  ; doy for the first
 . ; for lastDOW rule
 . i $e(dom,1,4)="last" d  q
 . . s dow=$$dayToNum($e(dom,5,$l(dom)))
 . . s ldom=$$monthDays(year,moy)
 . . f i=ldom:-1:1 d  q:fnd
 . . . i $$dayOfWeek(year,moy,i)=dow s fnd=1,dom=i
 . ; for DOW>=N rule
 . s dow=$$dayToNum($e(dom,1,3))
 . s sdom=$$dayToNum($e(dom,6,$l(dom)))
 . s ldom=$$monthDays(year,moy)
 . f i=sdom:1:ldom d  q:fnd
 . . i $$dayOfWeek(year,moy,i)=dow s fnd=1,dom=i
 ; 
 i 'dom quit ""
 ;
 s min=$$timeToMin(time)
 ;
 i time["u" i 1
 e  i time["s" s min=min-ztmin-soff
 e  s min=min-ztmin-roff
 ;
 quit $$minuteOfYear(year,moy,dom)+min
 ;
 ;
timeToMin(time) n hod,moh
 s hod=$p(time,":",1)+0
 s moh=$p(time,":",2)+0
 i hod<0 s moh=-moh
 quit (hod*60)+moh
 ;
 ;
minuteOfYear(year,moy,dom,hod,moh) n min,days
 s days=$$monthToDays(year,moy)+dom-1
 s min=(days*1440)+(hod*60)+moh
 quit min
 ;
 ;
dayOfWeek(year,moy,dom) n t
 s moy=$$monthToNum(moy)
 s t=$s(moy=2:3,moy=3:2,moy=4:5,moy=6:3,moy=7:5,moy=8:1,moy=9:4,moy=10:6,moy=11:2,moy=12:4,1:0)
 i moy<3 s year=year-1
 quit year+(year\4)-(year\100)+(year\400)+t+dom#7
 ;
 ;
monthDays(year,moy) n days,leap
 i (year#400=0)!((year#100'=0)&(year#4=0)) s leap=1
 s moy=$$monthToNum(moy)
 ;
 i moy=1 quit 31
 i moy=2 quit $s(leap:29,1:28) 
 i moy=3 quit 31
 i moy=4 quit 30
 i moy=5 quit 31
 i moy=6 quit 30
 i moy=7 quit 31
 i moy=8 quit 31
 i moy=9 quit 30
 i moy=10 quit 31
 i moy=11 quit 30
 i moy=12 quit 31
 ; 
 quit ""
 ;
 ;
monthToDays(year,moy) n days,leap
 i (year#400=0)!((year#100'=0)&(year#4=0)) s leap=1
 s moy=$$monthToNum(moy)
 ;
 i moy>1 s days=days+31
 i moy>2 s days=days+$s(leap:29,1:28) 
 i moy>3 s days=days+31
 i moy>4 s days=days+30
 i moy>5 s days=days+31
 i moy>6 s days=days+30
 i moy>7 s days=days+31
 i moy>8 s days=days+31
 i moy>9 s days=days+30
 i moy>10 s days=days+31
 i moy>11 s days=days+30
 i moy>12 s days=days+31
 ; 
 quit days
 ;
 ;
monthToNum(moy) i moy=(moy+0) quit moy
 ;
 s moy=$$toLower^dcStrUtil(moy)
 ;
 i moy="jan" quit 1
 i moy="january" quit 1
 i moy="feb" quit 2
 i moy="february" quit 2
 i moy="mar" quit 3
 i moy="march" quit 3
 i moy="apr" quit 4
 i moy="april" quit 4
 i moy="may" quit 5
 i moy="may" quit 5
 i moy="jun" quit 6
 i moy="june" quit 6
 i moy="jul" quit 7
 i moy="july" quit 7
 i moy="aug" quit 8
 i moy="august" quit 8
 i moy="sep" quit 9
 i moy="september" quit 9
 i moy="oct" quit 10
 i moy="october" quit 10
 i moy="nov" quit 11
 i moy="november" quit 11
 i moy="dec" quit 12
 i moy="december" quit 12
 ;
 quit ""
 ;
 ;
numToMonth(num) i (num<1)!(num>12) quit ""
 ;
 i num=1 quit "jan"
 i num=2 quit "feb"
 i num=3 quit "mar"
 i num=4 quit "apr"
 i num=5 quit "may"
 i num=6 quit "jun"
 i num=7 quit "jul"
 i num=8 quit "aug"
 i num=9 quit "sep"
 i num=10 quit "oct" 
 i num=11 quit "nov" 
 i num=12 quit "dec" 
 ;
 quit ""
 ;
 ;
dayToNum(dow) i dow=(dow+0) quit dow
 ;
 s dow=$$toLower^dcStrUtil(dow)
 ;
 i dow="sun" quit 0
 i dow="sunday" quit 0
 i dow="mon" quit 1
 i dow="monday" quit 1
 i dow="tue" quit 2
 i dow="tuesday" quit 2
 i dow="wed" quit 3
 i dow="wednesday" quit 3
 i dow="thu" quit 4
 i dow="thursday" quit 4
 i dow="fri" quit 5
 i dow="friday" quit 5
 i dow="sat" quit 6
 i dow="saturday" quit 6
 ;
 quit ""
 ;
 ;