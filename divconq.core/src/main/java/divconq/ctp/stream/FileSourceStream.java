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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

import divconq.ctp.f.FileDescriptor;
import divconq.filestore.IFileCollection;
import divconq.filestore.IFileStoreFile;
import divconq.filestore.local.FileSystemFile;
import divconq.hub.Hub;
import divconq.lang.op.FuncCallback;
import divconq.lang.op.OperationContext;
import divconq.script.StackEntry;
import divconq.xml.XElement;

public class FileSourceStream extends BaseStream implements IStreamSource {
	protected IFileCollection source = null;	
	protected IFileStoreFile current = null;
	protected FileChannel in = null;
	protected long insize = 0;
	protected long inprog = 0;
	
	public FileSourceStream(IFileCollection src) {
		this.source = src;
	}

	// for use with dcScript
	@Override
	public void init(StackEntry stack, XElement el) {
		// anything we need to gleam from the xml?
	}

	@Override
	public ReturnOption handle(FileDescriptor file, ByteBuf data) {
		// we are at top of stream, nothing to do here
		return ReturnOption.CONTINUE;
	}
	
	@Override
	public void close() {
		//System.out.println("File SRC killed");	// TODO
		
		if (this.in != null)
			try {
				this.in.close();
			} 
			catch (IOException x) {
			}
		
		this.in = null;
		this.current = null;
		this.source = null;
		
		super.close();
	}

	/**
	 * Someone downstream wants more data
	 */
	@Override
	public void read() {
		if (this.source == null) {
			this.downstream.handle(FileDescriptor.FINAL, null);
			return;
		}
		
		if (this.current == null) {
			this.source.next(new FuncCallback<IFileStoreFile>() {
				@Override
				public void callback() {
					if (this.hasErrors()) {
						OperationContext.get().getTaskRun().kill();
						return;
					}
					
					FileSourceStream.this.readFile(this.getResult());
				}
			});
		}
		// folders are handled in 1 msg, so we wouldn't get here in second or later call to a file
		else if (this.current instanceof FileSystemFile)
			FileSourceStream.this.readLocalFile();
		else {
			FileSourceStream.this.readOtherFile();
		}
	}
	
	public void readFile(IFileStoreFile file) {
		this.current = file;
		
		// if we reached the end of the collection then finish
		if (this.current == null) {
			this.downstream.handle(FileDescriptor.FINAL, null);
		}
		else if (this.current.isFolder()) {
			FileDescriptor fref = FileDescriptor.fromFileStore(this.current);
	        fref.setIsFolder(true);
	        fref.setPath(this.current.path().subpath(this.source.path()));
	        
			if (this.downstream.handle(fref, null) == ReturnOption.CONTINUE) {
				FileSourceStream.this.current = null;
				OperationContext.get().getTaskRun().resume();
			}
		}		
		else if (this.current instanceof FileSystemFile)
			FileSourceStream.this.readLocalFile();
		else {
			FileSourceStream.this.readOtherFile();
		}
	}
	
	public void readOtherFile() {
		// TODO abstract out so this class is a FileCollectionSourceStream and we
		// use it pull out the source streams of files, which we then use as if upstream from us
	}
	
	// release data if error
	public void readLocalFile() {
		FileSystemFile fs = (FileSystemFile) this.current;

		if (this.in == null) {
			this.insize = fs.getSize();
			
			// As a source we are responsible for progress tracking
			OperationContext.get().setAmountCompleted(0);
			
			try {
				this.in = FileChannel.open(fs.localPath(), StandardOpenOption.READ);
			} 
			catch (IOException x) {
				OperationContext.get().getTaskRun().kill("Unable to read source file " + x);
				return;
			}
		}
		
		while (true) {
			// TODO sizing?
	        ByteBuf data = Hub.instance.getBufferAllocator().heapBuffer(32768);
			
	        ByteBuffer buffer = ByteBuffer.wrap(data.array(), data.arrayOffset(), data.capacity());
	        
	        int pos = -1;
			
	        try {
				pos = (int)this.in.read(buffer);
			} 
			catch (IOException x1) {
				OperationContext.get().getTaskRun().kill("Problem reading source file: " + x1);
				data.release();
				return;
			}
	
	        FileDescriptor fref = FileDescriptor.fromFileStore(this.current);
	        fref.setPath(this.current.path().subpath(this.source.path()));
	        
	        System.out.println("writing: " + fref.getPath() + " from: " + this.inprog);
	        
	        if (pos == -1) {
	        	try {
					this.in.close();
				} 
	        	catch (IOException x) {
	        		OperationContext.get().getTaskRun().kill("Problem closing source file: " + x);
					data.release();
					return;
				}
	        	
	        	OperationContext.get().setAmountCompleted(100);
	        	
		        fref.setEof(true);
	        	
	        	this.current = null;
	        	this.in = null;
	        	this.insize = 0;
	        	this.inprog  = 0;
	        }
	        else {
		        this.inprog += pos;
		        
		        data.writerIndex(pos);
		        OperationContext.get().setAmountCompleted((int)(this.inprog * 100 / this.insize));
	        }
	        
	    	if (this.downstream.handle(fref, data) != ReturnOption.CONTINUE)
	    		break;
	    	
	    	if (this.current == null) {
	    		// we need the next file
	    		OperationContext.get().getTaskRun().resume();
	    		
	    		// wait on the implied request
	    		break;
	    	}
		}
	}
}
