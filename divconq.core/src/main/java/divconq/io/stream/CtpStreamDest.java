package divconq.io.stream;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import divconq.interchange.IFileStoreDriver;
import divconq.interchange.IFileStoreFile;
import divconq.lang.op.OperationContext;
import divconq.script.StackEntry;
import divconq.struct.Struct;
import divconq.struct.scalar.NullStruct;
import divconq.xml.XElement;

public class CtpStreamDest extends BaseStream implements IStreamDest {
	protected Channel chan = null;
	protected boolean userelpath = false;
	protected String relpath = null;
	
	public CtpStreamDest(Channel chan) {
		this.chan = chan;
	}

	@Override
	public void init(StackEntry stack, XElement el, boolean autorelative) {
		if (autorelative || stack.boolFromElement(el, "Relative", false) || el.getName().startsWith("X")) {
        	this.relpath = "";
        	this.userelpath = true;
        }

        Struct src = stack.refFromElement(el, "RelativeTo");
        
        if ((src != null) && !(src instanceof NullStruct)) {
            if (src instanceof IFileStoreDriver) 
            	this.relpath = "";
            else if (src instanceof IFileStoreFile)
            	this.relpath = ((IFileStoreFile)src).getPath();
            else 
            	this.relpath = src.toString();
            
        	this.userelpath = true;
        }
	}
	
	@Override
	public void close() {
		// TODO return the channel to it's manager, if not already - if we didn't get a FINAL below then 
		// tell manager it is bad - close channel, read/write state unknown
		
		this.chan = null;
		
		super.close();
	}
	
	@Override
	public HandleReturn handle(StreamMessage msg) {
		ChannelFutureListener cfl = new ChannelFutureListener() {					
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					if (msg == StreamMessage.FINAL)
						OperationContext.get().getTaskRun().complete();
					else
						OperationContext.get().getTaskRun().resume();
				}
				else {
					OperationContext.get().getTaskRun().kill("ERROR sending - DONE sending!  " + future.cause());
				}	
			}
		};
    	
		if (msg == StreamMessage.FINAL)
			this.chan.writeAndFlush(new TerminateStream()).addListener(cfl);
		else
			this.chan.writeAndFlush(msg).addListener(cfl);		// TODO build up a buffer of at least N size before flush...FINAL always flushes
    	
		return HandleReturn.AWAIT;
	}

	@Override
	public void request() {
		// we are terminal, no downstream should call us
		OperationContext.get().getTaskRun().kill("File destination cannot be a source");
	}

	@Override
	public void execute() {
		// TODO optimize if upstream is local file 
		
		this.upstream.request();
	}

}
