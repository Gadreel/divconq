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
package divconq.ctp;

import divconq.ctp.cmd.SimpleCommand;
import io.netty.buffer.ByteBuf;

abstract public class CtpCommand {
	static public final CtpCommand EXIT_NO_SIGN_OUT = new SimpleCommand(CtpConstants.CTP_CMD_EXIT);
	static public final CtpCommand EXIT_SIGN_OUT = new SimpleCommand(CtpConstants.CTP_CMD_EXIT_SIGN_OUT);
	static public final CtpCommand ALIVE = new SimpleCommand(CtpConstants.CTP_CMD_ALIVE);
	
	protected int cmdCode = 0;		// 2 byte
	
	public int getCmdCode() {
		return this.cmdCode;
	}
	
	public void setCmdCode(int v) {
		this.cmdCode = v;
	}
	
	public CtpCommand() {
	}
	
	public CtpCommand(int code) {
		this.cmdCode = code;
	}
	
	abstract public ByteBuf encode() throws Exception;

	abstract public void release();

	abstract public boolean decode(ByteBuf in) throws Exception;
	
	@Override
	public String toString() {
		return "Command Code: " + this.cmdCode;
	}
}
