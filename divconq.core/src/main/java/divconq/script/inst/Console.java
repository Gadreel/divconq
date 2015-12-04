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

import divconq.locale.Tr;
import divconq.script.ExecuteState;
import divconq.script.IDebugger;
import divconq.script.Instruction;
import divconq.script.StackEntry;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class Console extends Instruction {
	@Override
	public void run(final StackEntry stack) {
		String output = this.source.hasText() ? stack.resolveValue(this.source.getText()).toString() : null;
		long code = stack.intFromSource("Code", 0);
		
		if (StringUtil.isEmpty(output)) {
			List<XElement> params = this.source.selectAll("Param");
			Object[] oparams = new Object[params.size()];
			
			for (int i = 0; i < params.size(); i++) 
				oparams[i] = stack.refFromElement(params.get(i), "Value").toString();
			
			output = Tr.tr("_code_" + code, oparams);
		}		
		
		if (output == null)
			System.out.println();
		else
			System.out.println(output);
		
		IDebugger dbg = stack.getActivity().getDebugger();
		
		if (dbg != null) {
			if (output == null)
				dbg.console("");
			else
				dbg.console(output);
		}
		
		stack.setState(ExecuteState.Done);
		
		stack.resume();
	}
	
	@Override
	public void cancel(StackEntry stack) {
		// do nothing, this isn't cancellable
	}
}
