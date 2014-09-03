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
package divconq.script;

import java.util.ArrayList;
import java.util.List;

import divconq.lang.OperationCallback;
import divconq.lang.OperationResult;
import divconq.xml.XElement;

public abstract class BlockInstruction extends Instruction {
	protected List<Instruction> instructions = new ArrayList<Instruction>();

	protected List<Instruction> getInstructions() { 
		return this.instructions; 
    }

    public void addInstruction(Instruction inst) {
    	this.instructions.add(inst);
    }

	@Override
	public void compile(ActivityManager manager, OperationResult log) {
		super.compile(manager, log);
		
        for (XElement child : this.source.selectAll("*")) {
        	// Internal and Detail are always a special child tag, do not treat these as instructions
        	if ("Internal".equals(child.getName()) || "Detail".equals(child.getName()))
        		continue;
        	
            Instruction ni = manager.createInstruction(child);
            
            if (ni == null) {
            	log.errorTr(509, child.getName());
            	continue;
            }
            
            ni.setXml(child);
            
            this.addInstruction(ni);
            
            ni.compile(manager, log);
        }
	}

	// no need for try catch here, should always be handled in the subclass
	@Override
    public void run(final StackEntry stack) {
		if (stack.getState() == ExecuteState.Ready) {
        	stack.setState(ExecuteState.Resume);
        	stack.resume();
        	return;
		}
		
		this.alignInstruction(stack, new OperationCallback() {			
			@Override
			public void callback() {
		        if (stack.getState() != ExecuteState.Resume) {
		        	stack.resume();
		        	return;
		        }
		        
				final StackBlockEntry bstack = (StackBlockEntry)stack;
				
		        // give the debugger a more natural feel by pointing to the top instruction before executing it
		        if (bstack.getTopFlag()) {
		        	bstack.setTopFlag(false);
		        	stack.resume();
		        	return;
		        }

		        bstack.getChild().run(new IInstructionCallback() {
					@Override
					public void resume() {
						StackEntry child = bstack.getChild();
						
						if (child == null) {
							bstack.setState(ExecuteState.Done);
						}
						else {				
							ExecuteState cstate = child.getState();
							
							if ((cstate != ExecuteState.Ready) && (cstate != ExecuteState.Resume)) {
						        if (cstate == ExecuteState.Exit) 
						        	bstack.setState(ExecuteState.Exit);
						        else if (cstate == ExecuteState.Break)  
						        	bstack.setState(ExecuteState.Done);
						        else if (cstate == ExecuteState.Continue) 
						        	BlockInstruction.this.continueInstruction(stack);
						        else {  // if Done
						        	BlockInstruction.this.nextInstruction(stack, new OperationCallback() {										
										@Override
										public void callback() {
											stack.resume();
										}
									});
						        	
						        	return;
						        }
							}
						}
						
						stack.resume();
					}
				});
			}
		});
    }

	// continue is essentially the same as reaching the end of the block
	// see alignInstruction - subclasses will treat end of block as flag
	// to either bail or continue the loop (check condition)
    public void continueInstruction(final StackEntry stack) {
		final StackBlockEntry bstack = (StackBlockEntry)stack;
		
		bstack.setPosition(this.instructions.size());
		bstack.setChild(null);
		bstack.setTopFlag(true);
    }

    public void nextInstruction(final StackEntry stack, OperationCallback callback) {
		final StackBlockEntry bstack = (StackBlockEntry)stack;
		
    	bstack.setPosition(bstack.getPosition() + 1);
    	
    	// if at end of block, treat as a continue - initiate iteration logic
    	if (bstack.getPosition() >= this.instructions.size()) { 
    		this.continueInstruction(stack);
    		callback.completed();
    	}
    	else
    		this.alignInstruction(stack, callback);
    }
    
    // sub classes will override this method to control instruction placement
    // and block repetition
    public void alignInstruction(final StackEntry stack, OperationCallback callback) {
    	StackBlockEntry bstack = (StackBlockEntry)stack;
    	
    	if ((bstack.getState() == ExecuteState.Done) || (bstack.getState() == ExecuteState.Exit)) {
        	bstack.setChild(null);
    	}
    	else if ((bstack.getState() == ExecuteState.Break) || (bstack.getState() == ExecuteState.Continue)) {
        	bstack.setChild(null);
    		//stack.setState(ExecuteState.Done);
    	}
    	else if (bstack.getPosition() >= this.instructions.size()) {
        	bstack.setChild(null);
    		stack.setState(ExecuteState.Done);
    	}
        else {
        	Instruction inst = this.instructions.get(bstack.getPosition());        	
        	StackEntry child = bstack.getChild();
        	
        	// literally, is it the same inst?  if not then we load the current 
        	if ((child == null) || (child.getInstruction() != inst)) 
	        	bstack.setChild(inst.createStack(stack.getActivity(), bstack));
        }
    	
    	callback.completed();
    }

    @Override
	public StackEntry createStack(Activity act, StackEntry parent) {
		return new StackBlockEntry(act, parent, this);
	}	
	
	@Override
	public void cancel(StackEntry stack) {
		// only cancel on current aligned instruction - it is possible to be off here in a race, but instructions
		// should just be smart and only cancel when running
        if (stack.getState() == ExecuteState.Resume) {
			StackEntry child = ((StackBlockEntry)stack).getChild();
			
			if (child != null)
				child.cancel();
        }
	}
}
