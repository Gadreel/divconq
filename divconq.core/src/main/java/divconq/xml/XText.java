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
 * An XML element that simply contains text. This is normally a child element of
 * {@link XElement}.
 */
public class XText extends XNode {
	/**
	 * The value of the TextElement.
	 */
	protected String content = null;
	protected boolean cdata = false;
	
	public XText() {
	}

	/**
	 * @param string text content to associate with this node
	 */
	public XText(String string) {
		this.setValue(string);
	}

	public void append(char c) {
		this.content += XNode.quote(c);
	}

	public void append(String s) {
		this.content += XNode.quote(s);
	}

	public void appendEntity(String s) {
		this.content += s;
	}
	
	/**
	 * @param cdata flag to indicate that this text was stored within a CDATA section
	 * @param string text content to associate with this node
	 */
	public XText(boolean cdata, String string) {
		this.setValue(string, cdata);
	}
	
	@Override
	public XNode deepCopy() {
		XText copy = new XText();
		
		copy.cdata = this.cdata;
		copy.content = this.content;
		
		return copy;
	}
	
	/**
	 * Sets the value of this node
	 * 
	 * @param value the value to store 
	 */
	public void setValue(String value) {
		this.content = XNode.quote(value);
		this.cdata = false;
	}
	
	/*
	 * Sets the value of this node
	 * 
	 * @param value the value to store 
	 */
	public void setValue(String value, boolean cdata) {
		if (cdata)
			this.content = value;
		else
			this.content = XNode.quote(value);
		
		this.cdata = cdata;
	}

	public void setRawValue(String str) {
		this.content = str;
		this.cdata = false;
	}

	/**
	 * 
	 * @return the value of this node
	 */
	public String getValue() {
		if (cdata)
			return this.content;
		
		return XNode.unquote(this.content);
	}
	
	public String getRawValue() {
		return this.content;
	}
	
	/**
	 * Sets this element to print as a CDATA section
	 * 
	 * @param cdata true if it should be printed as a CDATA section
	 */
	public void setCData(boolean cdata) {
		this.cdata = cdata;
	}

	/**
	 * @return true if this text is part of a CDATA section
	 */
	public boolean getCData() {
		return this.cdata;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (this.cdata) {
			return "<![CDATA[" + this.content + "]]>";

			/*  TODO fix to support ]]> in content
			 * 
			 * You do not escape the ]]> but you escape the > after ]] by inserting ]]><![CDATA[ before the >, think 
			 * of this just like a \ in C/Java/PHP/Perl string but only needed before a > and after a ]].
			 */
			
			
			//return this.cdataToString(this.content).toString();
		}
		
		return this.content;
	}

	/* (non-Javadoc)
	 * @see divconq.xml.XNode#toString(java.lang.StringBuffer, boolean, int)
	 */
	@Override
	protected StringBuffer toString(StringBuffer sb, boolean formatted, int level) {
		if (formatted) {
			sb.append("\n");
			for (int i = level; i > 0; i--)
				sb.append("\t");
		}
		
		if (this.cdata) {
			sb.append("<![CDATA[");
			sb.append(this.content);
			sb.append("]]>");

			/*  TODO fix to support ]]> in content
			 * 
			 * You do not escape the ]]> but you escape the > after ]] by inserting ]]><![CDATA[ before the >, think 
			 * of this just like a \ in C/Java/PHP/Perl string but only needed before a > and after a ]].
			 */
		}
		else
			sb.append(this.content);
		
		return sb;
	}

	/* (non-Javadoc)
	 * @see divconq.xml.XNode#toMemory(divconq.lang.Memory, boolean, int)
	 */
	@Override
	protected void toMemory(Memory sb, boolean formatted, int level) {
		if (formatted) {
			sb.write("\n");
			for (int i = level; i > 0; i--)
				sb.write("\t");
		}
		
		if (this.cdata) {
			sb.write("<![CDATA[");
			sb.write(this.content);

			/*  TODO fix to support ]]> in content
			 * 
			 * You do not escape the ]]> but you escape the > after ]] by inserting ]]><![CDATA[ before the >, think 
			 * of this just like a \ in C/Java/PHP/Perl string but only needed before a > and after a ]].
			 * 
			 * 
			int index = 0;		
			
			while ((index >= 0) && (index <= sb.length())) {
				index = sb.indexOf("]]>", index);
				
				if (index < 0)
					break;
				
				index += 3;
				sb.insert(index, "]]&gt;<![CDATA[");
			}
			*/
			
			sb.write("]]>");
		}
		else
			sb.write(XNode.quote(this.content));
	}

	public static XText raw(String v) {
		XText t = new XText();
		t.setRawValue(v);
		return t;
	}

	public static XText cdata(String v) {
		XText t = new XText(true, v);
		return t;
	}

	public static XText escape(String v) {
		XText t = new XText(false, v);
		return t;
	}
}
