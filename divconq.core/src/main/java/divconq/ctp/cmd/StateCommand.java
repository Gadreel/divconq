package divconq.ctp.cmd;

import divconq.ctp.CtpConstants;

public class StateCommand extends BodyCommand {
	public StateCommand() {
		this.setCmdCode(CtpConstants.CTP_CMD_STATE);
	}
}
