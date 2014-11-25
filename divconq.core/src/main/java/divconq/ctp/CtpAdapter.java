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

import divconq.ctp.cmd.ResponseCommand;
import divconq.hub.Hub;
import divconq.lang.op.FuncCallback;
import divconq.lang.op.OperationContext;
import divconq.log.Logger;
import divconq.struct.RecordStruct;
import divconq.work.IWork;
import divconq.work.Task;
import divconq.work.TaskRun;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;

public class CtpAdapter {
	protected boolean switchingProtocols = false;
	protected CtpCommandMapper mapper = CtpCommandMapper.instance;		// for basic operations
	protected ICommandHandler handler = null;
	protected ICtpChannel channel = null;
	protected OperationContext context = null;
	
	protected CtpCommand current = null;
	
	protected FuncCallback<RecordStruct> currCallback = null;
	
	public void setChannel(ICtpChannel v) {
		this.channel = v;
	}
	
	public ICtpChannel getChannel() {
		return this.channel;
	}
	
	protected void setSwitchingProtocols(boolean v) {
		this.switchingProtocols = v;
	}
	
	public boolean isSwitchingProtocols() {
		return this.switchingProtocols;
	}
	
	public void setMapper(CtpCommandMapper v) {
		this.mapper = v;
	}
	
	public void setHandler(ICommandHandler v) {
		this.handler = v;
	}
	
	public OperationContext getContext() {
		return this.context;
	}
	
	public CtpAdapter(OperationContext ctx) {
		this.context = ctx;
	}
	
	public CtpAdapter() {
		this.context = OperationContext.get();
	}
	
	// used internally
	// return false if need more
	public boolean decode(ByteBuf buf) throws Exception {
		OperationContext.set(this.context);
		
		// get the command to continue to decode itself
		if (this.current != null) 
			return this.current.decode(buf);
		
		if (buf.readableBytes() < 1)
			return false;
		
		int cmdtype = buf.readUnsignedByte();
		
		this.current = this.mapper.map(cmdtype);
		
		// TODO handle error/close?
		if (this.current == null)
			return false;
		
		// get the command to decode itself
		return this.current.decode(buf);
	}
	
	// used internally
	public void handleCommand() {
		OperationContext.set(this.context);
		
		CtpCommand cmd = this.current;
		
		this.current = null;
		
		//System.out.println("Got Command ------ " + cmd.getClass().getName());
		
		//if (!this.readRequested)
		//	System.out.println("-------- Got Command when no read was requested ------ " + cmd.getClass().getName());
		
		//this.readRequested = false;
		
		if (cmd == CtpCommand.ALIVE) {
			//System.out.println("Ctp Adapter Touched");
			// TODO touch session...
			this.read();		// indicate we want another message
		}
		else if (cmd == CtpCommand.EXIT_NO_SIGN_OUT) {
			// TODO exit
			this.close();
		}
		else if (cmd == CtpCommand.EXIT_SIGN_OUT) {
			// TODO sign out and exit...
			this.close();
		}
		else if (cmd instanceof ResponseCommand) {
			FuncCallback<RecordStruct> cb = this.currCallback;
			
			if (cb != null) {
				this.currCallback = null;
				
				// TODO - rather than create a new task all the time, possibly make this ctpfclient class be an always running
				// never timing out task with state that can be resumed intermedentately to do different tasks...
				// then use with Progress and Read, etc too.
				// maybe set timeout to 30 minutes and close connection if idle that long? or just have the keep alive scheduler
				// also touch this?
				
				// put the call back into the work pool, don't tie up the IO thread 
				Task t = new Task()
					.withContext(cb.getContext().subContext())
					.withWork(new IWork() {
						@Override
						public void run(TaskRun trun) {
							RecordStruct res = ((ResponseCommand)cmd).getResult();
							
							if (res != null) {
								trun.getContext().logResult(res);
								cb.setResult(res);
							}
							
							cb.complete();

							// no, let caller decide when to read
							//CtpAdapter.this.read();
							
							trun.complete();
						}
					});
				
				Hub.instance.getWorkPool().submit(t);
			}
			else {
				// if we get a response but no callback is in queue - TODO log?
				
				// be sure to read (or close)
				this.read();
			}
		}
		else {
			// make sure handle runs in WorkPool so IO threads are free
			try {
				this.handler.handle(cmd, this);
			} 
			catch (Exception x) {
				// TODO exit/abort
				System.out.println("Error with command handler: " + x);
			}		
		}
	}
	
	// when using this be sure to issue "adapter.read()" in callback
	public void sendCommand(CtpCommand cmd, FuncCallback<RecordStruct> cb) throws Exception {
		this.currCallback = cb;
		
		this.sendCommandNotify(cmd, null);
	}
	
	public void sendCommand(CtpCommand cmd) throws Exception {
		this.sendCommandNotify(cmd, null);
	}
	
	public void sendCommandNotify(CtpCommand cmd, ChannelFutureListener listener) throws Exception {
        ByteBuf buf = cmd.encode();
		
        //System.out.println("writing buffer: " + buf.readableBytes());
        
        // TODO write to channel, not release
        //buf.release();
        
		try {
			if (this.channel != null) 
				this.channel.send(buf, listener);
		}
		catch (Exception x) {
			Logger.error("Error writing Ctp message: " + cmd);
			Logger.error("Error writing Ctp message: " + x);
			
			this.close();
		}
	}
	
	//protected boolean init = false;
	
	public void read() {
		if (this.switchingProtocols) {
			// TODO send CTP_S_CMD_RESPONSE_SUCCESS when read called, if
		}
		
		if (this.channel != null)
			this.channel.read();
		
		/*
		if (!this.init) {
			this.init = true;
			this.current = new InitCommand();
			this.handleCommand();
		}
		*/
	}
	
	public void close() {
		// TODO more
		
		if (this.channel != null) {
			this.channel.close();
			this.channel = null;
		}
		
		this.handler.close();
	}
}
