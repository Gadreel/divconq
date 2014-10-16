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

import divconq.script.ExecuteState;
import divconq.script.LogicBlockInstruction;
import divconq.script.StackEntry;

public class If extends LogicBlockInstruction {
    @Override
    public void run(final StackEntry stack) {
		if (stack.isDone()) {
        	stack.setState(ExecuteState.Done);
	        stack.resume();
        	return;
		}
		
        // if we do not pass logical condition then mark as done so we will skip this block
        // note that for the sake of nice debugging we do not set Done state here, would cause skip in debugger
		if (stack.getState() == ExecuteState.Ready) 
	        stack.setDone(!this.checkLogic(stack)); 

    	super.run(stack);
    }
}
