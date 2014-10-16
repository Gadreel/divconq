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
import java.io.InputStream;

import divconq.lang.Memory;

public class InputWrapper extends InputStream {
	protected Memory mem = null;
	
	public InputWrapper(Memory mem) {
		this.mem = new Memory(mem);
	}

	@Override
	public int read() throws IOException {
		return this.mem.readByte();
	}
}
