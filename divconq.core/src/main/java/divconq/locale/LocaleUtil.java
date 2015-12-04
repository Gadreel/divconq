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

import java.util.Locale;

import divconq.util.StringUtil;

/**
 * as much as possible DivConq attempts to avoid using Java's locales because they are
 * not extensible.  Translations rely entirely on our own Hub dictionaries.  Formatting
 * of dates and numbers still uses some Java locales for now.
 * 
 * @author Andy
 *
 */
public class LocaleUtil {
	/**
	 * Try to get a Java style Locale for a locale id string
	 * 
	 * @param ll locale id
	 * @return java type locale
	 */
	static public Locale getLocale(String ll) {		
		if (StringUtil.isEmpty(ll)) 
			return null;

		if (ll.contains("_")) {
			String[] lp = ll.split("_");
			return new Locale(lp[0], lp[1].toUpperCase());
		}
		
		return new Locale(ll);
	}
}
