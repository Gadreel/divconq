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
package divconq.session;

import divconq.bus.Message;
import divconq.struct.ListStruct;

public interface ISessionAdapter {
	void deliver(Message msg);
	ListStruct popMessages();
	void stop();
	String getClientKey();
}
