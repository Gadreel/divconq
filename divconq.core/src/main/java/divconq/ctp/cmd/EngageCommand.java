package divconq.ctp.cmd;

import divconq.ctp.CtpConstants;

public class EngageCommand extends BodyCommand {
	public EngageCommand() {
		this.setCmdCode(CtpConstants.CTP_CMD_ENGAGE);
	}
}
