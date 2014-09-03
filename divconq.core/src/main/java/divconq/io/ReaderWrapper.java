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
import java.io.Reader;

public class ReaderWrapper implements IReader {
	protected Reader rdr = null;
	
	public ReaderWrapper(Reader rdr) {
		this.rdr = rdr;
	}
	
	@Override
	public int readChar() {
		if (this.rdr == null)
			return -1;
		
		try {
			return this.rdr.read();
		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
		}
		
		return -1;
	}
	
	@Override
	public void close() {
		try {
			this.rdr.close();
		} 
		catch (IOException x) {
		}
	}
}
