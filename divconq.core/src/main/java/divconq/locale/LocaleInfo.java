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


public class LocaleInfo extends Translations {
	protected Locale loc = null;
	
	public LocaleInfo(String name) {
		super(name);
		
		this.loc = LocaleUtil.getLocale(name);
	}
	
	public Locale getLocale() {
		return this.loc;
	}

}
