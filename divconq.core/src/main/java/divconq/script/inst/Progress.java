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

import divconq.lang.op.OperationContext;
import divconq.script.ExecuteState;
import divconq.script.Instruction;
import divconq.script.StackEntry;
import divconq.util.StringUtil;

public class Progress extends Instruction {
	@Override
	public void run(final StackEntry stack) {
		String output = this.source.hasText() ? stack.resolveValue(this.source.getText()).toString() : null;
		long steps = stack.intFromSource("Steps", -1);
		long step = stack.intFromSource("Step", -1);
		long amount = stack.intFromSource("Amount", -1);
		long add = stack.intFromSource("Add", -1);
		String name = stack.stringFromSource("Name");
		
		if (amount >= 0)
			OperationContext.get().setAmountCompleted((int) amount);
		else if (add >= 0)
			OperationContext.get().setAmountCompleted(OperationContext.get().getAmountCompleted() + (int) add);
		
		if ((step >= 0) && StringUtil.isNotEmpty(name))
			OperationContext.get().setCurrentStep((int) step, name);
		
		if (steps >= 0)
			OperationContext.get().setSteps((int) steps);
		
		if (StringUtil.isNotEmpty(output))
			OperationContext.get().setProgressMessage(output);
		
		stack.setState(ExecuteState.Done);
		
		stack.resume();
	}
	
	@Override
	public void cancel(StackEntry stack) {
		// do nothing, this isn't cancellable
	}
}
