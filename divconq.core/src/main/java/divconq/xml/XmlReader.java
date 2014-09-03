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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Stack;

import divconq.io.CharSequenceReader;
import divconq.lang.FuncResult;
import divconq.lang.OperationResult;
import divconq.util.StringUtil;

public class XmlReader implements IParseHandler {
	/**
	 * Read and parse xml that is held a string
	 * 
	 * @param xml the string that holds the Xml
	 * @return the root xml element
	 */
	public static FuncResult<XElement> parse(CharSequence xml, boolean keepwhitespace) {
		return new XmlReader(new CharSequenceReader(xml), keepwhitespace).parse();
	}

	/**
	 * Read and parse xml that is pointed to by a reader (utf-8)
	 * 
	 * @param in the reader that holds the Xml
	 * @return the root xml element
	 */
	public static FuncResult<XElement> parse(Reader in, boolean keepwhitespace) {
		return new XmlReader(in, keepwhitespace).parse();
	}

	/**
	 * Read and parse xml that is pointed to by a stream (utf-8)
	 * 
	 * @param in the stream that holds the Xml
	 * @return the root xml element
	 */
	public static FuncResult<XElement> parse(InputStream in, boolean keepwhitespace) {
		return new XmlReader(new InputStreamReader(in), keepwhitespace).parse();
	}

	/**
	 * Read and parse xml that is held in memory (utf-8)
	 * 
	 * @param mem the memory that holds the Xml
	 * @return the root xml element
	 */
	//public static FuncResult<XElement> parse(Memory mem, boolean keepwhitespace) {
	//	return new XmlReader(mem, keepwhitespace).parse();
	//}

	/**
	 * Read and parse an xml file
	 * 
	 * @param fullname the file name to read and parse
	 * @return the root xml element
	 */
	public static FuncResult<XElement> loadFile(String fullname, boolean keepwhitespace) {
		try {
			return new XmlReader(new FileReader(fullname), keepwhitespace).parse();
		} 
		catch (FileNotFoundException x) {
			FuncResult<XElement> res = new FuncResult<XElement>();
			res.exitTr(233, fullname);
			return res;
		}
	}

	/**
	 * Read and parse an xml file
	 * 
	 * @param fl the file to read and parse
	 * @return the root xml element
	 */
	public static FuncResult<XElement> loadFile(File fl, boolean keepwhitespace) {
		try {
			return new XmlReader(new FileReader(fl), keepwhitespace).parse();
		} 
		catch (FileNotFoundException x) {
			FuncResult<XElement> res = new FuncResult<XElement>();
			res.exitTr(233, fl.getPath());
			return res;
		}
	}

	/**
	 * Read and parse an xml file
	 * 
	 * @param fl the file to read and parse
	 * @return the root xml element
	 */
	public static FuncResult<XElement> loadFile(Path fl, boolean keepwhitespace) {
		try {
			return new XmlReader(Files.newBufferedReader(fl), keepwhitespace).parse();
		} 
		catch (FileNotFoundException x) {
			FuncResult<XElement> res = new FuncResult<XElement>();
			res.exitTr(233, fl);
			return res;
		}
		catch (IOException x) {
			FuncResult<XElement> res = new FuncResult<XElement>();
			res.exit(1, "Error loading file: " + fl);
			return res;
		}
	}
	
	// instance
	
	/**
	 * The source of the XML
	 */
	protected Reader input = null;
	/**
	 * The root of the class structure
	 */
	protected XElement top = null;
	/**
	 * The current element being worked on
	 */
	protected XElement element = null;
	/**
	 * Holds all the parent elements of the current element
	 */
	protected Stack<XElement> stack = null;
	
	protected boolean keepwhitespace = false;
	
	/**
	 * Set XML source to be a Reader 
	 * 
	 * @param input the XML source to be parsed
	 */
	public XmlReader(Reader input, boolean keepwhitespace) {
		this.input = input;
		this.keepwhitespace = keepwhitespace;
	}

	/**
	 * Parses the XML and returns the root element.  Comments and PI
	 * will be missing, this is a really basic and lightweight XML utility.
	 * 
	 * @return the root XML element 
	 * @throws XMLParseException
	 *             if an error occurs when parsing the XML
	 * @throws IOException
	 *             if an error occurs when reading from the input source
	 */
	public FuncResult<XElement> parse() {
		FuncResult<XElement> res = new FuncResult<XElement>();
		
		this.top = null;
		this.element = null;
		this.stack = new Stack<XElement>();
		
		OperationResult or = XmlParser.parse(this, this.input);
		
		if (this.top == null)
			res.errorTr(247);
		
		if (or.hasErrors())
			res.copyMessages(or);
		else
			res.setResult(this.top);
		
		return res;
	}

	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see IParseHandler#startDocument()
	 */
	@Override
	public void startDocument(OperationResult or) {
		this.top = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see IParseHandler#endDocument()
	 */
	@Override
	public void endDocument(OperationResult or) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see IParseHandler#startElement(java.lang.String, java.lang.String, java.util.Map, int, int)
	 */
	@Override
	public void startElement(OperationResult or, String tag, Map<String, String> attributes, int line, int col) {
		XElement newElement = new XElement(tag);

		newElement.setLocation(line, col);

		for (Map.Entry<String,String> entry : attributes.entrySet()) 
			newElement.setRawAttribute((String) entry.getKey(),
					(String) entry.getValue());

		if (this.top == null)
			this.top = newElement;
		else
			this.element.add(newElement);
		
		if (this.element != null)
			this.stack.push(this.element);
		
		this.element = newElement;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see IParseHandler#element(java.lang.String, java.lang.String, java.util.Map, int, int)
	 */
	@Override
	public void element(OperationResult or, String tag, Map<String, String> attributes, int line, int col) {
		XElement newElement = new XElement(tag);

		newElement.setLocation(line, col);

		for (Map.Entry<String,String> entry : attributes.entrySet()) 
			newElement.setRawAttribute((String) entry.getKey(),
					(String) entry.getValue());

		if (this.top == null)
			this.top = newElement;
		else
			this.element.add(newElement);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see IParseHandler#endElement(java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(OperationResult or, String tag) {
		if (!this.stack.isEmpty())
			this.element = this.stack.pop();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see IParseHandler#text(java.lang.String, boolean, int, int)
	 */
	@Override
	public void text(OperationResult or, String str, boolean cdata, int line, int col) {
		if (this.element == null)
			return;
		
		if (!this.keepwhitespace) {
			str = str.trim();
			
			// non-cdata text should not keep its extra whitespace
			if (!cdata)
				str = StringUtil.stripWhitespacePerXml(str);
			
			if (StringUtil.isEmpty(str)) 
				return;
		}
		
		XText text = new XText();
		
		if (cdata)
			text.setValue(str, true);
		else
			text.setRawValue(str);
		
		this.element.add(text);
	}
}
