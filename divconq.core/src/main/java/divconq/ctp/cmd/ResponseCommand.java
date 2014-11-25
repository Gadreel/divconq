package divconq.ctp.cmd;

import divconq.ctp.CtpConstants;
import divconq.struct.RecordStruct;

public class ResponseCommand extends BodyCommand {
	public RecordStruct getResult() {
		return this.body;
	}

	public ResponseCommand() {
		this.setCmdCode(CtpConstants.CTP_CMD_RESPONSE);
	}
}
