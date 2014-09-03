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

import divconq.bus.net.StreamMessage;

public interface IStreamDriver {
	void message(StreamMessage msg);
	void cancel();
	void nextChunk();		// for source, tell it to send more
}
