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
package divconq.xml;

/**
 * Store the key and value pair for an Xml attribute.
 * 
 * @author Andy
 *
 */
public class XAttribute {
	protected String name = null;
	protected String value = null;

	/**
	 * 
	 * @return attribute's name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 
	 * @return attributes value
	 */
	public String getValue() {
		return XNode.unquote(this.value);
	}
	
	public String getRawValue() {
		return this.value;
	}
	
	/**
	 * Create an element with immutable key and value pair
	 *  
	 * @param name of attribute
	 * @param value of attribute
	 */
	public XAttribute(String name, String value) {
		this.name = name;
		this.value = XNode.quote(value);
	}
}
