package divconq.ctp.cmd;

import divconq.ctp.CtpConstants;

public class RequestCommand extends BodyCommand {
	public RequestCommand() {
		this.setCmdCode(CtpConstants.CTP_CMD_REQUEST);
	}
}
