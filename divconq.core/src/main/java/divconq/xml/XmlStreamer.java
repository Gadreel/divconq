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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import divconq.lang.OperationResult;

public class XmlStreamer {
	static public OperationResult parseStream(String path, int level, IPartHandler handler, boolean keepwhitespace) {
		OperationResult res = new OperationResult();
		
		try {
			// load names
			BufferedReader bReader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
			
			 XMLStreamReader xmlStreamReader = XMLInputFactory.newInstance().createXMLStreamReader(bReader);
			 
			 int clevel = 0;
			 
			 while (xmlStreamReader.hasNext()) {
				 int n = xmlStreamReader.next();
			        
				 switch (n) {
				 case XMLStreamConstants.START_ELEMENT:
					 if (level == clevel) {
						 XElement part = new XElement(xmlStreamReader, keepwhitespace);
						 handler.part(part);
						 //clevel--;
					 }
					 else
						 clevel++;
					 break;
				 case XMLStreamConstants.END_ELEMENT:
					 clevel--;
					 break;
				 }
			 }
			 
			 xmlStreamReader.close();		
		}
		catch (Exception x) {
			System.out.println("error reading xml stream: " + x);
		}
		
		return res;
	}
}
