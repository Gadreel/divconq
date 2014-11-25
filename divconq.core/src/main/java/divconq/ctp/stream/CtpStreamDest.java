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
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import divconq.ctp.CtpAdapter;
import divconq.ctp.CtpCommand;
import divconq.ctp.f.BlockCommand;
import divconq.ctp.f.CtpFCommand;
import divconq.ctp.f.FileDescriptor;
import divconq.filestore.IFileStoreDriver;
import divconq.filestore.IFileStoreFile;
import divconq.lang.op.OperationContext;
import divconq.script.StackEntry;
import divconq.struct.Struct;
import divconq.struct.scalar.NullStruct;
import divconq.work.TaskRun;
import divconq.xml.XElement;

// this is not tested much
public class CtpStreamDest extends BaseStream implements IStreamDest, ChannelFutureListener {
	protected CtpAdapter adapter = null;
	protected boolean userelpath = false;
	protected boolean finalFlag = false;
	protected String relpath = null;
	protected OperationContext ctx = null;
	
	public CtpStreamDest(CtpAdapter adapter, OperationContext ctx) {
		this.adapter = adapter;
		this.ctx = ctx;
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
		
		this.adapter = null;
		
		super.close();
	}

	@Override
	public void operationComplete(ChannelFuture future) throws Exception {
		OperationContext.set(this.ctx);
		
		TaskRun trun = this.ctx.getTaskRun();
		
		if (trun == null) {
			System.out.println("Error - stream task missing RUN");
		}
		else if (future.isSuccess()) {
			if (this.finalFlag)
				trun.complete();
			else
				trun.resume();
		}
		else {
			trun.kill("ERROR sending - DONE sending!  " + future.cause());
		}	
	}
	
	@Override
	public ReturnOption handle(FileDescriptor file, ByteBuf data) {
    	this.finalFlag = (file == FileDescriptor.FINAL);
    	
		// TODO build up a buffer of at least N size before flush...FINAL always flushes
		
		CtpCommand cmd = CtpFCommand.STREAM_FINAL;
		
		if (!this.finalFlag) {
			cmd = new BlockCommand();
			
			((BlockCommand)cmd).copyAttributes(file);
			((BlockCommand)cmd).setData(data);
		}
		 
		System.out.println("Sending FINAL: " + this.finalFlag);

		try {
			this.adapter.sendCommandNotify(cmd, this);
		} 
		catch (Exception x) {
			System.out.println("Ctp-F Client stream error: " + x);
		}
		
		/*
		try {
			// TODO if errors...ABORT
			
			if (this.isEmptyResult()) {
				this.adapter.sendCommand(new FinalCommand());
				OperationContext.get().getTaskRun().complete();
				return;
			}
			
			IFileStoreFile file = this.getResult();
			
			BlockCommand cmd = new BlockCommand();
			
			FileSelection selection = this.selector.selection();
			
			// TODO if CTP_F_ATTR_PREFERED then use session settings - from adapter?
			
			if (selection.hasAttr(CtpConstants.CTP_F_ATTR_PATH) || selection.hasAttr(CtpConstants.CTP_F_ATTR_PREFERED))
				cmd.setPath(file.path().subpath(this.relativeTo).toString());
			
			if (selection.hasAttr(CtpConstants.CTP_F_ATTR_IS_FOLDER) || selection.hasAttr(CtpConstants.CTP_F_ATTR_PREFERED))
				cmd.setIsFolder(file.isFolder());
			
			if (selection.hasAttr(CtpConstants.CTP_F_ATTR_SIZE) || selection.hasAttr(CtpConstants.CTP_F_ATTR_PREFERED))
				cmd.setSize(file.getSize());
			
			if (selection.hasAttr(CtpConstants.CTP_F_ATTR_MODTIME) || selection.hasAttr(CtpConstants.CTP_F_ATTR_PREFERED))
				cmd.setModTime(file.getModificationTime().getMillis());
			
			if (selection.hasAttr(CtpConstants.CTP_F_ATTR_PERMISSIONS) || selection.hasAttr(CtpConstants.CTP_F_ATTR_PREFERED))
				cmd.setPermissions(CtpConstants.CTP_F_PERMISSIONS_READ & CtpConstants.CTP_F_PERMISSIONS_WRITE);   // TODO file.getPermissions());
			
			
			if (selection.hasAttr(CtpConstants.CTP_F_ATTR_DATA)) {
				// send headers
				this.adapter.sendCommand(cmd);
				
				// send block 1
				cmd = new BlockCommand();
				
				ByteBuf d = Hub.instance.getBufferAllocator().buffer(24);
				
				d.writeLong(1);
				d.writeLong(2);
				d.writeLong(3);
				
				cmd.setData(d);
				
				this.adapter.sendCommand(cmd);
				
				// progress 1
				this.adapter.sendCommand(new ProgressCommand(33));
				
				// send block 2
				cmd = new BlockCommand();
				
				d = Hub.instance.getBufferAllocator().buffer(24);
				
				d.writeLong(1);
				d.writeLong(2);
				d.writeLong(3);
				
				cmd.setData(d);
				
				this.adapter.sendCommand(cmd);
				
				// progress 2
				this.adapter.sendCommand(new ProgressCommand(66));
				
				// send block 3
				cmd = new BlockCommand();
				
				d = Hub.instance.getBufferAllocator().buffer(24);
				
				d.writeLong(1);
				d.writeLong(2);
				d.writeLong(3);
				
				cmd.setData(d);
				
				this.adapter.sendCommand(cmd);
				
				// progress 3
				this.adapter.sendCommand(new ProgressCommand(99));
				
				// send end
				cmd = new BlockCommand();
				cmd.setEof(true);
				
				this.adapter.sendCommandNotify(cmd, this.future);
			}
			else {
				cmd.setEof(true);
				
				this.adapter.sendCommandNotify(cmd, this.future);
			}
		}
		catch (Exception x) {
			System.out.println("Ctp-F Server error: " + x);
		}
		*/
		
		return ReturnOption.AWAIT;
	}

	@Override
	public void read() {
		// we are terminal, no downstream should call us
		OperationContext.get().getTaskRun().kill("File destination cannot be a source");
	}

	@Override
	public void execute() {
		// TODO optimize if upstream is local file 
		
		this.upstream.read();
	}

}
