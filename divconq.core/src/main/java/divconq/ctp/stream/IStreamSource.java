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
package divconq.ctp.stream;

import divconq.script.StackEntry;
import divconq.xml.XElement;

public interface IStreamSource extends IStream {
	void init(StackEntry stack, XElement el);
}
