package divconq.ctp.f;

import divconq.ctp.CtpCommand;
import divconq.ctp.CtpConstants;
import divconq.ctp.cmd.SimpleCommand;

public class CtpFCommand {
	static public final CtpCommand STREAM_ABORT = new SimpleCommand(CtpConstants.CTP_F_CMD_STREAM_ABORT);
	static public final CtpCommand STREAM_FINAL = new SimpleCommand(CtpConstants.CTP_F_CMD_STREAM_FINAL);
	static public final CtpCommand STREAM_READ = new SimpleCommand(CtpConstants.CTP_F_CMD_STREAM_READ);
	static public final CtpCommand STREAM_WRITE = new SimpleCommand(CtpConstants.CTP_F_CMD_STREAM_WRITE);
}
