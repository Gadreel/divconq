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
import divconq.script.StackEntry;
import divconq.struct.ScalarStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;

public class Global extends With {
	@Override
	public void run(final StackEntry stack) {
		if (stack.getState() == ExecuteState.Ready) {
            String def = stack.stringFromSource("Type");
            String name = stack.stringFromSource("Name");
            
            Struct var = null;
            
            if (StringUtil.isNotEmpty(def))
            	var = stack.getActivity().createStruct(def);

			if (stack.codeHasAttribute("SetTo")) {
		        Struct var3 = stack.refFromSource("SetTo");
				
				if (var == null) 
	            	var = stack.getActivity().createStruct(var3.getType().getId());		
				
				if (var == null) {
					stack.setState(ExecuteState.Exit);
					stack.log().errorTr(522);
					stack.resume();
					return;
				} 
				
				if (var instanceof ScalarStruct) 
					((ScalarStruct) var).adaptValue(var3);
				else
					var = var3;
			}
            
			if (var == null) {
				stack.setState(ExecuteState.Exit);
				stack.log().errorTr(520);
				stack.resume();
				return;
			}
			
			// put the variable in global level
            stack.getActivity().addVariable(name, var);

			stack.getStore().setField("CurrNode", 0);
			stack.getStore().setField("Target", var);
			stack.setState(ExecuteState.Resume);
			
			stack.resume();
		}		
		else
			super.run(stack);
	}
}
