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
package divconq.filestore;

import divconq.lang.op.OperationCallback;
import divconq.session.DataStreamChannel;
import divconq.session.IStreamDriver;

// TODO rename
public interface IFileStoreStreamDriver extends IStreamDriver {
	void init(DataStreamChannel channel, OperationCallback cb);
}
