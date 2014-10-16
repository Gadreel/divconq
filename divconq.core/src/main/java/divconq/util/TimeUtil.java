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
package divconq.util;

import java.text.SimpleDateFormat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Months;
import org.joda.time.Weeks;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import divconq.bus.Message;
import divconq.lang.BigDateTime;
import divconq.lang.CoreLocalTime;

/**
 * DivConq uses the Joda date time library for nearly all date/time processing.
 * DivConq also assumes that date time in string format is typically in ISO format.
 * Joda has a setting to indicate which timezone the Hub is running in, all methods
 * that follow use that timezone setting.
 * 
 * @author Andy
 *
 */
public class TimeUtil {
	static public final DateTimeFormatter stampFmt = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssSSS'Z'");
	static public final DateTimeFormatter sqlStampFmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
	static public final SimpleDateFormat sqlStampReformat = new SimpleDateFormat("yyyyMMdd'T'HHmmssSSS");

	/*
	System.out.println("pt: " + ISODateTimeFormat.localTimeParser().parseDateTime("16:20:45"));
	
    DateTimeZone dtz = DateTimeZone.forID("America/Chicago");

    System.out.println(startDST(dtz, 2011));
    System.out.println(endDST(dtz, 2011));
    */
	
	/**
	 * check if a date is before today, ignore the time just look at the date
	 * 
	 * @param d date to check
	 * @return true if it comes before today
	 */	
	static public boolean isBeforeToday(DateTime d) {
		return d.toLocalDate().isBefore(new LocalDate());
	}
	
	/**
	 * try to supply a time for a date, if it fails it may be because of DST and that time (hour) is skipped on that date.
	 * So try again to supply a time +1 hour to see if it helps.
	 * 
	 * @param d date to set time into
	 * @param t time to set to
	 * @return datetime with the supplied time (maybe +1 hour) or null
	 */
    static public DateTime withTime(DateTime d, LocalTime t) {
    	try {
    		return d.withTime(t.getHourOfDay(), t.getMinuteOfHour(), t.getSecondOfMinute(), t.getMillisOfSecond());
    	}
    	catch (Exception x) {
    		// TODO hour +1 is a hack, should work in USA/Canada - and probably lots of places - but maybe not everywhere
    		if (TimeUtil.checkDST(d) == DaylightTransition.START)    		
        		return d.withTime(t.getHourOfDay() + 1, t.getMinuteOfHour(), t.getSecondOfMinute(), t.getMillisOfSecond());
    	}
    	
    	return null;
    }
	
	/**
	 * try to supply a time for a date, if it fails it may be because of DST and that time (hour) is skipped on that date.
	 * So try again to supply a time +1 hour to see if it helps.
	 * 
	 * @param d date to set time into
	 * @param t time to set to
	 * @return datetime with the supplied time (maybe +1 hour) or null
	 */
	static public DateTime withTime(DateTime dt, CoreLocalTime clt) {
		return dt.withTime(clt.getHour(), clt.getMinute(), clt.getSecond(), clt.getMillSec());
	}

	/**
	 * try to get a date at midnight tomorrow, if no midnight due to DST then it may be 1am
	 * 
	 * @param d date from which to calculate tomorrow
	 * @return datetime of midnight, or closest to midnight, tomorrow
	 */
    static public DateTime nextDayAtMidnight(DateTime d) {
    	return d.withTime(0, 0, 0, 0).plusDays(1);
    }
    
    /*
	System.out.println("1: " + ISODateTimeFormat.localTimeParser().parseDateTime("T15:00:00").toLocalTime());
	System.out.println("2: " + ISODateTimeFormat.localTimeParser().parseDateTime("15:00:00").toLocalTime());
	System.out.println("3: " + ISODateTimeFormat.localTimeParser().parseDateTime("15").toLocalTime());
	
	System.out.println("1: " + ISODateTimeFormat.timeParser().parseDateTime("T15:00:00Z").toLocalTime());
	System.out.println("2: " + ISODateTimeFormat.timeParser().parseDateTime("15:00:00Z").toLocalTime());
	System.out.println("3: " + ISODateTimeFormat.timeParser().parseDateTime("15").toLocalTime());
	*/
	
    /**
     * parse a string assuming ISO format
     * 
     * @param t string with iso formatted date
     * @return datetime if parsed, else null 
     */
    static public DateTime parseDateTime(String t) {
		if (StringUtil.isEmpty(t))
    		return null;
    	
		try {
			return ISODateTimeFormat.dateOptionalTimeParser().parseDateTime(t);    	
		}
		catch (Exception x) {			
			// TODO System.out.println(x.toString());
			
			try {
				return TimeUtil.stampFmt.parseDateTime(t);    	
			}
			catch (Exception x2) {			
				// TODO System.out.println(x.toString());
			}
		}
		
		return null;
    }
    
    /**
     * parse just the time
     * 
     * @param t string with iso formatted time
     * @return time if parsed, or null
     */
	static public LocalTime parseLocalTime(String t) {		
		if (StringUtil.isEmpty(t))
			return null;

		try {
			return ISODateTimeFormat.timeParser().parseDateTime(t).toLocalTime();
		}
		catch (Exception x) {			
		}
		
		return null;
	}
	
	/**
	 * return number of weeks since Jan 5, 1970
	 * 
	 * @param v date to calculate week number off of
	 * @return number of weeks
	 */
	static public int getWeekNumber(DateTime v) {
		DateTime root = new DateTime(1970, 1, 5, 0, 0, 0, 0, v.getZone());   // use the same zone
		
		return Weeks.weeksBetween(root, v).getWeeks();
		
		//long n = v.getMillis() - 345600000;  // start of first week
		//return (int) (n / 604800000);
	}
	
	/**
	 * return datetime at start of week number relative to Jan 5, 1970
	 * 
	 * @param weekNum the week number to use
	 * @return datetime that week started
	 */
	static public DateTime getStartOfWeek(int weekNum) {
		return new DateTime(1970, 1, 5, 0, 0, 0, 0).plusWeeks(weekNum);		// use default zone
		
		//long n = (long)weekNum * 604800000;
		//return new DateTime(n + 345600000);
	}
	
	/**
	 * return number of months since Jan 1, 1970
	 * 
	 * @param v date to calculate month number off of
	 * @return number of months
	 */
	static public int getMonthNumber(DateTime v) {
		DateTime root = new DateTime(1970, 1, 1, 0, 0, 0, 0, v.getZone());   // use the same zone
		
		return Months.monthsBetween(root, v).getMonths();
		
		//int n = (v.getYear() - 1970) * 12;  
		//return n + v.getMonthOfYear() - 1;
	}
	
	/**
	 * return datetime at start of month number relative to Jan 1, 1970
	 * 
	 * @param monthNum the month number to use
	 * @return datetime that month started
	 */
	static public DateTime getStartOfMonth(int monthNum) {
		return new DateTime(1970, 1, 1, 0, 0, 0, 0).plusMonths(monthNum);
		
		//return new DateTime((monthNum / 12) + 1970, (monthNum % 12) + 1, 1, 0, 0, 0, 0);
	}

	/**
	 * Format date and time in Long format
	 * 
	 * @param at datetime to format
	 * @param zone which timezone to use
	 * @param locale which locale to use 
	 * @return formatted datetime
	 */
	static public String fmtDateTimeLong(DateTime at, String zone, String locale) {
		if (at == null)
			return null;
		
		return TimeUtil.fmtDateTimeLong(at.withZone(TimeUtil.selectZone(zone)), locale);
	}
	
	/**
	 * Format date and time in Long format
	 * 
	 * @param at datetime to format
	 * @param zone which timezone to use
	 * @param locale which locale to use 
	 * @return formatted datetime
	 */
	static public String fmtDateTimeLong(DateTime at, DateTimeZone zone, String locale) {
		if (at == null)
			return null;
		
		return TimeUtil.fmtDateTimeLong(at.withZone(zone) , locale);
	}
	
	/**
	 * Format date and time in Long format
	 * 
	 * @param at datetime to format
	 * @param locale which locale to use 
	 * @return formatted datetime
	 */
	static public String fmtDateTimeLong(DateTime at, String locale) {
		if (at == null)
			return null;
		
		// TODO user locale to look up format
		DateTimeFormatter fmt = DateTimeFormat.forPattern("MMMM dd yyyy hh:mm:ss a z");
		return fmt.print(at);
	}
	
	/**
	 * Format date in Long format
	 * 
	 * @param at datetime to format
	 * @param zone which timezone to use
	 * @param locale which locale to use 
	 * @return formatted date
	 */
	static public String fmtDateLong(DateTime at, String zone, String locale) {
		if (at == null)
			return null;
		
		return TimeUtil.fmtDateLong(at.withZone(TimeUtil.selectZone(zone)), locale);
	}	
	
	/**
	 * Format date in Long format
	 * 
	 * @param at datetime to format
	 * @param zone which timezone to use
	 * @param locale which locale to use 
	 * @return formatted date
	 */
	static public String fmtDateLong(DateTime at, DateTimeZone zone, String locale) {
		if (at == null)
			return null;
		
		return TimeUtil.fmtDateLong(at.withZone(zone) , locale);
	}
	
	/**
	 * Format date in Long format
	 * 
	 * @param at datetime to format
	 * @param locale which locale to use 
	 * @return formatted date
	 */
	static public String fmtDateLong(DateTime at, String locale) {
		if (at == null)
			return null;
		
		// TODO user locale to look up format
		DateTimeFormatter fmt = DateTimeFormat.forPattern("MMMM dd yyyy");
		return fmt.print(at);
	}

	/**
	 * gets the correct timezone for use based on setting in a message
	 * Useful because request messages may hold info on their timezone.
	 * 
	 * @param msg source of timezone metadata
	 * @return the selected timezone
	 */
	static public DateTimeZone selectZone(Message msg) {
		return TimeUtil.selectZone(msg.getFieldAsString("TimeZone"));
	}
	
	/**
	 * try to lookup a timezone, but use default if it fails 
	 * 
	 * @param zoneId id of the timezone desired
	 * @return timezone to use
	 */
	static public DateTimeZone selectZone(String zoneId) {
		DateTimeZone zone = DateTimeZone.getDefault();
		
		try {
			if (StringUtil.isNotEmpty(zoneId))
				zone = DateTimeZone.forID(zoneId);
		}
		catch (Exception x) {			
		}
		
		return zone;
	}
	
	/**
	 * Parse string to CoreLocalTime - a flexible format that allows hours > 23, useful for some settings.
	 * 
	 * @param t string holding hours:minutes:seconds
	 * @return time object
	 */
	static public CoreLocalTime parseCoreLocalTime(String t) {		
		if (StringUtil.isEmpty(t))
			return null;

		String[] parts = t.trim().split(":");
		
		int h = 0;
		int m = 0;
		int s = 0;
		
		if (parts.length >= 1)
			h = (int)StringUtil.parseInt(parts[0], 0);
		
		if (parts.length >= 2)
			m = (int)StringUtil.parseInt(parts[1], 0);
		
		if (parts.length >= 3)
			s = (int)StringUtil.parseInt(parts[2], 0);
		
		try {
			return new CoreLocalTime(h, m, s, 0);
		}
		catch (Exception x) {			
		}
		
		return null;
	}

	/**
	 * detect if given datetime lands on a DST transition
	 * 
	 * @param n date to check
	 * @return START if is a start of DST, END if is a end of DST, NA means day is not a transition
	 */
	public static DaylightTransition checkDST(DateTime n) {
		DateTime start = TimeUtil.startDST(n.getYear());
		
		if (start.toLocalDate().equals(n.toLocalDate()))
			return DaylightTransition.START;
		
		DateTime end = TimeUtil.startDST(n.getYear());
		
		if (end.toLocalDate().equals(n.toLocalDate()))
			return DaylightTransition.END;
		
		return DaylightTransition.NA;
	}
	
	public enum DaylightTransition {
		NA,
		START,
		END
	}
	
	/**
	 * get the DST start transition for a given year
	 * 
	 * @param zone timezone to use for DST rules
	 * @param year the year to use, e.g. 2012
	 * @return datetime of the start transition
	 */
    public static DateTime startDST(DateTimeZone zone, int year) {
        return new DateTime(zone.nextTransition(new DateTime(year, 1, 1, 0, 0, 0, 0, zone).getMillis()));
    }

	/**
	 * get the DST start transition for a given year
	 * 
	 * @param year the year to use, e.g. 2012
	 * @return datetime of the start transition
	 */
    public static DateTime startDST(int year) {
    	DateTimeZone zone = DateTimeZone.getDefault();
        return new DateTime(zone.nextTransition(new DateTime(year, 1, 1, 0, 0, 0, 0, zone).getMillis()));
    }

	/**
	 * get the DST end transition for a given year
	 * 
	 * @param zone timezone to use for DST rules
	 * @param year the year to use, e.g. 2012
	 * @return datetime of the end transition
	 */
    public static DateTime endDST(DateTimeZone zone, int year) {
        return new DateTime(zone.previousTransition(new DateTime(year + 1, 1, 1, 0, 0, 0, 0, zone).getMillis()));
    }	

	/**
	 * get the DST end transition for a given year
	 * 
	 * @param year the year to use, e.g. 2012
	 * @return datetime of the end transition
	 */
    public static DateTime endDST(int year) {
    	DateTimeZone zone = DateTimeZone.getDefault();
        return new DateTime(zone.previousTransition(new DateTime(year + 1, 1, 1, 0, 0, 0, 0, zone).getMillis()));
    }
    
	/**
	 * BigDateTime can handle dates into the billions of years, both past and future.  It is designed to be
	 * a versatile way to handle historical data.  Dates are based off proleptic Gregorian, time is optional
	 * but if present then is based off UTC zone.  
	 * 
	 * @param date 		in string format 
	 * @return 			converted to object or null if not able to parse
	 * 
	 * @see 			divconq.lang.BigDateTime
	 */
	public static BigDateTime parseBigDateTime(String date) {
		return BigDateTime.parse(date).getResult();
	}	
	
	// this assumes that the time stamp - as formatted - will be in UTC time.  All DC values should be store in UTC time.
	public static DateTime convertSqlDate(java.sql.Timestamp v) {
		if (v == null)
			return null;
		
		String dt = TimeUtil.sqlStampReformat.format(v) + "Z";
		
		return TimeUtil.stampFmt.parseDateTime(dt);		
	}
}
