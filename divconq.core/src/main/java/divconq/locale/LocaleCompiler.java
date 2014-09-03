/* ************************************************************************
#
#  DivConq
#
#  http://divconq.com/
#
#  Copyright:
#    Copyright 2014 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package divconq.locale;

/*
 * 
 * get at http://unicode.org/repos/cldr/trunk/common/main/ 

<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE ldml SYSTEM "../../common/dtd/ldml.dtd">
<ldml>
	<identity>
		<version number="$Revision: 5806 $"/>
		<generation date="$Date: 2011-05-02 13:42:02 -0500 (Mon, 02 May 2011) $"/>
		<language type="en"/>
	</identity>
	<layout>						
		<orientation characters="right-to-left"/>										_cldr_orientation		RTL | LTR [default]
	</layout>
	<dates>
		<calendars>
			<calendar type="gregorian">
				<months>
					<monthContext type="format">
						<monthWidth type="abbreviated">
							<month type="1">Jan</month>									_cldr_cal_greg_mon_NN_abbr
							<month type="2">Feb</month>
							<month type="3">Mar</month>
							<month type="4">Apr</month>
							<month type="5">May</month>
							<month type="6">Jun</month>
							<month type="7">Jul</month>
							<month type="8">Aug</month>
							<month type="9">Sep</month>
							<month type="10">Oct</month>
							<month type="11">Nov</month>
							<month type="12">Dec</month>
						</monthWidth>
						<monthWidth type="wide">
							<month type="1">January</month>								_cldr_cal_greg_mon_NN_wide
							<month type="2">February</month>
							<month type="3">March</month>
							<month type="4">April</month>
							<month type="5">May</month>
							<month type="6">June</month>
							<month type="7">July</month>
							<month type="8">August</month>
							<month type="9">September</month>
							<month type="10">October</month>
							<month type="11">November</month>
							<month type="12">December</month>
						</monthWidth>
					</monthContext>
					<monthContext type="stand-alone">
						<monthWidth type="narrow">
							<month type="1">J</month>									_cldr_cal_greg_mon_NN_narr
							<month type="2">F</month>
							<month type="3">M</month>
							<month type="4">A</month>
							<month type="5">M</month>
							<month type="6">J</month>
							<month type="7">J</month>
							<month type="8">A</month>
							<month type="9">S</month>
							<month type="10">O</month>
							<month type="11">N</month>
							<month type="12">D</month>
						</monthWidth>
					</monthContext>
				</months>
				<days>
					<dayContext type="format">
						<dayWidth type="abbreviated">
							<day type="sun">Sun</day>									_cldr_cal_greg_day_NNN_abbr
							<day type="mon">Mon</day>
							<day type="tue">Tue</day>
							<day type="wed">Wed</day>
							<day type="thu">Thu</day>
							<day type="fri">Fri</day>
							<day type="sat">Sat</day>
						</dayWidth>
						<dayWidth type="wide">
							<day type="sun">Sunday</day>								_cldr_cal_greg_day_NNN_wide
							<day type="mon">Monday</day>
							<day type="tue">Tuesday</day>
							<day type="wed">Wednesday</day>
							<day type="thu">Thursday</day>
							<day type="fri">Friday</day>
							<day type="sat">Saturday</day>
						</dayWidth>
					</dayContext>
					<dayContext type="stand-alone">
						<dayWidth type="narrow">
							<day type="sun">S</day>										_cldr_cal_greg_day_NNN_narr
							<day type="mon">M</day>
							<day type="tue">T</day>
							<day type="wed">W</day>
							<day type="thu">T</day>
							<day type="fri">F</day>
							<day type="sat">S</day>
						</dayWidth>
					</dayContext>
				</days>
				<dayPeriods>
					<dayPeriodContext type="format">
						<dayPeriodWidth type="wide">
							<dayPeriod type="am">AM</dayPeriod>							_cldr_cal_greg_per_NN
							<dayPeriod type="pm">PM</dayPeriod>
						</dayPeriodWidth>
					</dayPeriodContext>
				</dayPeriods>
				<dateFormats>
					<dateFormatLength type="full">
						<dateFormat>
							<pattern>EEEE, MMMM d, y</pattern>							_cldr_cal_greg_dfmt_full
						</dateFormat>
					</dateFormatLength>
					<dateFormatLength type="long">
						<dateFormat>
							<pattern>MMMM d, y</pattern>								_cldr_cal_greg_dfmt_long
						</dateFormat>
					</dateFormatLength>
					<dateFormatLength type="medium">									_cldr_cal_greg_dfmt_medium
						<dateFormat>
							<pattern>MMM d, y</pattern>
						</dateFormat>
					</dateFormatLength>
					<dateFormatLength type="short">										_cldr_cal_greg_dfmt_short
						<dateFormat>
							<pattern>M/d/yy</pattern>
						</dateFormat>
					</dateFormatLength>
				</dateFormats>
				<timeFormats>
					<timeFormatLength type="full">
						<timeFormat>
							<pattern>h:mm:ss a zzzz</pattern>							_cldr_cal_greg_tfmt_full
						</timeFormat>
					</timeFormatLength>
					<timeFormatLength type="long">
						<timeFormat>
							<pattern>h:mm:ss a z</pattern>								_cldr_cal_greg_tfmt_long
						</timeFormat>
					</timeFormatLength>
					<timeFormatLength type="medium">
						<timeFormat>
							<pattern>h:mm:ss a</pattern>								_cldr_cal_greg_tfmt_med
						</timeFormat>
					</timeFormatLength>
					<timeFormatLength type="short">
						<timeFormat>
							<pattern>h:mm a</pattern>									_cldr_cal_greg_tfmt_short
						</timeFormat>
					</timeFormatLength>
				</timeFormats>
			</calendar>
		</calendars>
	</dates>
	<numbers>
		<symbols numberSystem="latn">
			<decimal>.</decimal>													_cldr_num_sym_decimal
			<group>,</group>														_cldr_num_sym_group
			<list>;</list>															_cldr_num_sym_list
			<percentSign>%</percentSign>											_cldr_num_sym_percent
			<plusSign>+</plusSign>													_cldr_num_sym_plus
			<minusSign>-</minusSign>												_cldr_num_sym_minus
			<exponential>E</exponential>											_cldr_num_sym_exponential
			<infinity>∞</infinity>													_cldr_num_sym_infinity
			<nan>NaN</nan>															_cldr_num_sym_nan
		</symbols>
		<decimalFormats>
			<decimalFormatLength>
				<decimalFormat>
					<pattern>#,##0.###</pattern>									_cldr_num_fmt_decimal
				</decimalFormat>
			</decimalFormatLength>
		</decimalFormats>
		<scientificFormats>
			<scientificFormatLength>
				<scientificFormat>
					<pattern>#E0</pattern>											_cldr_num_fmt_scientific
				</scientificFormat>
			</scientificFormatLength>
		</scientificFormats>
		<percentFormats>
			<percentFormatLength>
				<percentFormat>
					<pattern>#,##0%</pattern>										_cldr_num_fmt_percent
				</percentFormat>
			</percentFormatLength>
		</percentFormats>
		<currencyFormats>
			<currencyFormatLength>
				<currencyFormat>
					<pattern>¤#,##0.00;(¤#,##0.00)</pattern>						_cldr_num_fmt_currency
				</currencyFormat>
			</currencyFormatLength>
		</currencyFormats>
	</numbers>
</ldml>

 */
public class LocaleCompiler {

}
