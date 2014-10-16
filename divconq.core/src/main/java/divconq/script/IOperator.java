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

import divconq.struct.Struct;
import divconq.xml.XElement;

public interface IOperator {
	void operation(StackEntry stack, XElement code, Struct dest);
}
