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
package divconq.test;

import divconq.struct.CompositeParser;
import divconq.struct.CompositeStruct;
import divconq.xml.XAttribute;
import divconq.xml.XElement;
import divconq.xml.XText;

public class TestXmlBuilder {
	public static void main(String[] args) {
		XElement test1 = new XElement("Record",
				new XElement("Field",
						new XAttribute("Name", "Age1"),
						new XElement("Scalar", new XText("9"))
				),
				new XElement("Field",
						new XAttribute("Name", "Age2"),
						new XElement("Scalar")
				),
				new XElement("Field",
						new XAttribute("Name", "Age3"),
						new XAttribute("Value", "9")
				),
				new XElement("Field",
						new XAttribute("Name", "Age4")
				),
				new XElement("Field",
						new XAttribute("Name", "NickName"),
						new XAttribute("Value", "Freckles")
				),
				new XElement("Field",
						new XAttribute("Name", "Friends"),
						new XElement("List", 
								new XElement("Scalar", new XText("Stacy")),
								new XElement("Scalar", new XText("Lenny")),
								new XElement("Scalar"),
								new XElement("Scalar", new XText("Patty"))								
						)
				)
		);
		
		CompositeStruct res = CompositeParser.parseXml(test1.toString(true));
		
		System.out.print(res.toString());
	}
}
