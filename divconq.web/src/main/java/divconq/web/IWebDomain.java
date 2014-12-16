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

import divconq.filestore.CommonPath;
import divconq.hub.DomainInfo;
import divconq.locale.LocaleInfo;
import divconq.web.dcui.Nodes;
import divconq.web.dcui.ViewOutputAdapter;
import divconq.xml.XElement;

public interface IWebDomain {
	String getId();
	String getAlias();
    CommonPath getHomePath();
    CommonPath getMainPath();
	
	void init(DomainInfo domain);
	String tr(LocaleInfo locale, String token, Object... params);
	
	void siteNotify();
	void execute(WebContext ctx);
	
	IOutputAdapter findFile(WebContext ctx, CommonPath path);

	// TODO re-organize this class interface
    Nodes parseXml(ViewOutputAdapter view, XElement container);
    Nodes parseElement(ViewOutputAdapter view, XElement xel);
    void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel);
}
