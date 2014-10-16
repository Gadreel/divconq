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

import divconq.interchange.CommonPath;
import divconq.io.FileStoreEvent;
import divconq.lang.FuncResult;
import divconq.lang.OperationResult;
import divconq.view.Nodes;
import divconq.xml.XElement;

public interface IWebDomain {
	IWebExtension getExtension();
	String getId();
	
	void init(IWebExtension ext, String id);
	
	void fileNotify(FileStoreEvent result);
	void siteNotify();
	//void load(RecordStruct site);
	OperationResult execute(WebContext ctx);
	
	// TODO re-organize this class interface
	FuncResult<ViewInfo> getView(WebContext ctx, CommonPath path, String type);
	Class<? extends IViewBuilder> getBuilder(String format);
	Class<? extends IContentInfo> getContentLoader(String fmt);
	//IViewParser getFormatParser(String fmt);
    Nodes parseXml(String format, ViewInfo view, XElement container);
    Nodes parseElement(String format, ViewInfo view, XElement xel);
    void parseElement(String format, ViewInfo view, Nodes nodes, XElement xel);
}
