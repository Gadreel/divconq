package divconq.web;

import divconq.lang.op.OperationResult;

public interface IOutputAdapter {
	OperationResult execute(WebContext ctx) throws Exception;
}
