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

import divconq.lang.op.OperationCallback;
import divconq.script.ExecuteState;
import divconq.script.LogicBlockInstruction;
import divconq.script.StackBlockEntry;
import divconq.script.StackEntry;

public class Until extends LogicBlockInstruction {
    @Override
    public void alignInstruction(final StackEntry stack, OperationCallback callback) {
    	StackBlockEntry bstack = (StackBlockEntry)stack;
    	
    	// signal end if conditional logic fails after loop
    	if (bstack.getPosition() >= this.instructions.size()) 
    		if (!this.checkLogic(stack))
    			bstack.setPosition(0);
    		else
	        	stack.setState(ExecuteState.Done);
    	
       	super.alignInstruction(stack, callback);
    }
}
