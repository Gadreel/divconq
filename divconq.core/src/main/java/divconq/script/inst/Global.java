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

import divconq.script.StackEntry;
import divconq.struct.ScalarStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;

public class Global extends With {
	@Override
	public void prepTarget(StackEntry stack) {
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
				stack.log().errorTr(522);
				this.nextOpResume(stack);
				return;
			} 
			
			if (var instanceof ScalarStruct) 
				((ScalarStruct) var).adaptValue(var3);
			else
				var = var3;
		}
        
		if (var == null) {
			stack.log().errorTr(520);
			this.nextOpResume(stack);
			return;
		}
		
		// put the variable in global level
        stack.getActivity().addVariable(name, var);
        this.setTarget(stack, var);
		
		this.nextOpResume(stack);
	}
}
