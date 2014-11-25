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

import io.netty.buffer.ByteBuf;
import divconq.ctp.f.FileDescriptor;

public interface IStream extends AutoCloseable {
	void setUpstream(IStream upstream);
	void setDownstream(IStream downstream);
	
	ReturnOption handle(FileDescriptor file, ByteBuf data);
	void read();
	
	IStreamSource getOrigin();
	void cleanup();
}
