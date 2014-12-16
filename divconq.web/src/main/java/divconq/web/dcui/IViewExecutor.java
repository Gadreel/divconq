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
package divconq.web.dcui;

import divconq.lang.op.OperationResult;
import divconq.web.WebContext;


public interface IViewExecutor {
	WebContext getContext();
	OperationResult execute(WebContext ctx) throws Exception;
	
	ViewOutputAdapter getView();
	void setViewInfo(ViewOutputAdapter v);
}
