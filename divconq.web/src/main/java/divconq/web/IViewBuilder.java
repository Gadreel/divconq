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


public interface IViewBuilder {
	WebContext getContext();
	ViewInfo getView();
	void setViewInfo(ViewInfo v);
	void execute(WebContext ctx) throws Exception;
}
