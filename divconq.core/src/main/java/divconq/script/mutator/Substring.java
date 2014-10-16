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
package divconq.script.mutator;

import divconq.script.IOperator;
import divconq.script.StackEntry;
import divconq.struct.Struct;
import divconq.struct.scalar.StringStruct;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class Substring implements IOperator {

	@Override
	public void operation(StackEntry stack, XElement code, Struct dest) {
		int from = (int) stack.intFromElement(code, "From", 0); 
		int to = (int) stack.intFromElement(code, "To", 0); 
		int length = (int) stack.intFromElement(code, "Length", 0); 
				
		if (dest instanceof StringStruct) {
			StringStruct idest = (StringStruct)dest;
			String val = idest.getValue();
			
			if (StringUtil.isEmpty(val))
				return;
			
			if (to > 0) 
				idest.setValue(val.substring(from, to));
			else if (length > 0) 
				idest.setValue(val.substring(from, from + length));
			else
				idest.setValue(val.substring(from));
			
			System.out.println("Using override Substring!");
		}
		
		stack.resume();
	}
}
