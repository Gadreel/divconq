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

public class ClassicIterableAdapter<T> implements IAsyncIterable<T> {
	protected Iterable<T> classic = null;
	
	public ClassicIterableAdapter(Iterable<T> classic) {
		this.classic = classic;
	}

	@Override
	public IAsyncIterator<T> iterator() {
		Iterator<T> it = this.classic.iterator();
		
		return new ClassicIteratorAdapter<T>(it);
	}

}
