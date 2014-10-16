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

import divconq.script.Activity;
import divconq.script.BlockInstruction;
import divconq.script.ExecuteState;
import divconq.script.StackEntry;
import divconq.script.StackFunctionEntry;

public class Function extends BlockInstruction {
	@Override
    public void run(final StackEntry stack) {
		StackFunctionEntry fstack = (StackFunctionEntry)stack;
		
		if (stack.getState() == ExecuteState.Ready) 
			fstack.setParameterName(stack.stringFromSource("Parameter"));

		super.run(stack);
    }
	
	@Override
	public StackEntry createStack(Activity act, StackEntry parent) {
		return new StackFunctionEntry(act, parent, this);
	}
}
