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

import java.util.List;

import divconq.script.ExecuteState;
import divconq.script.Instruction;
import divconq.script.StackEntry;
import divconq.struct.Struct;
import divconq.struct.scalar.StringStruct;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class Exit extends Instruction {
	@Override
	public void run(final StackEntry stack) {
		String output = this.source.hasText() ? stack.resolveValue(this.source.getText()).toString() : null;
		long code = stack.intFromSource("Code", 0);
		Struct result = stack.codeHasAttribute("Result") ? stack.refFromSource("Result") : null;
		
		if (StringUtil.isNotEmpty(output))
			stack.log().exit(code, output);
		else if (code > 0) {
			List<XElement> params = this.source.selectAll("Param");
			Object[] oparams = new Object[params.size()];
			
			for (int i = 0; i < params.size(); i++) 
				oparams[i] = stack.refFromElement(params.get(i), "Value").toString();

			stack.log().exitTr(code, oparams);
		}
		
		//System.out.println(stack.log().getMessage());
		
		if ((result == null) && StringUtil.isNotEmpty(output))
			result = new StringStruct(output);
		
		if (stack.codeHasAttribute("Code")) { 
			stack.setLastResult(code, result);
			stack.log().exit(code, (result != null) ? result.toString() : null);
		}
		else if (result != null) {
			stack.setLastResult(result);
		}
		
		stack.setState(ExecuteState.Exit);
		stack.resume();
	}
	
	@Override
	public void cancel(StackEntry stack) {
		// do nothing, this isn't cancellable
	}
}
