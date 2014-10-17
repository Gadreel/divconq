package divconq.io.stream;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

import divconq.hub.Hub;
import divconq.interchange.FileSystemFile;
import divconq.interchange.IFileCollection;
import divconq.interchange.IFileStoreFile;
import divconq.lang.WrappedFuncCallback;
import divconq.script.StackEntry;
import divconq.work.TaskRun;
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
	public HandleReturn handle(TaskRun cb, StreamMessage msg) {
		// we are at top of stream, nothing to do here
		return HandleReturn.CONTINUE;
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
	public void request(TaskRun cb) {
		if (this.source == null) {
			this.downstream.handle(cb, StreamMessage.FINAL);
			return;
		}
		
		if (this.current == null) {
			this.source.next(new WrappedFuncCallback<IFileStoreFile>(cb) {
				@Override
				public void callback() {
					if (this.hasErrors()) {
						cb.kill();
						return;
					}
					
					FileSourceStream.this.readFile(cb, this.getResult());
				}
			});
		}
		// folders are handled in 1 msg, so we wouldn't get here in second or later call to a file
		else if (this.current instanceof FileSystemFile)
			FileSourceStream.this.readLocalFile(cb);
		else {
			FileSourceStream.this.readOtherFile(cb);
		}
	}
	
	public void readFile(TaskRun cb, IFileStoreFile file) {
		this.current = file;
		
		// if we reached the end of the collection then finish
		if (this.current == null) {
			this.downstream.handle(cb, StreamMessage.FINAL);
		}
		else if (this.current.isFolder()) {
	        StreamMessage fref = StreamMessage.fromFileStore(this.current);
	        fref.setIsFolder(true);
	        fref.setPath(this.current.path().subpath(this.source.path()));
	        
			if (this.downstream.handle(cb, fref) == HandleReturn.CONTINUE) {
				FileSourceStream.this.current = null;
				cb.resume();
			}
		}		
		else if (this.current instanceof FileSystemFile)
			FileSourceStream.this.readLocalFile(cb);
		else {
			FileSourceStream.this.readOtherFile(cb);
		}
	}
	
	public void readOtherFile(TaskRun cb) {
		// TODO abstract out so this class is a FileCollectionSourceStream and we
		// use it pull out the source streams of files, which we then use as if upstream from us
	}
	
	// release data if error
	public void readLocalFile(TaskRun cb) {
		FileSystemFile fs = (FileSystemFile) this.current;

		if (this.in == null) {
			this.insize = fs.getSize();
			
			// As a source we are responsible for progress tracking
			cb.setAmountCompleted(0);
			
			try {
				this.in = FileChannel.open(fs.localPath(), StandardOpenOption.READ);
			} 
			catch (IOException x) {
				cb.kill("Unable to read source file " + x);
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
				cb.kill("Problem reading source file: " + x1);
				data.release();
				return;
			}
	
	        StreamMessage fref = StreamMessage.fromFileStore(this.current);
	        fref.setPayload(data);
	        fref.setPath(this.current.path().subpath(this.source.path()).toString());
	        
	        System.out.println("writing: " + fref.getPath() + " from: " + this.inprog);
	        
	        if (pos == -1) {
	        	try {
					this.in.close();
				} 
	        	catch (IOException x) {
					cb.kill("Problem closing source file: " + x);
					data.release();
					return;
				}
	        	
	        	cb.setAmountCompleted(100);
	        	
		        fref.setEof(true);
	        	
	        	this.current = null;
	        	this.in = null;
	        	this.insize = 0;
	        	this.inprog  = 0;
	        }
	        else {
		        this.inprog += pos;
		        
		        data.writerIndex(pos);
		        cb.setAmountCompleted((int)(this.inprog * 100 / this.insize));
	        }
	        
	    	if (this.downstream.handle(cb, fref) != HandleReturn.CONTINUE)
	    		break;
	    	
	    	if (this.current == null) {
	    		// we need the next file
	    		cb.resume();
	    		
	    		// wait on the implied request
	    		break;
	    	}
		}
	}
}
