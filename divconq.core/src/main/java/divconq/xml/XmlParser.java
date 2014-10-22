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

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Stack;

import divconq.lang.OperationResult;
import divconq.util.IOUtil;

/**
 * Quick and Dirty XML parser. This parser is, like the SAX parser, an event
 * based parser, but with much less functionality.  Based off of QDParser
 * by Kevin Twidle see http://twicom.com/ 
 */
public class XmlParser {
	private static int popMode(Stack<Integer> st) {
		if (!st.empty())
			return st.pop().intValue();
		return PRE;
	}

	private final static int TEXT = 1, ENTITY = 2, OPEN_TAG = 3, CLOSE_TAG = 4,
			START_TAG = 5, ATTRIBUTE_LVALUE = 6, ATTRIBUTE_EQUAL = 9,
			ATTRIBUTE_RVALUE = 10, QUOTE = 7, IN_TAG = 8, SINGLE_TAG = 12,
			COMMENT = 13, IGNORE = 14, PRE = 15, CDATA = 16,
			OPEN_INSTRUCTION = 17;

	/*
	 * Parses XML from a reader and returns a data structure containing the
	 * parsed XML.
	 * 
	 * @param doc
	 *            the DocHandler that will be given the different elements of
	 *            the XML
	 * @param reader
	 *            the Reader to get the source XML from
	 * @throws XMLParseException
	 *             if an error in the XML is detected
	 * @throws IOException
	 *             if an error using the Reader is detected
	 */
	public static OperationResult parse(IParseHandler doc, Reader reader) {
		OperationResult or = new OperationResult();
		
		try {
			Stack<Integer> st = new Stack<Integer>();
			int depth = 0;
			int mode = PRE;
			int c = 0;
			int quotec = '"';
			depth = 0;
			StringBuffer sb = new StringBuffer();
			StringBuffer etag = new StringBuffer();
			String tagName = null;
			String lvalue = null;
			String rvalue = null;
			Map<String, String> attrs = null;
			
			doc.startDocument(or);
			
			if (or.hasErrors())
				return or;
			
			int line = 1, col = 0;
			boolean eol = false;
	
			// TODO add support for surrogate pair, set String Builder 32
			while ((c = reader.read()) != -1) {
				// We need to map \r, \r\n, and \n to \n
				// See XML spec section 2.11
				if (c == '\n' && eol) {
					eol = false;
					continue;
				} 
				else if (eol) {
					eol = false;
				} 
				else if (c == '\n') {
					line++;
					col = 0;
				} 
				else if (c == '\r') {
					eol = true;
					c = '\n';
					line++;
					col = 0;
				} 
				else {
					col++;
				}
	
				if (mode == TEXT) {
					// We are between tags collecting text.
					if (c == '<') {
						st.push(new Integer(mode));
						mode = START_TAG;
						if (sb.length() > 0) {
							doc.text(or, sb.toString(), false, line, col);
							
							if (or.hasErrors())
								return or;
							
							sb.setLength(0);
						}
					} 
					else if (c == '&') {
						st.push(new Integer(mode));
						mode = ENTITY;
						etag.setLength(0);
					} 
					else
						sb.append((char) c);
	
				} 
				else if (mode == CLOSE_TAG) {
					// we are processing a closing tag: e.g. </foo>
					if (c == '>') {
						mode = popMode(st);
						tagName = sb.toString();
						sb.setLength(0);
						depth--;
						
						doc.endElement(or, tagName);
						
						if (or.hasErrors())
							return or;
						
						if (depth == 0) {
							doc.endDocument(or);
							return or;
						}
					} 
					else {
						sb.append((char) c);
					}
	
				} 
				else if (mode == CDATA) {
					// we are processing CDATA
					if (c == '>' && sb.toString().endsWith("]]")) {
						sb.setLength(sb.length() - 2);
						
						doc.text(or, sb.toString(), true, line, col);
						
						if (or.hasErrors())
							return or;
						
						sb.setLength(0);
						mode = popMode(st);
					} 
					else
						sb.append((char) c);
	
				} 
				else if (mode == COMMENT) {
					// we are processing a comment. We are inside
					// the <!-- .... --> looking for the -->.
					if (c == '>' && sb.toString().endsWith("--")) {
						sb.setLength(0);
						mode = popMode(st);
					} 
					else
						sb.append((char) c);
	
				} 
				else if (mode == PRE) {
					// We are outside the root tag element
					if (c == '<') {
						mode = TEXT;
						st.push(new Integer(mode));
						mode = START_TAG;
					}
	
				} 
				else if (mode == IGNORE) {
					// We are inside one of these <? ... ?>
					// or one of these <!DOCTYPE ... >
					if (c == '>') {
						mode = popMode(st);
						if (mode == TEXT)
							mode = PRE;
					}
	
				} 
				else if (mode == START_TAG) {
					// we have just seen a < and
					// are wondering what we are looking at
					// <foo>, </foo>, <!-- ... --->, etc.
					mode = popMode(st);
					if (c == '/') {
						st.push(new Integer(mode));
						mode = CLOSE_TAG;
					} 
					else if (c == '?') {
						mode = IGNORE;
					} 
					else if (c == '!') {
						st.push(new Integer(mode));
						mode = OPEN_INSTRUCTION;
						tagName = null;
						attrs = new Hashtable<String, String>();
						sb.append((char) c);
					} 
					else if (c == '_' || Character.isLetter(c)) {
						st.push(new Integer(mode));
						mode = OPEN_TAG;
						tagName = null;
						attrs = new Hashtable<String, String>();
						sb.append((char) c);
					} 
					else {
						or.errorTr(242, line, col, (char) c);
						return or;
					}
	
				} 
				else if (mode == ENTITY) {
					// we are processing an entity, e.g. &lt;, &#187;, etc.
					if (c == ';') {
						mode = popMode(st);
						String cent = etag.toString();
						etag.setLength(0);
						
						/*
						if (cent.equals("lt"))
							sb.append('<');
						else if (cent.equals("gt"))
							sb.append('>');
						else if (cent.equals("amp"))
							sb.append('&');
						else if (cent.equals("quot"))
							sb.append('"');
						else if (cent.equals("apos"))
							sb.append('\'');
						else if (cent.startsWith("#x"))
							sb.append((char) Integer.parseInt(cent.substring(2), 16));
						else if (cent.startsWith("#"))
							sb.append((char) Integer.parseInt(cent.substring(1)));
						else {
							// Just keep the unknown entity
							sb.append('&');
							sb.append(cent);
							sb.append(';');
							// exc("Unknown entity: &" + cent + ";", line, col);
						}
						*/
						
						// APW Just keep the entity
						sb.append('&');
						sb.append(cent);
						sb.append(';');
					} 
					else {
						etag.append((char) c);
					}
	
				} 
				else if (mode == SINGLE_TAG) {
					// we have just seen something like this:
					// <foo a="b"/
					// and are looking for the final >.
					if (tagName == null)
						tagName = sb.toString();
					
					if (c != '>') {
						or.errorTr(241, line, col, tagName);
						return or;
					}
					
					doc.element(or, tagName, attrs, line, col);
					//doc.endElement(tagName);
					
					if (or.hasErrors())
						return or;
					
					if (depth == 0) {
						doc.endDocument(or);
						return or;
					}
					
					sb.setLength(0);
					attrs = new HashMap<String, String>();
					tagName = null;
					mode = popMode(st);
	
				} 
				else if (mode == OPEN_INSTRUCTION) {
					// we are processing <!... >.
					// We already have the first character
					if (c == '>') {
						or.errorTr(241, line, col, sb.toString());
						return or;
					} 
					else if (c == '-' && sb.toString().equals("!-")) {
						mode = COMMENT;
					} 
					else if (c == '[' && sb.toString().equals("![CDATA")) {
						mode = CDATA;
						sb.setLength(0);
					} 
					else if (c == 'E' && sb.toString().equals("!DOCTYP")) {
						sb.setLength(0);
						mode = IGNORE;
					} 
					else if (Character.isWhitespace((char) c)) {
						or.errorTr(240, line, col, sb.toString());
						return or;
					} 
					else {
						// We have a character to add to the instruction
						// Check for length
						if (sb.length() > 9) {
							or.errorTr(239, line, col, sb.toString());
							return or;
						}
						
						// Check for validity
						if (c == '-' || c == '[' || Character.isLetter(c))
							sb.append((char) c);
						else {
							or.errorTr(238, line, col, c, sb.toString());
							return or;
						}
					}
				} 
				else if (mode == OPEN_TAG) {
					// we are processing something
					// like this <foo ... >.
					// We already have the first character
					if (c == '>') {
						if (tagName == null)
							tagName = sb.toString();
						
						sb.setLength(0);
						depth++;
						
						doc.startElement(or, tagName, attrs, line, col);
						
						if (or.hasErrors())
							return or;
						
						tagName = null;
						attrs = new HashMap<String, String>();
						mode = popMode(st);
					} 
					else if (c == '/') {
						mode = SINGLE_TAG;
					} 
					else if (Character.isWhitespace((char) c)) {
						tagName = sb.toString();
						sb.setLength(0);
						mode = IN_TAG;
					} 
					else {
						// We have a character to add to the name
						// Check for validity
						if (Character.isLetterOrDigit(c) || c == '_' || c == '-'
								|| c == '.' || c == ':')
							sb.append((char) c);
						else {
							or.errorTr(237, line, col, c);
							return or;
						}
					}
	
				}
				else if (mode == QUOTE) {
					// We are processing the quoted right-hand side
					// of an element's attribute.
					if (c == quotec) {
						rvalue = sb.toString();
						sb.setLength(0);
						attrs.put(lvalue, rvalue);
						mode = IN_TAG;
						// See section the XML spec, section 3.3.3
						// on normalization processing.
					} 
					else if (" \r\n\u0009".indexOf(c) >= 0) {
						sb.append(' ');
					} 
					else if (c == '&') {
						st.push(new Integer(mode));
						mode = ENTITY;
						etag.setLength(0);
					} 
					else {
						sb.append((char) c);
					}
	
				} 
				else if (mode == ATTRIBUTE_RVALUE) {
					if (c == '"' || c == '\'') {
						quotec = c;
						mode = QUOTE;
					} 
					else if (Character.isWhitespace((char) c)) {
						;
					} 
					else {
						or.errorTr(236, line, col);
						return or;
					}
	
				} 
				else if (mode == ATTRIBUTE_LVALUE) {
					if (Character.isWhitespace((char) c)) {
						lvalue = sb.toString();
						sb.setLength(0);
						mode = ATTRIBUTE_EQUAL;
					} 
					else if (c == '=') {
						lvalue = sb.toString();
						sb.setLength(0);
						mode = ATTRIBUTE_RVALUE;
					} 
					else {
						sb.append((char) c);
					}
	
				} 
				else if (mode == ATTRIBUTE_EQUAL) {
					if (c == '=') {
						mode = ATTRIBUTE_RVALUE;
					} 
					else if (Character.isWhitespace((char) c)) {
						;
					} 
					else {
						or.errorTr(235, line, col);
						return or;
					}
	
				} 
				else if (mode == IN_TAG) {
					if (c == '>') {
						mode = popMode(st);
						
						doc.startElement(or, tagName, attrs, line, col);
						
						if (or.hasErrors())
							return or;
						
						depth++;
						tagName = null;
						attrs = new HashMap<String, String>();
					} 
					else if (c == '/') {
						mode = SINGLE_TAG;
					} 
					else if (Character.isWhitespace((char) c)) {
						;
					} 
					else {
						mode = ATTRIBUTE_LVALUE;
						sb.append((char) c);
					}
				}
			}
			
			if (mode != PRE)
				or.errorTr(234, line, col);
			
			return or;
		}
		catch (IOException x) {
			or.error("Erroring reading XML: " + x);
		}
		finally {
			IOUtil.closeQuietly(reader);
		}
		
		return or;
	}
}
