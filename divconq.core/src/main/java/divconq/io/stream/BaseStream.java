package divconq.io.stream;

import divconq.struct.RecordStruct;

abstract public class BaseStream extends RecordStruct implements IStream {
	protected IStream upstream = null;
	protected IStream downstream = null;
	
	public void setUpstream(IStream upstream) {
		this.upstream = upstream;
		
		upstream.setDownstream(this);
	}
	
	public void setDownstream(IStream downstream) {
		this.downstream = downstream;
	}
	
	/**
	 * A message was sent from upstream to me.
	 * 
	 * @param msg data to process
	 * @return AWAIT if you are processing this async
	 */
	abstract public HandleReturn handle(StreamMessage msg);
	
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
	abstract public void request();
	
	public IStreamSource getOrigin() {
		if (this instanceof IStreamSource)
			return (IStreamSource) this;
		
		if (this.upstream != null)
			return this.upstream.getOrigin();
		
		return null;
	}
	
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
