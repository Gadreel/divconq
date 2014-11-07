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
package divconq.util;

import java.util.Iterator;

import divconq.lang.op.FuncCallback;

public class ClassicIteratorAdapter<T> implements IAsyncIterator<T> {
	protected Iterator<T> classic = null;

	public ClassicIteratorAdapter(Iterator<T> iterator) {
		this.classic = iterator;
	}
	
	@Override
	public void hasNext(FuncCallback<Boolean> callback) {
		callback.setResult(this.classic.hasNext());
		callback.complete();
	}

	@Override
	public void next(FuncCallback<T> callback) {
		callback.setResult(this.classic.next());
		callback.complete();
	}
}
