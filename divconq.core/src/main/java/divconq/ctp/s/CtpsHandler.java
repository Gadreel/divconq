package divconq.ctp.s;

import divconq.ctp.CtpAdapter;
import divconq.ctp.CtpCommand;
import divconq.ctp.ICommandHandler;
import divconq.ctp.cmd.EngageCommand;
import divconq.ctp.cmd.ResponseCommand;
import divconq.hub.Hub;
import divconq.lang.op.OperationContext;
import divconq.work.IWork;
import divconq.work.Task;
import divconq.work.TaskRun;

public class CtpsHandler implements ICommandHandler {

	@Override
	public void handle(CtpCommand cmd, CtpAdapter adapter) throws Exception {
		OperationContext ctx = OperationContext.get();
		
		ctx.touch();
		
		System.out.println("Ctp-S Server got command: " + cmd.getCmdCode());
		
		if (cmd instanceof EngageCommand) {
			if (ctx != null) {
				// put the call back into the work pool, don't tie up the IO thread 
				Task t = new Task()
					.withContext(ctx.subContext())
					.withWork(new IWork() {
						@Override
						public void run(TaskRun trun) {
      						try {
      							adapter.sendCommand(new ResponseCommand());
							}
							catch (Exception x) {
								System.out.println("Ctp-S Server error: " + x);
							}
							
							adapter.read();
							
							trun.complete();
						}
					});
				
				Hub.instance.getWorkPool().submit(t);
			}
			
			return;
		}
	}

	@Override
	public void close() {
		System.out.println("Ctp-S Server Connection closed");
	}
}
