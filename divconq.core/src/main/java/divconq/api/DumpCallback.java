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
package divconq.api;

import divconq.lang.chars.Utf8Encoder;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.Struct;
import divconq.util.HexUtil;

public class DumpCallback extends ServiceResult {
	protected String name = null;

	public DumpCallback(String req) {
		this.name = req;
	}

	@Override
	public void callback() {
		ListStruct msgs = this.getMessages();
		
		if (this.hasErrors()) {
			System.out.println(this.name + " Error: " + this.getCode());
			
			if (msgs != null)
				System.out.println(msgs.toPrettyString());
		}
		else  {
			System.out.println(this.name + " Messages:");
			if (msgs != null)
				System.out.println(msgs.toPrettyString());
			
			System.out.println();
			System.out.println(this.name + " Response:");
			
			Struct body = this.getResult().getField("Body");
			
			if (body instanceof CompositeStruct)
				System.out.println(((CompositeStruct)body).toPrettyString());
			else if (body != null)
				System.out.println("  Value: " + body + "  -- " + HexUtil.bufferToHex(Utf8Encoder.encode(body.toString())));
		}
	}
}