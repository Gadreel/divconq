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
	@Override
	public void run(StackEntry stack) {
		stack.getStore().removeField("Future");
		
		// TODO support Period or Duration also
		int secs = (int) stack.intFromSource("Seconds");
		OperationContext ctx = OperationContext.get();
		
		TaskRun srun = stack.getActivity().getTaskRun();
		int omin2 = 0;
		
		// if we are inside a task we have only 1 minute, what if sleep is longer?
		// automatically change timeout for the instruction
		if (srun != null) {
			omin2 = srun.getTask().getTimeout();
			int pmin = (secs / 60) + 1;  // convert sleep to minutes
			srun.getTask().withTimeout(pmin);		// up the timeout for this instruction
		}
		
		int omin = omin2;
		
		// TODO review if shutdown leaves Script thread hanging and uncanceled if sleep is long
		stack.getStore().setField("Future", Hub.instance.getClock().scheduleOnceInternal(() -> {
			//System.out.println("after sleep point");
			stack.getStore().removeField("Future");
			
			// ensure we are working with the correct context during resume 
			OperationContext.set(ctx);
			
			if (srun != null) 
				srun.getTask().withTimeout(omin);		// restore the original timeout 
			
			stack.setState(ExecuteState.Done);
			stack.resume();
		}, secs));
	}
	
	@Override
	public void cancel(StackEntry stack) {
		//System.out.println("sleep cancelled");
		
		// avoid race condition
		ScheduledFuture<?> lf = (ScheduledFuture<?>) stack.getStore().getFieldAsAny("Future");
		
		if (lf != null) {
			lf.cancel(false);
			
			stack.getStore().removeField("Future");
		}
	}
}
