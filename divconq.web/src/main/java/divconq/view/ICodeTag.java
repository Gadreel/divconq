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
package divconq.view;

import divconq.web.ViewInfo;
import divconq.xml.XElement;

public interface ICodeTag {
	void parseElement(ViewInfo view, Nodes nodes, XElement xel);
}
