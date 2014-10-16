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
package divconq.io;

import java.io.IOException;
import java.io.OutputStream;

import divconq.lang.Memory;

public class OutputWrapper extends OutputStream {
	protected Memory mem = null;
	
	public OutputWrapper() {
		this.mem = new Memory();
	}
	
	public OutputWrapper(Memory mem) {
		this.mem = mem;
	}
	
	@Override
	public void write(int value) throws IOException {
		this.mem.writeByte((byte)value);
	}
}
