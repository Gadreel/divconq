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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import divconq.lang.Memory;
import divconq.lang.op.FuncResult;
import divconq.struct.CompositeStruct;
import divconq.struct.CompositeParser;
import divconq.util.StringUtil;

/**
 * Represents a XML element and contains all the information that the
 * original had. Attributes may be set, accessed and deleted. Child elements may
 * be set accessed or deleted. This can be converted back to XML, along with its
 * children, in a formatted or unformatted string.
 */
public class XElement extends XNode {
	protected String tagName = null;
	protected int line = 0;
	protected int col = 0;

	protected Map<String, String> attributes = null;
	protected List<XNode> children = null;
	protected String comment = null;

	/**
	 * constructor specifying the name with an optional array
	 * of objects to be added as child elements
	 * 
	 * @param tag the name of the tag
	 * @param children
	 *            an array of objects to be added as children
	 */
	public XElement(String tag, Object... children) {
		this.tagName = tag;

		for (int i = 0; i < children.length; i++) {
			Object obj = children[i];

			if (obj instanceof XNode)
				this.add((XNode) obj);
			else if (obj instanceof XAttribute)
				this.setAttribute(((XAttribute) obj).getName(), ((XAttribute) obj).getRawValue());
			else
				this.add(obj.toString());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	/*
	 * TODO public Object clone() throws CloneNotSupportedException {
	 * TaggedElement newElement = (TaggedElement)super.clone(); if (attributes
	 * != null) { newElement.attributes = new
	 * HashMap<String,String>(attributes); } if (elements != null) {
	 * newElement.elements = new ArrayList<Element>(); Iterator<Element> it =
	 * elements.iterator(); while (it.hasNext()) {
	 * newElement.elements.add((Element)it.next().clone()); } } return
	 * newElement; }
	 */

	public XElement(XMLStreamReader xmlStreamReader, boolean keepwhitespace) {
		this.tagName = xmlStreamReader.getLocalName();
		
		 for (int a = 0; a < xmlStreamReader.getAttributeCount(); a++) 
			 this.setAttribute(xmlStreamReader.getAttributeLocalName(a), xmlStreamReader.getAttributeValue(a));
		
		 try {
			while (xmlStreamReader.hasNext()) {
				 int n = xmlStreamReader.next();
			        
				 switch (n) {
				 case XMLStreamConstants.START_ELEMENT:
					 this.add(new XElement(xmlStreamReader, keepwhitespace));					 
					 break;
				 case XMLStreamConstants.END_ELEMENT:
					 return;
				 case XMLStreamConstants.SPACE:
				 case XMLStreamConstants.CHARACTERS:
					 String str = xmlStreamReader.getText();
					 
					if (!keepwhitespace) {
						str = StringUtil.stripWhitespacePerXml(str);
					}

					// this is not always good - see if we can do it anyway
					if (StringUtil.isEmpty(str)) 
						break;
					
					XText text = new XText();
					
					text.setRawValue(str);
					
					this.add(text);
						
					 break;
					 /*
				 case XMLStreamConstants.ATTRIBUTE:
					 for (int a = 0; a < xmlStreamReader.getAttributeCount(); a++) {
						 this.setAttribute(xmlStreamReader.getAttributeLocalName(a), xmlStreamReader.getAttributeValue(a));
					 }
					 break;
					 */
				 case XMLStreamConstants.CDATA:
					 String str2 = xmlStreamReader.getText();
					 
						if (!keepwhitespace) {
							str2 = str2.trim();
							
							if (StringUtil.isEmpty(str2)) 
								break;
						}
						
						XText text2 = new XText();
						
						text2.setValue(str2, true);
						
						this.add(text2);
					 break;
				 }
			 }
		} 
		 catch (XMLStreamException x) {
			// TODO Auto-generated catch block
			x.printStackTrace();
		}
	}

	/**
	 * gets the element name 
	 * 
	 * @return the element name 
	 */
	public String getName() {
		return this.tagName;
	}

	/**
	 * sets an attribute of this element
	 * 
	 * @param name
	 *            the name of the attribute to be set
	 * @param value
	 *            the value of the attribute to be set
	 */
	public void setAttribute(String name, String value) {
		if (this.attributes == null)
			this.attributes = new HashMap<String, String>();

		this.attributes.put(name, XNode.quote(value));
	}
	
	public void setRawAttribute(String name, String value) {
		if (this.attributes == null)
			this.attributes = new HashMap<String, String>();

		this.attributes.put(name, value);
	}
	
	public XElement withAttribute(String name, String value) {
		if (this.attributes == null)
			this.attributes = new HashMap<String, String>();

		this.attributes.put(name, XNode.quote(value));
		
		return this;
	}
	

	/**
	 * gets the specified attribute of this element
	 * 
	 * @param name
	 *            the name of the attribute to get
	 * @return the value of the attribute
	 */
	public String getAttribute(String name) {
		return (this.attributes == null ? null : XNode.unquote(this.attributes.get(name)));
	}

	public String getRawAttribute(String name) {
		//return (this.attributes == null ? null : XNode.unquote(this.attributes.get(name)));
		return (this.attributes == null ? null : this.attributes.get(name));
	}
	
	/**
	 * gets the specified attribute of this element but returns given default
	 * value if the attribute does not exist
	 * 
	 * @param name
	 *            the name of the attribute to get
	 * @param defaultValue
	 *            the value to be returned if the attribute doesn't exist
	 * @return the value of the attribute
	 */
	public String getAttribute(String name, String defaultValue) {
		String result = this.getAttribute(name);
		return result == null ? defaultValue : result;
	}

	/**
	 * finds out whether an attribute exists
	 * 
	 * @param name
	 *            the name of the attribute to look for
	 * @return whether the attribute exists in this element
	 */
	public boolean hasAttribute(String name) {
		return this.attributes == null ? false : this.attributes.containsKey(name);
	}

	/**
	 * finds out whether this element has any attributes
	 * 
	 * @return whether this element has any attributes
	 */
	public boolean hasAttributes() {
		return this.attributes != null && !this.attributes.isEmpty();
	}

	/**
	 * gets the attributes in the form of an indexed table
	 * 
	 * @return the attribute table for this element
	 */
	public Map<String, String> getAttributes() {
		if (this.attributes == null)
			this.attributes = new HashMap<String, String>();

		return this.attributes;
	}

	/**
	 * removes the named attribute
	 *  
	 * @param name Name of attribute to remove
	 */
	public void removeAttribute(String name) {
		if (this.attributes != null)
			this.attributes.remove(name);
	}

	/**
	 * gets the number of child elements this element has
	 * 
	 * @return the number of child elements this element has
	 */
	public int children() {
		return this.children == null ? 0 : this.children.size();
	}

	/**
	 * finds out whether this element has any child elements
	 * 
	 * @return whether this element has any child elements
	 */
	public boolean hasChildren() {
		return this.children() != 0;
	}

	/**
	 * removes the children from this element
	 * 
	 */
	public void clearChildren() {
		if (children != null)
			children.clear();
	}

	/**
	 * adds a child to the end of this element
	 * 
	 * @param element
	 *            the child element to be added
	 */
	public void add(XNode element) {
		this.add(-1, element);
	}
	
	public XElement with(XNode v) {
		this.add(-1, v);
		
		return this;
	}

	/**
	 * inserts a child into this element. If the index is out of range, the
	 * child is added at the end
	 * 
	 * @param index
	 *            the location to insert the child
	 * @param element
	 *            the child element to be added
	 */
	public void add(int index, XNode element) {
		if (this.children == null)
			this.children = new ArrayList<XNode>();

		if ((index < 0) || (index >= this.children.size()))
			this.children.add(element);
		else
			this.children.add(index, element);
	}

	/**
	 * adds a child to the end of this element
	 * 
	 * @param string
	 *            the child element to be added
	 */
	public void add(String string) {
		this.add(new XText(string));
	}
	
	public XElement withText(String v) {
		this.add(new XText(v));
		
		return this;
	}
	
	public XElement withCData(String v) {
		this.add(new XText(true, v));
		
		return this;
	}

	/**
	 * inserts a child into this element If the index is out of range, the child
	 * is added at the end
	 * 
	 * @param index
	 *            the location to insert the child
	 * @param string
	 *            the child element to be added
	 */
	public void add(int index, String string) {
		this.add(index, new XText(string));
	}

	/**
	 * gets a child from the specified place in this element
	 * 
	 * @param i
	 *            the index where the child is to be added
	 * @return the specified child
	 */
	public XNode getChild(int i) {
		if ((i < 0) || (i >= this.children()))
			return null;

		return this.children.get(i);
	}

	/**
	 * replaces a child element with the one given
	 * 
	 * @param index
	 *            the child element number to replace
	 * @param newElement
	 *            the new element to replace the old one
	 */
	public void replace(int index, XNode newElement) {
		if (this.children == null)
			this.children = new ArrayList<XNode>();
		
		if (index >= this.children.size())
			this.children.add(newElement);
		else
			this.children.set(index, newElement);
	}

	/**
	 * replaces all children with the children of the provided element
	 * 
	 * @param source
	 *       		the new element providing the source of the children
	 */
	public void replaceChildren(XElement source) {
		this.children = new ArrayList<XNode>();

		if (source.hasChildren())
			this.children.addAll(source.children);
	}
	
	public void replaceAttributes(XElement source) {
		this.attributes = new HashMap<String, String>();

		if (source.hasAttributes()) 
			this.attributes.putAll(source.attributes);
	}
	
	public void replace(XElement source) {
		this.tagName = source.tagName;
		this.comment = source.comment;
		this.replaceChildren(source);
		this.replaceAttributes(source);
	}

	/**
	 * Removes a child from this element
	 * 
	 * @param index
	 *            the index of the child to be removed
	 * @return whether the child was found and removed or not
	 */
	public XNode remove(int index) {
		XNode result = null;
		
		if (this.children == null)
			this.children = new ArrayList<XNode>();
		
		if (index >= 0 && index < this.children.size())
			result = this.children.remove(index);
		
		return result;
	}

	/**
	 * Removes a child from this element
	 * 
	 * @param element
	 *            the child to be removed
	 * @return whether the child was found and removed or not
	 */
	public boolean remove(XNode element) {
		if (element == null)
			return true;
		
		return this.children.remove(element);
	}

	@Override
	public XNode deepCopy() {
		XElement copy = new XElement(this.tagName);
		
		copy.line = this.line;
		copy.col = this.col;
		copy.comment = this.comment;
		
		if (this.attributes != null) {
			copy.attributes = new HashMap<>();
			
			for (Entry<String, String> entry : this.attributes.entrySet()) 
				copy.attributes.put(entry.getKey(), entry.getValue());
		}
		
		if (this.children != null) {
			copy.children = new ArrayList<XNode>();
			
			for (XNode entry : this.children) 
				copy.children.add(entry.deepCopy());
		}
		
		return copy;
	}

	/**
	 * Finds a named child tagged element. If there is no such child, null is
	 * returned.
	 * 
	 * @param name
	 *            the name of the child of this TaggedElement to find
	 * @return the name of the found element or null if not found
	 */
	public XElement find(String... name) {
		if (this.children != null)
			for (int i = 0; i < this.children.size(); i++) {
				XNode element = this.children.get(i);
				
				if (element instanceof XElement) {
					for (int n = 0; n < name.length; n++)
						if (((XElement) element).getName().equals(name[n]))
							return (XElement) element;
				}
			}
		
		return null;
	}

	public XElement findId(String id) {
		if (id == null)
			return null;
		
		if (this.attributes != null) 
			if (id.equals(this.getAttribute("id")) || id.equals(this.getAttribute("Id")) || id.equals(this.getAttribute("ID")))
				return this;
		
		if (this.children != null) {
			for (XNode n : this.children) {
				if (n instanceof XElement) {
					XElement match = ((XElement)n).findId(id);
					
					if (match != null)
						return match;
				}
			}
		}
		
		return null;
	}

	public XElement findParentOfId(String id) {
		return findParentOfId(id, null);
	}

	public XElement findParentOfId(String id, XElement parent) {
		if (id == null)
			return null;
		
		if (this.attributes != null) 
			if (id.equals(this.getAttribute("id")) || id.equals(this.getAttribute("Id")) || id.equals(this.getAttribute("ID")))
				return parent;
		
		if (this.children != null) {
			for (XNode n : this.children) {
				if (n instanceof XElement) {
					XElement match = ((XElement)n).findParentOfId(id, this);
					
					if (match != null)
						return match;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * A way to select child or sub child elements similar to XPath but lightweight.
	 * Cannot select values or attributes, just elements.  * is supported to match
	 * all elements at a given level.
	 * 
	 * For example: "Toys/Toy" called on "&lt;Person&gt;" means select all Toy elements
	 * inside of the Toys child element (child of Person).
	 * 
	 * Cannot go up levels, or back to root.  Do not start with a slash as in "/People".
	 * 
	 * @param path a backslash delimited string
	 * @return list of all matching elements, or empty list if no match
	 */
	public List<XElement> selectAll(String path) {
		List<XElement> matches = new ArrayList<XElement>();
		this.selectAll(path, matches);
		return matches;
	}
	
	/**
	 * Internal, recursive search used by selectAll
	 * 
	 * @param path a backslash delimited string
	 * @param matches list of all matching elements, or empty list if no match
	 */
	protected void selectAll(String path, List<XElement> matches) {
		if (!this.hasChildren())
			return;
		
		int pos = path.indexOf('/');

		// go back to root not supported
		if (pos == 0)
			return;
		
		String name = null;
		
		if (pos == -1) {
			name = path;
			path = null;
		}
		else { 
			name = path.substring(0, pos);
			path = path.substring(pos + 1);
		}
		
		// TODO add filter per XPath - [@n = f]
		
		for (XNode n : this.children) {
			if (n instanceof XElement) {
				if ("*".equals(name) || ((XElement)n).getName().equals(name)) {
					if (pos == -1) 
						matches.add((XElement)n);
					else  
						((XElement)n).selectAll(path, matches);
				}
			}
		}
	}
	
	/**
	 * A way to select text of a  child or sub child elements similar to XPath but lightweight.
	 * '*' is supported to match
	 * all elements at a given level.  Returns only the first match.
	 * 
	 * For example: "Toys/Toy" called on "&lt;Person&gt;" means return the text of first Toy element
	 * inside of the Toys child element (child of Person).
	 * 
	 * Cannot go up levels, or back to root.  Do not start with a slash as in "/People".
	 * 
	 * @param path a backslash delimited string
	 * @return text of first matching element, or null if no match
	 */
	public String selectFirstText(String path) {
		XElement first = this.selectFirst(path);
		
		if (first != null)
			return first.getText();
		
		return null;
	}
	
	/**
	 * A way to select text of a  child or sub child elements similar to XPath but lightweight.
	 * '*' is supported to match
	 * all elements at a given level.  Returns only the first match.
	 * 
	 * For example: "Toys/Toy" called on "&lt;Person&gt;" means return the text of first Toy element
	 * inside of the Toys child element (child of Person).
	 * 
	 * Cannot go up levels, or back to root.  Do not start with a slash as in "/People".
	 * 
	 * @param path a backslash delimited string
	 * @param def default text if none found
	 * @return text of first matching element, or null if no match
	 */
	public String selectFirstText(String path, String def) {
		XElement first = this.selectFirst(path);
		
		if (first != null) {
			String t = first.getText();
			
			if (StringUtil.isNotEmpty(t))
				return t;
		}
		
		return def;
	}
	
	/**
	 * A way to select text of a  child or sub child elements similar to XPath but lightweight.
	 * '*' is supported to match
	 * all elements at a given level.  Returns only the first match.
	 * 
	 * For example: "Toys/Toy" called on "&lt;Person&gt;" means return the text of first Toy element
	 * inside of the Toys child element (child of Person).
	 * 
	 * Cannot go up levels, or back to root.  Do not start with a slash as in "/People".
	 * 
	 * @param path a backslash delimited string
	 * @param def default object to return if path not found
	 * @return text of first matching element, or null if no match
	 */
	public Object selectFirstValue(String path, Object def) {
		XElement first = this.selectFirst(path);
		
		if (first != null) {
			String t = first.getText();
			
			if (StringUtil.isNotEmpty(t))
				return t;
		}
		
		return def;
	}
	
	/**
	 * A way to select child or sub child elements similar to XPath but lightweight.
	 * Cannot select values or attributes, just elements.  * is supported to match
	 * all elements at a given level.  Returns only the first match.
	 * 
	 * For example: "Toys/Toy" called on "&lt;Person&gt;" means return first Toy element
	 * inside of the Toys child element (child of Person).
	 * 
	 * Cannot go up levels, or back to root.  Do not start with a slash as in "/People".
	 * 
	 * @param path a backslash delimited string
	 * @return first matching element, or null if no match
	 */
	public XElement selectFirst(String path) {
		if (!this.hasChildren())
			return null;
		
		int pos = path.indexOf('/');

		// go back to root not supported
		if (pos == 0)
			return null;
		
		String name = null;
		
		if (pos == -1) {
			name = path;
			path = null;
		}
		else { 
			name = path.substring(0, pos);
			path = path.substring(pos + 1);
		}
		
		// TODO add filter per XPath - [@n = f]
		
		for (XNode n : this.children) {
			if (n instanceof XElement) {
				if ("*".equals(name) || ((XElement)n).getName().equals(name)) {
					if (pos == -1) 
						return (XElement)n;
					else  {
						XElement r = ((XElement)n).selectFirst(path);
						
						if (r != null)
							return r;
					}
				}
			}
		}
		
		return null;
	}

	/**
	 * Finds the index of a named child tagged element. If there is no such
	 * child, -1 is returned.
	 * 
	 * @param name
	 *            the name of the child of this TaggedElement to find
	 * @return the index of the found element or -1 if not found
	 */
	public int findIndex(String name) {
		if (this.children != null)
			for (int i = 0; i < this.children.size(); i++) {
				XNode node = this.children.get(i);
				
				if (node instanceof XElement) 
					if (((XElement) node).getName().equals(name))
						return i;
			}
		
		return -1;
	}

	/**
	 * sets the list of children of this element. This method replaces the
	 * current children.
	 * 
	 * @param elements
	 *            the new list of children
	 */
	public void setElements(List<XNode> elements) {
		this.children = elements;
	}

	/**
	 * gets a list of the children of this element. This method always returns a
	 * List even if it is empty.
	 * 
	 * @return the List containing the children of this element
	 */
	public Collection<XNode> getChildren() {
		if (this.children == null)
			this.children = new ArrayList<XNode>();
		
		return this.children;
	}

	/**
	 * sets the XML source code location information for this element
	 * 
	 * @param line
	 *            the line number of the start tag
	 * @param col
	 *            the column number of the start tag
	 */
	public void setLocation(int line, int col) {
		this.line = line;
		this.col = col;
	}

	/**
	 * gets the XML source code line number where this element was declared.
	 * This number will be 0 if it was never set.
	 * 
	 * @return the XML source code line number of this element's declaration
	 */
	public int getLine() {
		return this.line;
	}

	/**
	 * gets the XML source code cloumn number where this element was declared.
	 * This number will be 0 if it was never set.
	 * 
	 * @return the XML source code column number of this element's declaration
	 */
	public int getCol() {
		return this.col;
	}

	/**
	 * get the comment associated with this element
	 * 
	 * @return this element's comment
	 */
	public String getComment() {
		return this.comment;
	}

	/**
	 * set this element's comment
	 * 
	 * @param comment
	 *            the comment to be stored
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	/**
	 * 
	 * @return the text contained in this element if any, else null
	 */
	public String getText() {
		if (!this.hasChildren())
			return null;
		
		// TODO improve to support multiple CDATA sections in one element
		XNode f = this.children.get(0);
		
		if (f instanceof XText)
			return ((XText)f).getValue();
		
		return null;
	}

	public boolean hasText() {
		if (!this.hasChildren())
			return false;
		
		// TODO improve to support multiple CDATA sections in one element
		XNode f = this.children.get(0);
		
		if (f instanceof XText)
			return true;
		
		return false;
	}
	
	/**
	 * assumes that the text content of this element is escaped xml,
	 * reads and parses the text content and returns the root element
	 * from that content
	 * 
	 * @param keepwhitespace don't strip white space when parsing the content of element
	 * @return root xml element for parsed content or null
	 */
	public FuncResult<XElement> toXml(boolean keepwhitespace) {
		if (!this.hasChildren()) {
			FuncResult<XElement> res = new FuncResult<XElement>();
			res.errorTr(244);
			return res;
		}
		
		XNode f = this.children.get(0);
		
		if (f instanceof XText)
			return XmlReader.parse(((XText)f).getValue(), keepwhitespace);
		
		FuncResult<XElement> res = new FuncResult<XElement>();
		res.errorTr(245);
		return res;
	}

	/**
	 * assumes the text content of this element is Json, reads and
	 * parses the text content
	 * 
	 * @return struct or null
	 */
	public CompositeStruct toStruct() {
		if (!this.hasChildren())
			return null;
		
		XNode f = this.children.get(0);
		
		if (f instanceof XText)
			return CompositeParser.parseJson(((XText)f).getValue()).getResult();
		
		return null;
	}

	/**
	 * 
	 * @return the first child node or null
	 */
	public XNode getFirstChild() {
		if (this.hasChildren())
			return this.children.get(0);
		
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.toString(true);
	}

	/**
	 * returns just the tag start, not the content or children or the tag end.  
	 * useful for debugging
	 * 
	 * @return tag start in xml syntax
	 */
	public String toLocalString() {
		StringBuffer sb = new StringBuffer();

		// Put the opening tag out
		sb.append("<" + this.tagName);

		// Write the attributes out
		if (this.attributes != null) 
			for (Map.Entry<String, String> entry : this.attributes.entrySet()) {
				sb.append(" " + entry.getKey() + "=");
				sb.append("\"" + entry.getValue() + "\"");
			}

		sb.append(">");
		
		return sb.toString();
	}
	
	public String toInnerString() {
		return this.toInnerString(true);
	}
	
	public String toInnerString(boolean formatted) {
	    StringBuffer sb = new StringBuffer();

		// write out the closing tag or other elements
		boolean formatThis = formatted;
		
		for (XNode element : this.children) {
			formatThis = (element instanceof XText) ? false : formatted;
			element.toString(sb, formatThis, 1);
		}
		
	    return sb.toString();
	}

	/* (non-Javadoc)
	 * @see divconq.xml.XNode#toString(java.lang.StringBuffer, boolean, int)
	 */
	@Override
	protected StringBuffer toString(StringBuffer sb, boolean formatted, int level) {
		// Add leading newline and spaces, if necessary
		if (formatted && level > 0) {
			sb.append("\n");
			for (int i = level; i > 0; i--)
				sb.append("\t");
		}

		// Put the opening tag out
		sb.append("<" + this.tagName);

		// Write the attributes out
		if (this.attributes != null) 
			for (Map.Entry<String, String> entry : this.attributes.entrySet()) {
				sb.append(" " + entry.getKey() + "=");
				sb.append("\"" + entry.getValue() + "\"");
			}

		// write out the closing tag or other elements
		boolean formatThis = formatted;
		boolean nontext = false;
		
		if (!this.hasChildren()) {
			sb.append(" /> ");
		} 
		else {
			sb.append(">");
			
			for (XNode node : this.children) {
				formatThis = (node instanceof XText) ? false : formatted;
				
				if (!(node instanceof XText))
					nontext = true;

				node.toString(sb, formatThis, level + 1);
			}
			
			// Add leading newline and spaces, if necessary
			if (formatThis || nontext) {
				sb.append("\n");
				
				for (int i = level; i > 0; i--)
					sb.append("\t");
			}
			
			// Now put the closing tag out
			sb.append("</" + this.tagName + "> ");
		}
		
		return sb;
	}
	
	/* (non-Javadoc)
	 * @see divconq.xml.XNode#toMemory(divconq.lang.Memory, boolean, int)
	 */
	@Override
	protected void toMemory(Memory sb, boolean formatted, int level) {
		// Add leading newline and spaces, if necessary
		if (formatted && level > 0) {
			sb.write("\n");
			for (int i = level; i > 0; i--)
				sb.write("\t");
		}

		// Put the opening tag out
		sb.write("<" + this.tagName);

		// Write the attributes out
		if (this.attributes != null) 
			for (Map.Entry<String, String> entry : this.attributes.entrySet()) {
				sb.write(" " + entry.getKey() + "=");
				sb.write("\"" + entry.getValue() + "\"");
			}

		// write out the closing tag or other elements
		boolean formatThis = formatted;
		boolean nontext = false;
		
		if (!this.hasChildren()) {
			sb.write(" /> ");
		} 
		else {
			sb.write(">");
			
			for (XNode node : this.children) {
				formatThis = (node instanceof XText) ? false : formatted;
				
				if (!(node instanceof XText))
					nontext = true;
				
				node.toMemory(sb, formatThis, level + 1);
			}
			
			// Add leading newline and spaces, if necessary
			if (formatThis || nontext) {
				sb.write("\n");
				
				for (int i = level; i > 0; i--)
					sb.write("\t");
			}
			
			// Now put the closing tag out
			sb.write("</" + this.tagName + "> ");
		}
	}
}
