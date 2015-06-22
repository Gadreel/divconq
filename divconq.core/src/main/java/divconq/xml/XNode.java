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

import divconq.lang.Memory;

/**
 * Super class to support all xml classes (XElement and XTex)
 * 
 * @author Andy
 *
 */
public abstract class XNode {
  /**
   * Returns formatted or unformatted XML source.
   * 
   * @param formatted
   *            whether to return formatted XML source. If true, the source is
   *            pretty-printed with new lines and indentation. If false, the XML
   *            string is returned as one lone, unformatted line.
   * @return a String containing the XML source
   */
  public String toString(boolean formatted) {
    StringBuffer sb = new StringBuffer();
    return toString(sb, formatted, 0).toString();
  }

  /**
   * Internal method used recursively to format XML with appropriate
   * indentation.
   * 
   * @param sb
   *            destination for xml (character) output
   * @param formatted
   *            whether to return formatted XML source. If true, the source is
   *            pretty-printed with new lines and indentation. If false, the XML
   *            string is returned as one long, unformatted line.
   * @param level
   *            the indentation level used to write leading spaces
   * 
   * @return a String containing the XML source
   */
  protected abstract StringBuffer toString(StringBuffer sb, boolean formatted, int level);

  /**
   * quotes a string according to XML rules. When attributes and text elements
   * are written out special characters have to be quoted.
   * 
   * @param string
   *            the string to process
   * @return the string with quoted special characters
   */
	public static String quote(String string) {
		if (string == null)
			return null;
		
	    StringBuffer sb = new StringBuffer();
	    
	    for (int i = 0; i < string.length(); i++) {
	      char ch = string.charAt(i);
	      switch (ch) {
	        case '&':
	          sb.append("&amp;");
	          break;
	        case '<':
	          sb.append("&lt;");
	          break;
	        case '>':
	          sb.append("&gt;");
	          break;
	        case '"':
	          sb.append("&quot;");
	          break;
	        case '\'':
	          sb.append("&apos;");
	          break;
	        default:
	          sb.append(ch);
	      }
	    }
	    
	    return sb.toString();
   }

	  /**
	   * Returns formatted or unformatted XML source.
	   * 
	   * @param formatted
	   *            whether to return formatted XML source. If true, the source is
	   *            pretty-printed with new lines and indentation. If false, the XML
	   *            string is returned as one lone, unformatted line.
	   * @return Memory containing the XML source (utf-8)
	   */
	public Memory toMemory(boolean formatted) {
		Memory m = new Memory();
		this.toMemory(m, formatted, 0);
		return m;
	}
	
	abstract protected void toMemory(Memory sb, boolean formatted, int level);

	abstract public XNode deepCopy();

  /**
   * quotes a string according to XML rules. When attributes and text elements
   * are written out special characters have to be quoted.
   * 
   * @param string
   *            the string to process
   * @return the string with quoted special characters
   */
  public static String unquote(String string) {
	if (string == null)
		return null;
	  
    StringBuffer sb = new StringBuffer();
    boolean inQuote = false;
    StringBuffer quoteBuf = new StringBuffer();
    
    for (int i = 0; i < string.length(); i++) {
      char ch = string.charAt(i);
      
      if (inQuote) {
        if (ch == ';') {
          String quote = quoteBuf.toString();
          if (quote.equals("lt"))
            sb.append('<');
          else if (quote.equals("gt"))
            sb.append('>');
          else if (quote.equals("amp"))
            sb.append('&');
          else if (quote.equals("quot"))
            sb.append('"');
          else if (quote.equals("apos"))
            sb.append('\'');
          else if (quote.startsWith("#x"))
            sb.append((char)Integer.parseInt(quote.substring(2), 16));
          else if (quote.startsWith("#"))
            sb.append((char)Integer.parseInt(quote.substring(1)));
          else
            sb.append(quoteBuf);
          inQuote = false;
          quoteBuf.setLength(0);
        }
        else {
          quoteBuf.append(ch);
        }
      }
      else {
        if (ch == '&')
          inQuote = true;
        else
          sb.append(ch);
      }
    }
    
    if (inQuote)
      sb.append(quoteBuf);
    
    return sb.toString();
  }
}
