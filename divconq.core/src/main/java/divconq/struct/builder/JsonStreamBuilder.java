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
package divconq.struct.builder;

import java.io.PrintStream;

public class JsonStreamBuilder extends JsonBuilder {
	protected PrintStream pw = null;
	
	public JsonStreamBuilder(PrintStream pw) {
		super(false);
		this.pw = pw;
	}
	
	public JsonStreamBuilder(PrintStream pw, boolean pretty) {
		super(pretty);
		this.pw = pw;
	}

	public void write(String v) {
		this.pw.append(v);
	}

	public void writeChar(char v) {
		this.pw.append(v);
	}	
}
