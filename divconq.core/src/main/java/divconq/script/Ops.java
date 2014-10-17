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

import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.struct.scalar.NullStruct;
import divconq.xml.XElement;
import divconq.xml.XNode;

// for instructions that are composed of multiple operations
abstract public class Ops extends Instruction {
	@Override
	public void run(StackEntry stack) {
		if (stack.getState() == ExecuteState.Ready) {
			stack.getStore().setField("SubAlign", true);
			stack.getStore().setField("CurrNode", 0);
			stack.getStore().setField("Target", NullStruct.instance);
			stack.setState(ExecuteState.Resume);
			this.prepTarget(stack);
		}
		else if (stack.getState() == ExecuteState.Resume) {
			if (stack.getStore().getFieldAsBooleanOrFalse("SubAlign")) {
				this.nextOpResume(stack);
				return;
			}

			// next op needs to align
			stack.getStore().setField("SubAlign", true);
			
			Struct var2 = (Struct) stack.getStore().getField("Target");
			int cnode = stack.getStore().getFieldAsInteger("CurrNode").intValue();
			XNode nod = this.source.getChild(cnode - 1);
			
			this.runOp(stack, (XElement)nod, var2);
		}
	}
	
	public void nextOpResume(StackEntry stack) {
		// next op needs to run
		stack.getStore().setField("SubAlign", false);
		
		while (true) {
			int cnode = stack.getStore().getFieldAsInteger("CurrNode").intValue();
			
			if (cnode >= this.source.children()) {
				stack.setState(ExecuteState.Done);
				break;
			}
			
			XNode nod = this.source.getChild(cnode);
			stack.getStore().setField("CurrNode", cnode + 1);
			
			if (nod instanceof XElement) {
				stack.setState(ExecuteState.Resume);
				break;
			}
		} 
		
		stack.resume();
	}
	
	public void setTarget(StackEntry stack, Struct v) {
		stack.getStore().setField("Target", v);
		stack.setLastResult(v);
	}
	
	// subclass is responsible for resuming stack  
	abstract public void prepTarget(StackEntry stack);
	
	// subclass is responsible for resuming stack  
	abstract public void runOp(StackEntry stack, XElement op, Struct target);

	@Override
	public RecordStruct collectDebugRecord(StackEntry stack, RecordStruct rec) {
		RecordStruct sub = super.collectDebugRecord(stack, rec);

		if (stack.getState() == ExecuteState.Resume) {
			int cnode = stack.getStore().getFieldAsInteger("CurrNode").intValue() - 1;
			
			if (cnode >= this.source.children()) 
				return null;
			
			XNode nod = this.source.getChild(cnode);
			
			if (! (nod instanceof XElement)) 
				return null;

			XElement mut = (XElement)nod;
			
			sub = new RecordStruct();
			sub.setField("Line", mut.getLine());
			sub.setField("Column", mut.getCol());		
			sub.setField("Command", mut.toLocalString());
		   	return sub;
		}
		
		return sub;
	}
}
