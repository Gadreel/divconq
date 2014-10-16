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

// just toss out the calls, useful only for subclassing
abstract public class OperationObserver implements IOperationObserver {
	@Override
	public void log(OperationResult or, RecordStruct entry) {
	}

	@Override
	public void boundary(OperationResult or, String... tags) {
	}

	@Override
	public void step(OperationResult or, int num, int of, String name) {
	}

	@Override
	public void progress(OperationResult or, String msg) {
	}

	@Override
	public void amount(OperationResult or, int v) {
	}
}
