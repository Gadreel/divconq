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
package divconq.script.inst;

import java.util.concurrent.ScheduledFuture;

import divconq.hub.Hub;
import divconq.lang.OperationContext;
import divconq.script.ExecuteState;
import divconq.script.Instruction;
import divconq.script.StackEntry;
import divconq.work.TaskRun;

public class Sleep extends Instruction {
	protected ScheduledFuture<?> future = null;
	
	@Override
	public void run(StackEntry stack) {
		this.future = null;
		
		// TODO support Period or Duration also
		int secs = (int) stack.intFromSource("Seconds");
		OperationContext ctx = OperationContext.get();
		
		TaskRun srun = stack.getActivity().getTaskRun();
		int omin2 = 0;
		
		// if we are inside a task (most of the time, but not for single step debug) we have only 1 minute, what if sleep is longer?
		// don't worry about restoring the timeout, each time we run an instruction it is reset to 1 min
		if (srun != null) {
			omin2 = srun.getTask().getTimeout();
			int pmin = (secs / 60) + 1;  // convert sleep to minutes
			srun.getTask().withTimeout(pmin);		// up the timeout for this instruction
		}
		
		int omin = omin2;
		
		//System.out.println("sleep started");
		
		// do not use, scheduler shuts down and leaves Script thread hanging on the resume, resume must execute even in shut down
		this.future = Hub.instance.getClock().scheduleOnceInternal(() -> {
			//System.out.println("after sleep point");
			Sleep.this.future = null;
			
			// strictly speaking may not be needed, but good idea to ensure you are always working with the correct context
			OperationContext.set(ctx);
			
			if (srun != null) 
				srun.getTask().withTimeout(omin);		// up the timeout for this instruction
			
			stack.setState(ExecuteState.Done);
			stack.resume();
		}, secs);
	}
	
	@Override
	public void cancel(StackEntry stack) {
		System.out.println("sleep cancelled");
		
		// avoid race condition
		ScheduledFuture<?> lf = this.future;
		
		if (lf != null) {
			lf.cancel(false);
			this.future = null;
		}
	}
}
