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
package divconq.ctp.cmd;

import io.netty.buffer.ByteBuf;
import divconq.ctp.CtpCommand;
import divconq.hub.Hub;

// commands with no additional fields
public class SimpleCommand extends CtpCommand {
	public SimpleCommand() {		
	}
	
	public SimpleCommand(int code) {
		super(code);
	}
	
	@Override
	public ByteBuf encode() {
		int size = 1;  // type
		
		ByteBuf bb = Hub.instance.getBufferAllocator().buffer(size);
		
		bb.writeByte(this.cmdCode);
		
		return bb;
	}

	@Override
	public void release() {
		// na
	}

	@Override
	public boolean decode(ByteBuf in) {
		return true;
	}
}
