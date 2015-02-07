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
import divconq.lang.op.OperationContext;
import divconq.script.BlockInstruction;
import divconq.script.ExecuteState;
import divconq.script.StackBlockEntry;
import divconq.script.StackEntry;
import divconq.struct.RecordStruct;
import divconq.struct.scalar.IntegerStruct;

public class For extends BlockInstruction {
    @Override
    public void alignInstruction(final StackEntry stack, OperationCallback callback) {
    	StackBlockEntry bstack = (StackBlockEntry)stack;
    	
    	// signal end if conditional logic fails after loop
    	if (bstack.getPosition() >= this.instructions.size()) { 
    		RecordStruct store = stack.getStore();
    	    
    		IntegerStruct cntvar = (IntegerStruct) store.getField("Counter");
    	    long to = store.getFieldAsInteger("To");
    	    long step = store.getFieldAsInteger("Step");
    	    
        	cntvar.setValue(cntvar.getValue() + step);
        	
        	boolean flagdone = false;

        	if (step > 0) 
        		flagdone = (cntvar.getValue() > to);
        	else 
        		flagdone = (cntvar.getValue() < to);
        	
        	if (flagdone) 
	        	stack.setState(ExecuteState.Done);
        	else
        		bstack.setPosition(0);
    	}
    	
       	super.alignInstruction(stack, callback);
    }
    
    @Override
    public void run(final StackEntry stack) {
    	StackBlockEntry bstack = (StackBlockEntry)stack;
    	
		if (stack.getState() == ExecuteState.Ready) {
			long from = stack.intFromSource("From", 0);  
			long to = stack.intFromSource("To", 0);
			long step = stack.intFromSource("Step", 1);

			IntegerStruct cntvar = new IntegerStruct();

			cntvar.setType(OperationContext.get().getSchema().getType("Integer"));		// TODO souldn't need this
			//cntvar.setName(stack.stringFromSource("Name", "_forindex"));
			cntvar.setValue(from);
			
    		RecordStruct store = stack.getStore();
    	    
    		store.setField("Counter", cntvar);
    	    store.setField("To", to);
    	    store.setField("Step", step);

			bstack.addVariable(stack.stringFromSource("Name", "_forindex"), cntvar);
		}
		
		super.run(stack);
	}
}
