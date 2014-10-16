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
package divconq.log;

import divconq.util.StringUtil;

/**
 * When logging messages to the debug log each message has a debug level
 * based on this enum.  The logger has a filter level based on this enum
 * also.  Messages of lower priority than the current debug level will
 * not be logged.
 * 
 * Note that 99% of the time the "current" debug level is determined by
 * the current TaskContext.  The preferred way to log messages is through 
 * the TaskContext or through an OperationResult.  Ultimately a filter
 * is used to determine what should go in the log.  
 * 
 * @author Andy
 *
 */
public enum DebugLevel {
    None(0),
    Error(1),
    Warn(2),
    Info(3),
    Debug(4),
    // use with very frequent messages (<5 sec repeat) or very detailed - use debug mostly
    Trace(5);
    
    private int code;

    private DebugLevel(int c) {
      code = c;
    }

    public int getCode() {
      return code;
    }

	public String getIndicator() {
		return "0" + code;
	}

	public static DebugLevel parse(String level) {
		return DebugLevel.valueOf(level);
	}

	public static DebugLevel parseIndicator(String ind) {
		if ((ind == null) || (ind.length() != 3))
			return None;
		
		return DebugLevel.parseCode(StringUtil.parseInt(ind.substring(2)));
	}

	public static DebugLevel parseCode(Long code) {
		if (code == null)
			return None;
		
		return DebugLevel.parseCode((long)code);
	}

	public static DebugLevel parseCode(long code) {
		switch ((int)code) {
		case 1:
			return Error;
		case 2:
			return Warn;
		case 3:
			return Info;
		case 4:
			return Debug;
		case 5:
			return Trace;
		default:
			return None;
		}
	}    
}


/*
 * 
 * Codes
 * 
 * 1 - 39,999			dc (Java Framework core)
 * 
 * 40,000 - 49,999		dct (Java Test core)
 * 
 * 50,000 - 79,999		dc (M Framework core)
 * 
 * 80,000 - 89,999		z (generic customer code)
 * 
 * 90,000 - 99,999		dct (M Test code)
 * 
 * 100,000 - 1,757,500,000	vendor range based on 3 character abbr (e.g. dci)
 * 
 * 		http://www.unitconversion.org/unit_converter/numbers.html?unit=base-26
 * 		where a = 0 and z = p
 * 
 * 		208,800,000 - 208,899,999	dci
 * 
 */

