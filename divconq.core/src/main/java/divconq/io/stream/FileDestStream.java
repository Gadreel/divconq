package divconq.io.stream;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import divconq.interchange.FileSystemFile;
import divconq.interchange.IFileStoreDriver;
import divconq.interchange.IFileStoreFile;
import divconq.script.StackEntry;
import divconq.struct.Struct;
import divconq.struct.scalar.NullStruct;
import divconq.work.TaskRun;
import divconq.xml.XElement;

public class FileDestStream extends BaseStream implements IStreamDest {
	protected FileSystemFile file = null;
	protected FileChannel out = null;
	protected boolean userelpath = false;
	protected String relpath = "";

	public FileDestStream(FileSystemFile file) {
		this.file = file;
	}

	public FileDestStream withRelative(boolean v) {
		this.userelpath = v;
		return this;
	}
	
	// for use with dcScript
	@Override
	public void init(StackEntry stack, XElement el, boolean autorelative) {
		if (autorelative || stack.boolFromElement(el, "Relative", false) || el.getName().startsWith("X")) {
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
		//System.out.println("File DEST killed");	// TODO
		
		if (this.out != null)
			try {
				this.out.close();
			} 
			catch (IOException x) {
			}
		
		this.out = null;
		this.file = null;
		
		super.close();
	}
	
	// make sure we don't return without first releasing the file reference content
	@Override
	public HandleReturn handle(TaskRun cb, StreamMessage msg) {
		if (msg == StreamMessage.FINAL) {
			cb.complete();
			return HandleReturn.DONE;
		}
		
		if (this.file.isFolder())
			return this.handleLocalFolder(cb, msg);
		
		return this.handleLocalFile(cb, msg);
	}
	
	public HandleReturn handleLocalFile(TaskRun cb, StreamMessage msg) {
		ByteBuf data = msg.getPayload();
		
		if (msg.isFolder()) {
			if (data != null)
				data.release();
			
			cb.kill("Folder cannot be stored into a file");
			return HandleReturn.DONE;
		}
		
		if (data != null) {
			if (this.out == null) {
				try {
					Path dpath = this.file.localPath();
					
					Files.createDirectories(dpath.getParent());
					
					this.out = FileChannel.open(dpath, 
							StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
				} 
				catch (IOException x) {
					if (data != null)
						data.release();
					
					cb.kill("Problem opening destination file: " + x);
					return HandleReturn.DONE;
				}
			}
			
			for (ByteBuffer buff : data.nioBuffers()) {
				try {
					this.out.write(buff);
				} 
				catch (IOException x) {
					data.release();
					cb.kill("Problem writing destination file: " + x);
					return HandleReturn.DONE;
				}
			}
		
			data.release();
		}
		
		if (msg.isEof()) {
			try {
				if (this.out != null) {
					this.out.close();
					this.out = null;
				}
				
				this.file.refreshProps();
			} 
			catch (IOException x) {
				cb.kill("Problem closing destination file: " + x);
				return HandleReturn.DONE;
			}
		}
		
		return HandleReturn.CONTINUE;
	}
	
	public HandleReturn handleLocalFolder(TaskRun cb, StreamMessage msg) {
		ByteBuf data = msg.getPayload();
		
		Path folder = this.file.localPath();
		
		if (Files.notExists(folder))
			try {
				Files.createDirectories(folder); 
			} 
			catch (IOException x) {
				if (data != null)
					data.release();
				
				cb.kill("Problem making destination top folder: " + x);
				return HandleReturn.DONE;
			}
		
		String fpath = (this.userelpath) ? this.relpath + msg.getPath() : "/" + msg.getPath().getFileName();
		
		if (msg.isFolder()) {
			try {
				Files.createDirectories(folder.resolve(fpath.substring(1))); 
			} 
			catch (IOException x) {
				if (data != null)
					data.release();
				
				cb.kill("Problem making destination folder: " + x);
				return HandleReturn.DONE;
			}
			
			return HandleReturn.CONTINUE;
		}

		if (this.out == null)
			try {
				Path dpath = folder.resolve(fpath.substring(1));
				
				Files.createDirectories(dpath.getParent());
				
				this.out = FileChannel.open(dpath, 
						StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
			} 
			catch (IOException x) {
				if (data != null)
					data.release();
				
				cb.kill("Problem opening destination file: " + x);
				return HandleReturn.DONE;
			}
		
		if (data != null) {
			for (ByteBuffer buff : data.nioBuffers()) {
				try {
					this.out.write(buff);
				} 
				catch (IOException x) {
					data.release();
					cb.kill("Problem writing destination file: " + x);
					return HandleReturn.DONE;
				}
			}
			
			data.release();
		}
		
		if (msg.isEof()) {
			try {
				this.out.close();
				this.out = null;
				
				this.file.refreshProps();
			} 
			catch (IOException x) {
				cb.kill("Problem closing destination file: " + x);
				return HandleReturn.DONE;
			}
		}
		
		return HandleReturn.CONTINUE;
	}

	@Override
	public void request(TaskRun cb) {
		// we are terminal, no downstream should call us
		cb.kill("File destination cannot be a source");
	}

	@Override
	public void execute(TaskRun cb) {
		// TODO optimize if upstream is local file also
		
		this.upstream.request(cb);
	}
}
