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

import divconq.hub.Hub;
import divconq.lang.FuncCallback;
import divconq.lang.OperationCallback;
import divconq.script.BlockInstruction;
import divconq.script.ExecuteState;
import divconq.script.StackBlockEntry;
import divconq.script.StackEntry;
import divconq.struct.IItemCollection;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.struct.scalar.AnyStruct;
import divconq.util.IAsyncIterator;

public class ForEach extends BlockInstruction {
    @Override
    public void alignInstruction(final StackEntry stack, final OperationCallback callback) {
    	final StackBlockEntry bstack = (StackBlockEntry)stack;
    	
    	// signal end if conditional logic fails after loop
    	if (bstack.getPosition() >= this.instructions.size()) { 
    		RecordStruct store = stack.getStore();
    	    
    		AnyStruct collection = (AnyStruct) store.getField("Collection");
    	    final String name = store.getFieldAsString("Name");
        	
        	@SuppressWarnings("unchecked")
			final IAsyncIterator<Struct> it = (IAsyncIterator<Struct>) collection.getValue();
        	
        	if (it != null) {
        		it.hasNext(new FuncCallback<Boolean>() {					
					@Override
					public void callback() {
						if (this.getResult()) {
							it.next(new FuncCallback<Struct>() {								
								@Override
								public void callback() {									
					        		bstack.addVariable(name, this.getResult());
					        		bstack.setPosition(0);
					        		
						        	ForEach.super.alignInstruction(stack, callback);
								}
							});
						}
						else {
				        	stack.setState(ExecuteState.Done);
				    	
				        	ForEach.super.alignInstruction(stack, callback);
						}
					}
				}); 

        		return;
        	}
        	else 
	        	stack.setState(ExecuteState.Done);
    	}
    	
       	super.alignInstruction(stack, callback);
    }
    
    @Override
    public void run(final StackEntry stack) {
		if (stack.getState() == ExecuteState.Ready) {
			String name = stack.stringFromSource("Name");  
			Struct source = stack.refFromSource("In");

			AnyStruct collection = new AnyStruct();
			collection.setType(Hub.instance.getSchema().getType("Any"));		// TODO shouldn't need this
			
			if (source instanceof IItemCollection)
				collection.setValue(((IItemCollection)source).getItemsAsync().iterator());
			
    		RecordStruct store = stack.getStore();
    	    
    		store.setField("Collection", collection);
    	    store.setField("Name", name);
    	    
    	    // tell alignment to do first iteration by passing position beyond end
        	StackBlockEntry bstack = (StackBlockEntry)stack;
        	bstack.setPosition(this.instructions.size());
		}
		
		super.run(stack);
	}
}
