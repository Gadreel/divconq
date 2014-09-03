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
package divconq.mod;

import divconq.lang.OperationResult;
import divconq.xml.XElement;

public interface IModule {
	void setLoader(ModuleLoader v);	
	ModuleLoader getLoader();
	void init(OperationResult log, XElement config);
	long startTime();
	void start(OperationResult log);
	void stop(OperationResult log); 
}
