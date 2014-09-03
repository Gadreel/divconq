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
package divconq.view.html;

import java.util.Map.Entry;

import divconq.view.Attributes;
import divconq.xml.XElement;

public class HtmlUtil  {
    static public Attributes initAttrs(XElement xel) {
		Attributes attrs = new Attributes();
		
		if (xel == null)
			return attrs;
		
		if (xel.hasAttribute("accesskey"))
			attrs.add("accesskey", xel.getRawAttribute("accesskey"));
		
		if (xel.hasAttribute("class"))
			attrs.add("class", xel.getRawAttribute("class"));
		
		if (xel.hasAttribute("dir"))
			attrs.add("dir", xel.getRawAttribute("dir"));
		
		if (xel.hasAttribute("id"))
			attrs.add("id", xel.getRawAttribute("id"));
		
		if (xel.hasAttribute("lang"))
			attrs.add("lang", xel.getRawAttribute("lang"));
		
		if (xel.hasAttribute("style"))
			attrs.add("style", xel.getRawAttribute("style"));
		
		if (xel.hasAttribute("tabindex"))
			attrs.add("tabindex", xel.getRawAttribute("tabindex"));
		
		if (xel.hasAttribute("title"))
			attrs.add("title", xel.getRawAttribute("title"));   			
		
		if (xel.hasAttribute("onclick"))
			attrs.add("onclick", xel.getRawAttribute("onclick"));   			
		
		// copy all data attributes
		for (Entry<String, String> entry : xel.getAttributes().entrySet()) {
			if (entry.getKey().startsWith("data-"))
				attrs.add(entry.getKey(), entry.getValue());
		}
		
		return attrs;
    }
}
