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
package divconq.lang;

import divconq.struct.RecordStruct;

// if you are listening to work, your efforts in "completed" will be in the work context, not yours - be sure to respect that
public interface IOperationObserver {
	// this code must not be async or
	// if it has to be then it must start another task to complete that work
	void log(OperationResult or, RecordStruct entry);
	void boundary(OperationResult or, String... tags);
	void step(OperationResult or, int num, int of, String name);
	void progress(OperationResult or, String msg);
	void amount(OperationResult or, int v);
}
