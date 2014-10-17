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

import divconq.lang.OperationContext;
import divconq.locale.LocaleUtil;
import divconq.script.ExecuteState;
import divconq.script.Instruction;
import divconq.script.StackEntry;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class Debug extends Instruction {
	@Override
	public void run(final StackEntry stack) {
		String output = this.source.hasText() ? stack.resolveValue(this.source.getText()).toString() : null;
		long code = stack.intFromSource("Code", 0);
		
		if (StringUtil.isEmpty(output)) {
			List<XElement> params = this.source.selectAll("Param");
			Object[] oparams = new Object[params.size()];
			
			for (int i = 0; i < params.size(); i++) 
				oparams[i] = stack.refFromElement(params.get(i), "Value").toString();

			OperationContext tc = OperationContext.get();
			
			output = (tc != null) 
					? tc.tr("_code_" + code, oparams)
					: LocaleUtil.tr(LocaleUtil.getDefaultLocale(), "_code_" + code, oparams);
		}		
		
		stack.log().debug(code, output);
		
		//System.out.println(output);
		stack.setState(ExecuteState.Done);
		
		stack.resume();
	}
	
	@Override
	public void cancel(StackEntry stack) {
		// do nothing, this isn't cancellable
	}
}
