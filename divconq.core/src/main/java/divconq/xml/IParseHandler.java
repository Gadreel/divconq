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

import java.util.Map;

import divconq.lang.OperationResult;

/**
 * Use to create a custom Xml parser for use with XmlParser.  SAX like
 * processing which handles start and end of elements as well as text
 * nodes.
 * 
 * @author Andy
 *
 */
public interface IParseHandler {
	/**
	 * Called to indicate the start of a tagged element
	 * 
	 * @param tag
	 *            the name part of the tag
	 * @param attributes
	 *            the table of attributes for this element
	 * @param line
	 *            the line number where this element was started
	 * @param col
	 *            the column number where this element was started
	 * @throws XMLParseException
	 *             if a semantic error is observed
	 */
	public void startElement(OperationResult or, String tag, Map<String, String> attributes, int line, int col);

	/**
	 * Called to indicate the end of a tagged element
	 * 
	 * @param tag
	 *            the name part of the tag
	 * @throws XMLParseException
	 *             if a semantic error is observed
	 */
	public void endElement(OperationResult or, String tag);
	
	/**
	 * Called to indicate a complete tagged element
	 * 
	 * @param tag
	 *            the name part of the tag
	 * @param attributes
	 *            the table of attributes for this element
	 * @param line
	 *            the line number where this element was started
	 * @param col
	 *            the column number where this element was started
	 * @throws XMLParseException
	 *             if a semantic error is observed
	 */
	public void element(OperationResult or, String tag, Map<String, String> attributes, int line, int col);

	/**
	 * Called to indicate the start of the XML document being read
	 * 
	 * @throws XMLParseException
	 *             if a semantic error is observed
	 */
	public void startDocument(OperationResult or);

	/**
	 * Called to indicate the end of the XML document being read
	 * 
	 * @throws XMLParseException
	 *             if a semantic error is observed
	 */
	public void endDocument(OperationResult or);

	/**
	 * Called to indicate that an untagged element has been read
	 * 
	 * @param str
	 *            the value of the untagged element
	 * @param cdata
	 *            true if the element was a CDATA element
	 * @param line
	 *            the line number where this element was started
	 * @param col
	 *            the column number where this element was started
	 * @throws XMLParseException
	 *             if a semantic error is observed
	 */
	public void text(OperationResult or, String str, boolean cdata, int line, int col);
}
