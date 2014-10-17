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

import divconq.script.BlockInstruction;
import divconq.script.ExecuteState;
import divconq.script.StackEntry;
import divconq.struct.Struct;
import divconq.struct.scalar.BooleanStruct;

public class Else extends BlockInstruction {
    @Override
    public void run(final StackEntry stack) {
        // if we do not pass logical condition then mark as done so we will skip this block
        // note that for the sake of nice debugging we do not set Done state here, would cause skip in debugger
		if (stack.getState() == ExecuteState.Ready) {
			Struct passvar = stack.queryVariable("_LastIf");
			boolean pass = false;
			
			if ((passvar != null) && (passvar instanceof BooleanStruct))
				pass = !((BooleanStruct)passvar).getValue();
			
			if (!pass)
				stack.setState(ExecuteState.Done); 
		}
		
    	super.run(stack);
    }
}
