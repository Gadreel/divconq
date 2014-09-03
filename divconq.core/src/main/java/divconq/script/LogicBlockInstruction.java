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

import divconq.struct.ScalarStruct;
import divconq.struct.Struct;
import divconq.xml.XElement;

abstract public class LogicBlockInstruction extends BlockInstruction {
    protected boolean checkLogic(StackEntry stack) {
        return checkLogic(stack, this.source);
    }

    protected boolean checkLogic(StackEntry stack, XElement source) {
        if (source == null) 
        	source = this.source;
      
        Struct target = source.hasAttribute("Target")
        		? stack.refFromElement(source, "Target")
        	    : stack.queryVariable("_LastResult");

        return this.checkLogic(stack, (ScalarStruct)target, source);
    }

    protected boolean checkLogic(StackEntry stack, ScalarStruct target, XElement source) {
        boolean isok = true;
        boolean condFound = false;
        
        if (source == null) 
        	source = this.source;

        if (target == null) {
        	isok = false;
            
    		if (stack.boolFromElement(source, "IsNull")) 
    			isok = !isok;
        }
        else {
			if (isok && source.hasAttribute("Equal")) {
				Struct other = stack.refFromElement(source, "Equal");
	            isok = (target.compare(other) == 0);  //  (var == iv);
	            condFound = true;
	        }
			
			if (isok && source.hasAttribute("Equals")) {
				Struct other = stack.refFromElement(source, "Equals");
	            isok = (target.compare(other) == 0);  //  (var == iv);
	            condFound = true;
	        }
			
			if (isok && source.hasAttribute("LessThan")) {
				Struct other = stack.refFromElement(source, "LessThan");
	            isok = (target.compare(other) < 0);  //  (var < iv);
	            condFound = true;
	        }
			
			if (isok && source.hasAttribute("GreaterThan")) {
				Struct other = stack.refFromElement(source, "GreaterThan");
	            isok = (target.compare(other) > 0);  //  (var > iv);
	            condFound = true;
	        }
	        
			if (isok && source.hasAttribute("LessThanOrEqual")) {
				Struct other = stack.refFromElement(source, "LessThanOrEqual");
	            isok = (target.compare(other) <= 0);  //  (var <= iv);
	            condFound = true;
	        }
			
			if (isok && source.hasAttribute("GreaterThanOrEqual")) {
				Struct other = stack.refFromElement(source, "GreaterThanOrEqual");
	            isok = (target.compare(other) >= 0);  //  (var >= iv);
	            condFound = true;
	        }
			
			if (!condFound) 
				isok = target.checkLogic(stack, source);			
        }
        
		if (stack.boolFromElement(source, "Not")) 
			isok = !isok;

        return isok;
    }
}
