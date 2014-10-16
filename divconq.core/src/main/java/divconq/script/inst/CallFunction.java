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
import divconq.script.ExecuteState;
import divconq.script.IInstructionCallback;
import divconq.script.Instruction;
import divconq.script.StackCallEntry;
import divconq.script.StackEntry;
import divconq.script.StackFunctionEntry;
import divconq.struct.Struct;

public class CallFunction extends Instruction {
	@Override
    public void run(final StackEntry stack) {
		if (stack.isDone()) {
        	stack.setState(ExecuteState.Done);
        	stack.resume();
        	return;
		}
				
		final StackCallEntry cstack = (StackCallEntry)stack;
		
		if (stack.getState() == ExecuteState.Ready) {
			String name = stack.stringFromSource("Name");
			Struct arg = stack.refFromSource("Argument");		// naturally default to string if no type given

			Instruction func = stack.getActivity().queryFunction(name);

			if (func == null) {
				stack.log().errorTr(517, name);
	        	stack.setState(ExecuteState.Exit); 
			}
			else {
				StackEntry sfunc = func.createStack(stack.getActivity(), stack);
				
				if ((sfunc == null) || !(sfunc instanceof StackFunctionEntry)) {
					stack.log().errorTr(518, name);
		        	stack.setState(ExecuteState.Exit); 
				}
				else {
					StackFunctionEntry sbfunc = (StackFunctionEntry)sfunc;
					
					sbfunc.setParameter(arg);
					
					cstack.setChild(sbfunc);
		        	stack.setState(ExecuteState.Resume);
				}
			}
			
			stack.resume();
        	return;
		}
		
        if (stack.getState() != ExecuteState.Resume) {
        	stack.resume();
        	return;
        }

        final StackFunctionEntry child = (StackFunctionEntry)cstack.getChild();
        
        if (child == null) {
			stack.log().errorTr(519);
        	stack.setState(ExecuteState.Exit); 
        	stack.resume();
        	return;
        }
        
        child.run(new IInstructionCallback() {
			@Override
			public void resume() {
				ExecuteState cstate = child.getState();
				
				if ((cstate != ExecuteState.Ready) && (cstate != ExecuteState.Resume)) {
			        if (cstate == ExecuteState.Exit) 
			        	stack.setState(ExecuteState.Exit);
			        else  {
			        	stack.setDone(true);
			        	cstack.setChild(null);
			        }
				}
				
				stack.resume();
			}
		});
    }
	
	@Override
	public StackEntry createStack(Activity act, StackEntry parent) {
		return new StackCallEntry(act, parent, this);
	}
	
	@Override
	public void cancel(StackEntry stack) {
		// TODO
	}
}
