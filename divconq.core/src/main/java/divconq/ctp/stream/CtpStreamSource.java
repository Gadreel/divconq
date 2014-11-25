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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import io.netty.buffer.ByteBuf;
import divconq.ctp.CtpAdapter;
import divconq.ctp.f.BlockCommand;
import divconq.ctp.f.CtpFCommand;
import divconq.ctp.f.FileDescriptor;
import divconq.script.StackEntry;
import divconq.xml.XElement;

public class CtpStreamSource extends BaseStream implements IStreamSource {
	protected CtpAdapter adapter = null;
	protected boolean initialized = false;
	protected List<FileEntry> entries = new ArrayList<>();
	protected ReentrantLock entryLock = new ReentrantLock();
	protected FileDescriptor currFile = null;
	
	public CtpStreamSource(CtpAdapter adapter) {
		this.adapter = adapter;
	}

	@Override
	public void init(StackEntry stack, XElement el) {
	}
	
	public void setFinal() {
		this.entryLock.lock();
		
		try {
			this.currFile = null;
			this.entries.add(new FileEntry(FileDescriptor.FINAL, null));
		}
		finally {
			this.entryLock.unlock();
		}
	}
	
	public void addNext(BlockCommand cmd) {
		this.entryLock.lock();
		
		try {
			if (this.currFile == null)
				this.currFile = new FileDescriptor();
			
			this.currFile.copyAttributes(cmd);
			
			this.entries.add(new FileEntry(this.currFile, cmd.getData()));
			
			if (this.currFile.isEof())
				this.currFile = null;
		}
		finally {
			this.entryLock.unlock();
		}
	}
	
	@Override
	public ReturnOption handle(FileDescriptor file, ByteBuf data) {
		return null;
	}

	@Override
	public void read() {
		// if not initialized get the stream flowing by sending a READ
		if (!this.initialized) {
			this.initialized = true;
			
			try {
				this.adapter.sendCommand(CtpFCommand.STREAM_READ);
			} 
			catch (Exception x) {
				System.out.println("Error sending READ: " + x);
			}
		}
		else {
			// since only we remove, it is ok to check > 0 from here 
			// adding thread cannot add and call this at the same time
			// when they do call us, they'll catch any missed entries.
			while (this.entries.size() > 0) {
				FileEntry f = null;
				
				this.entryLock.lock();
				
				try {
					if (this.entries.size() > 0)
						f = this.entries.remove(0);
				}
				finally {
					this.entryLock.unlock();
				}
				
				if ((f != null) && (this.downstream.handle(f.file, f.data) != ReturnOption.CONTINUE)) 
					return;
			}
				
			// if no entries (left or to start with) then ask for more
			this.adapter.read();
		}
	}
	
	protected class FileEntry {
		protected FileDescriptor file = null;
		protected ByteBuf data = null;
		
		public FileEntry(FileDescriptor file, ByteBuf data) {
			this.file = file;
			this.data = data;
		}
	}
}
