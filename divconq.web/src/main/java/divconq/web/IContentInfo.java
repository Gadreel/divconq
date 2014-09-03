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
package divconq.web;

import divconq.view.Fragment;
import divconq.view.Nodes;
import divconq.xml.XElement;

public interface IContentInfo {
	void load(ViewInfo info, XElement el);
	Nodes getOutput(Fragment frag, WebContext ctx) throws Exception;
	//Nodes getTemplateOutput(String name, Element parent, Map<String, String> params);
}
