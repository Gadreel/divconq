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
import divconq.script.Instruction;
import divconq.script.StackEntry;
import divconq.struct.RecordStruct;
import divconq.struct.ScalarStruct;
import divconq.struct.Struct;
import divconq.xml.XElement;
import divconq.xml.XNode;

public class With extends Instruction {
	@Override
	public void run(final StackEntry stack) {
		if (stack.getState() == ExecuteState.Ready) {
	        Struct var = stack.refFromSource("Target");

			if (stack.codeHasAttribute("SetTo")) {
		        Struct var3 = stack.refFromSource("SetTo");
								
				if (var instanceof ScalarStruct) 
					((ScalarStruct) var).adaptValue(var3);
				else {
					// TODO replace variable at correct stack block level - but then again maybe we don't really need this - consider error instead
					
					///var = var3;
					
		            //stack.getActivity().addVariable(stack, name, var);					
				}
			}
			
			stack.getStore().setField("CurrNode", 0);
			stack.getStore().setField("Target", var);
			stack.setState(ExecuteState.Resume);
		}
		else if (stack.getState() == ExecuteState.Resume) {
			Struct var2 = (Struct) stack.getStore().getField("Target");
			
			while (true) {
				int cnode = stack.getStore().getFieldAsInteger("CurrNode").intValue();
				
				if (cnode >= this.source.children()) {
					stack.setState(ExecuteState.Done);
					break;
				}
				
				XNode nod = this.source.getChild(cnode);
				stack.getStore().setField("CurrNode", cnode + 1);
				
				if (nod instanceof XElement) {
					// operate is responsible for stack.resume();
					stack.operate(var2, (XElement)nod);
					return;
				}
			} 
		}
		
		stack.resume();
	}

	@Override
	public RecordStruct collectDebugRecord(final StackEntry stack, RecordStruct rec) {
		super.collectDebugRecord(stack, rec);
		
		if (stack.getState() == ExecuteState.Ready)  
			return null;

		if (stack.getState() == ExecuteState.Resume) {
			int cnode = stack.getStore().getFieldAsInteger("CurrNode").intValue() - 1;
			
			if (cnode >= this.source.children()) 
				return null;
			
			XNode nod = this.source.getChild(cnode);
			
			if (! (nod instanceof XElement)) 
				return null;

			XElement mut = (XElement)nod;
			
			RecordStruct sub = new RecordStruct();
			sub.setField("Line", mut.getLine());
			sub.setField("Column", mut.getCol());		
			sub.setField("Command", mut.toLocalString());
		   	return sub;
		}
		
		return null;
	}
	
	@Override
	public void cancel(StackEntry stack) {
		// do nothing, this isn't cancellable
	}
}
