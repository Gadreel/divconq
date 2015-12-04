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

public class LocaleDefinition {
	protected Locale loc = null;
	protected String name = null;
	protected String lang = null;
	protected String variant = null;
	protected boolean rtl = false;
	
	public Locale getLocale() {
		return this.loc;
	}
	
	public String getName() {
		return this.name;
	}

	public String getVariant() {
		return this.variant;
	}
	
	public boolean hasVariant() {
		return StringUtil.isNotEmpty(this.variant);
	}
	
	public String getLanguage() {
		return this.lang;
	}
	
	public boolean isRightToLeft() {
		return this.rtl;
	}

	// assumes name is [lang]/[variant] - this is because some languages are x-name/x-variant
	public LocaleDefinition(String name) {
		this.name = name;
		this.loc = LocaleUtil.getLocale(name);		// TODO convert to ln-LL ??? 
		
		int pos = this.name.indexOf('_');
		
		this.lang = (pos != -1) ? this.name.substring(0, pos) : this.name;
		this.variant = (pos != -1) ? this.name.substring(pos + 1) : null;

		// TODO infer any settings from loc if possible
		
		if (this.loc != null) {
			this.rtl = (Character.getDirectionality(this.loc.getDisplayName(this.loc).charAt(0)) == Character.DIRECTIONALITY_RIGHT_TO_LEFT);
		}
		
		// TODO load dc specific info about locale 
		// return "true".equals(this.get("rtl"));  
	}

	public boolean match(String locale) {
		if (locale.startsWith(this.name))
			return true;
		
		return false;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LocaleDefinition)
			return this.name.equals(((LocaleDefinition)obj).name);
		
		return super.equals(obj);
	}
}
