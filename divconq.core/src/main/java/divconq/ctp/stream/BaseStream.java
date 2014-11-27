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
import divconq.struct.RecordStruct;

abstract public class BaseStream extends RecordStruct implements IStream {
	protected IStream upstream = null;
	protected IStream downstream = null;
	
	@Override
	public void setUpstream(IStream upstream) {
		this.upstream = upstream;
		
		upstream.setDownstream(this);
	}
	
	@Override
	public void setDownstream(IStream downstream) {
		this.downstream = downstream;
	}
	
	/*
	 * A message was sent from upstream to me.
	 * 
	 * @param msg data to process
	 * @return AWAIT if you are processing this async
	 */
	@Override
	abstract public ReturnOption handle(FileDescriptor file, ByteBuf data);
	
	/**
	 * A cancel was sent from upstream to me.
	 */
	/*
	public void cancel(TaskRun cb) {
		if (this.downstream != null)
			this.downstream.cancel(cb);
	}
	*/
	
	/**
	 * downstream is requesting that you send more messages
	 */
	@Override
	abstract public void read();
	
	@Override
	public IStreamSource getOrigin() {
		if (this instanceof IStreamSource)
			return (IStreamSource) this;
		
		if (this.upstream != null)
			return this.upstream.getOrigin();
		
		return null;
	}
	
	@Override
	public void cleanup() {
		IStream up = this.upstream;
		
		if (up != null)
			up.cleanup();
		
		this.close();
	}
	
	@Override
	public void close() {
		this.upstream = null;
		this.downstream = null;
	}
}
