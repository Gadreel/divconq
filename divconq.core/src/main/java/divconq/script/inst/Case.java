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

import divconq.lang.OperationCallback;
import divconq.script.ExecuteState;
import divconq.script.LogicBlockInstruction;
import divconq.script.StackBlockEntry;
import divconq.script.StackEntry;
import divconq.struct.ScalarStruct;
import divconq.struct.Struct;

public class Case extends LogicBlockInstruction {
    @Override
    public void alignInstruction(final StackEntry stack, OperationCallback callback) {
    	StackBlockEntry bstack = (StackBlockEntry)stack;
    	
    	if (bstack.getPosition() >= this.instructions.size()) {
    		stack.setState(ExecuteState.Break);						// Break, not Done - get out of the Switch
        	
        	callback.completed();
    	}
        else
        	super.alignInstruction(stack, callback);
    }
    
    @Override
    public void run(final StackEntry stack) {
		if (stack.isDone()) {
        	stack.setState(ExecuteState.Done);
        	stack.resume();
        	return;
		}
		
		// mark the flag as ready to stop, but to give debugger a
		// natural feel don't actually stop until second execute
		if (stack.getState() == ExecuteState.Ready) {
			// use local name in condition check if present
	        // if not then try parent name
			Struct var = stack.codeHasAttribute("Target")
					? stack.refFromSource("Target")
					: stack.getParent().refFromSource("Target");
	        
	        if (var == null) 
	        	stack.log().trace(1, "Case has no variable to compare with, missing Target");	        
	        else if (!(var instanceof ScalarStruct)) {
	        	stack.log().trace(1, "Case has no variable to compare with, Target is not a scalar");
	        	var = null;
	        }
	        
	        // if we do not have or do not pass logical condition then mark as done so we will skip this block
	        // note that for the sake of nice debugging we do not set Done state here, would cause skip in debugger
	        stack.setDone((var == null) || !this.checkLogic(stack, (ScalarStruct)var, this.source)); 
		}

    	super.run(stack);
    }
}
