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
package divconq.ctp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;

public interface ICtpChannel {
	void read();
	void send(ByteBuf buf, ChannelFutureListener listener);
	void close();
}
