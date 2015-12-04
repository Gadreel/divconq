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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Translation {
	protected String name = null;
	protected Map<String,String> strings = new HashMap<String,String>();
	
	public String getName() {
		return this.name;
	}
	
	public Translation(String name) {
		this.name = name;
	}
	
	public String get(String token) {
		if (token == null)
			return null;
		
		return this.strings.get(token);
	}

	public boolean has(String token) {
		return this.strings.containsKey(token);
	}

	public void put(String token, String v) {
		this.strings.put(token, v);
	}

	public Collection<String> getTokens() {
		return this.strings.keySet();
	}
}
