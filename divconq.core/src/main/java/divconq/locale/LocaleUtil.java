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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import divconq.bus.Message;
import divconq.hub.Hub;
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
	 * translate the token and parameters using the provided locale
	 * 
	 * @param locale name of the local to use
	 * @param token name of the dictionary item to lookup
	 * @param params parameters to use when formatting the output
	 * @return translated and formatted text
	 */
	static public String tr(String locale, String token, Object... params) {
		Localization loc = Hub.instance.getDictionary();
		
		if (loc != null)
			return loc.tr(locale, token, params);
		
		return null;
	}
	
	/**
	 * translate a token and parameters using the provided locale.
	 * if the first parameter is numeric and 1 then use the singular token
	 * otherwise lookup the plural token
	 * 
	 * @param locale name of the local to use
	 * @param pluraltoken name of the dictionary item to lookup
	 * @param singulartoken name of the dictionary item to lookup
	 * @param params parameters to use when formatting the output
	 * @return translated and formatted text
	 */
	static public String trp(String locale, String pluraltoken, String singulartoken, Object... params) {
		Localization loc = Hub.instance.getDictionary();
		
		if (loc != null)
			return loc.trp(locale, pluraltoken, singulartoken, params);
		
		return null;
	}

	/**
	 * Messages contain locale metadata, use that to try to get a Java style Locale
	 * 
	 * @param m message with metadata
	 * @return java type locale
	 */
	static public Locale getLocale(Message m) {		
		return LocaleUtil.getLocale(m.getFieldAsString("Locale"));
	}
	
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
	
	/**
	 * Try to get a DivConq style Locale for a locale id string
	 * 
	 * @param ll locale id
	 * @return DivConq type locale
	 */
	static public LocaleInfo getLocaleInfo(String ll) {		
		if (StringUtil.isEmpty(ll)) 
			return null;
		
		Localization loc = Hub.instance.getDictionary();
		
		if (loc != null)
			return loc.getLocalization(ll);
		
		return null;
	}
	
	/*
	// TODO include region - TODO get aspects of this idea working
	public static ViewLocale lookup(String lang) {
		if (!ViewLocale.locales.containsKey(lang)) {
			Locale l = null;
			boolean ltr = false;
			
			if (lang.equals("x-pig-latin")) {
				l = new Locale("it");		// Italian, as an example only
			}
			else if (lang.equals("x-hsilgne")) {
				l = new Locale("iw");		// Hebrew, as an example only
				ltr = true;
			}
			else {
				ltr = ("ar".equals(lang) || "iw".equals(lang));				
				l = new Locale(lang);
			}
			
			ViewLocale ml = new ViewLocale(lang, l, ltr);
			ViewLocale.locales.put(lang, ml);
			return ml;
		}
		
		return ViewLocale.locales.get(lang);
	}
	*/	

	/**
	 * 
	 * @return the default locale id string
	 */
	public static String getDefaultLocale() {
		Localization loc = Hub.instance.getDictionary();
		
		if (loc != null)
			return loc.getDefault();
		
		return Locale.getDefault().toString();
	}

	/**
	 * 
	 * @return DivConq type locale for default locale
	 */
	public static LocaleInfo getDefaultLocaleInfo() {
		Localization loc = Hub.instance.getDictionary();
		
		if (loc != null)
			return loc.getLocalization(null);
		
		return null;
	}
	
	/**
	 * Get list of locale ids (lang id and lang_region id) to look for based on a preferred locale id
	 *  
	 * @param locale id string of preferred locale
	 * @return list of possible locale ids to use
	 */
	static public List<String> getSearchPath(String locale) {
		return LocaleUtil.getSearchPath(LocaleUtil.getLocaleInfo(locale));		
	}
	
	/**
	 * Get list of locale ids (lang id and lang_region id) to look for based on a preferred locale id
	 *  
	 * @param locale DivConq style locale object of preferred locale
	 * @return list of possible locale ids to use
	 */
	static public List<String> getSearchPath(LocaleInfo locale) {
		List<String> path = new ArrayList<String>();
		
		if (locale != null) {
			String tn = locale.getName();
			
			path.add(tn);
			
			if (tn.indexOf("_") > -1) 
				path.add(tn.substring(0, tn.indexOf("_")));
		}
		
		// add default
		//path.add(LocaleUtil.getDefaultLocale());
		
		if (locale.isRightToLeft())
			path.add("rtl");
		
		// add default
		path.add("common");
		
		return path;
	}
}
