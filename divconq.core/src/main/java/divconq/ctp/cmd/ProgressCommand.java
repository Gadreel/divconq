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
import divconq.ctp.CtpConstants;
import divconq.hub.Hub;

public class ProgressCommand extends CtpCommand {
	protected int amount = 0;
	
	public void setAmount(int v) {
		this.amount = v;
	}
	
	public int getAmount() {
		return this.amount;
	}
	
	public ProgressCommand() {
		super(CtpConstants.CTP_CMD_PROGRESS);
	}
	
	public ProgressCommand(int amt) {
		super(CtpConstants.CTP_CMD_PROGRESS);
		
		this.amount = amt;
	}
	
	@Override
	public ByteBuf encode() {
		int size = 2;  // code + amt
		
		ByteBuf bb = Hub.instance.getBufferAllocator().buffer(size);
		
		bb.writeByte(this.cmdCode);
		bb.writeByte(this.amount);
		
		return bb;
	}

	@Override
	public void release() {
		// na
	}

	@Override
	public boolean decode(ByteBuf in) {
        if (in.readableBytes() < 1) 
            return false;
        
        this.amount = in.readUnsignedByte();
		
		return true;
	}

}
