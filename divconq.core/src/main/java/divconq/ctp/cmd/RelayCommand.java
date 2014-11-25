package divconq.ctp.cmd;

import divconq.ctp.CtpConstants;

public class RelayCommand extends BodyCommand {
	public RelayCommand() {
		this.setCmdCode(CtpConstants.CTP_CMD_RELAY);
	}
}
