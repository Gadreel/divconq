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
package divconq.interchange;

import io.netty.buffer.ByteBuf;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.joda.time.DateTime;

import divconq.bus.MessageUtil;
import divconq.bus.net.StreamMessage;
import divconq.hub.Hub;
import divconq.lang.FuncCallback;
import divconq.lang.FuncResult;
import divconq.lang.OperationCallback;
import divconq.lang.OperationContext;
import divconq.lang.OperationResult;
import divconq.log.Logger;
import divconq.script.StackEntry;
import divconq.session.DataStreamChannel;
import divconq.struct.RecordStruct;
import divconq.struct.ScalarStruct;
import divconq.struct.Struct;
import divconq.struct.scalar.NullStruct;
import divconq.struct.scalar.StringStruct;
import divconq.util.FileUtil;
import divconq.util.HashUtil;
import divconq.util.IOUtil;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class FileSystemFile extends RecordStruct implements IFileStoreFile {
	protected FileSystemDriver driver = null;
	protected Path localpath = null;
	protected FileInputStream input = null;
	protected long offset = 0;
		
	public FileSystemFile() {
		this.setType(Hub.instance.getSchema().getType("dciFileSystemFile"));
	}

	public FileSystemFile(FileSystemDriver driver, Path file) {
		this();
		
		this.driver = driver;
		this.localpath = file;
		
		refreshProps();
	}

	public FileSystemFile(FileSystemDriver driver, CommonPath file) {
		this();
		
		this.driver = driver;
		this.localpath = driver.resolveToLocalPath(file);
		
		refreshProps();
	}
	
    public FileSystemFile(FileSystemDriver driver, RecordStruct rec) {
		this();
		
		this.driver = driver;
		
		((RecordStruct) this).copyFields(rec);
		
		// only works with relative paths - even if my path is / it is considered relative to WF
		// which is good
		String cwd = driver.getFieldAsString("RootFolder");
		this.localpath = Paths.get(cwd, this.getFieldAsString("Path"));
		
		refreshProps();
	}
    
    public void refreshProps() {
		
		if (Files.exists(this.localpath)) {
			// ignore what the caller told us, these are the right values:
			this.setField("Name", this.localpath.getFileName());
			
			try {
				this.setField("Size", Files.size(this.localpath));
				this.setField("Modified", new DateTime(Files.getLastModifiedTime(this.localpath).toMillis()));
			} 
			catch (IOException x) {
			}
			
			this.setField("IsFolder", Files.isDirectory(this.localpath));
			this.setField("Exists", true);
						
			// TODO enhance for "absolute"
			String cwd = this.driver.getFieldAsString("RootFolder");
			String fpath = this.localpath.normalize().toString();
			
			// common path format in "absolute" relative to mount (TODO not relative to WF - fix instead relative to RootFolder)
			// also, since fpath may be absolute - only do substring thing if cwd is above fpath in folder chain TODO
			this.setField("Path", "/" + fpath.substring(cwd.length() + 1).replace('\\', '/'));
			this.setField("FullPath", fpath);
		}
    }
    
    @Override
    public boolean exists() {
    	return this.getFieldAsBooleanOrFalse("Exists");
    }

	@Override
	public String getName() {
		return this.getFieldAsString("Name");
	}

	@Override
	public String getPath() {
		return this.getFieldAsString("Path");
	}

	@Override
	public String getExtension() {
		return FileUtil.getFileExtension(this.getFieldAsString("Name"));
	}

	@Override
	public String getFullPath() {
		return this.getFieldAsString("FullPath");
	}

	@Override
	public DateTime getMofificationTime() {
		return this.getFieldAsDateTime("Modified");
	}

	@Override
	public long getSize() {
		return this.getFieldAsInteger("Size", 0);
	}

	@Override
	public boolean isFolder() {
		return this.getFieldAsBooleanOrFalse("IsFolder");
	}

	/*
	@Override
	public Iterable<Struct> getItems() {
		if (this.driver == null)
			return null;
		
		String cwd = this.driver.getFieldAsString("RootFolder");
		Boolean recursive = this.getFieldAsBoolean("Recursive");
		ListStruct match = this.getFieldAsList("MatchFiles");

		List<String> wildcards = new ArrayList<String>();
		
		if (match != null) 
			for (Struct s : match.getItems()) 
				wildcards.add(((StringStruct)s).getValue());
		
		// see AndFileFilter and OrFileFilter
		IOFileFilter filefilter = new WildcardFileFilter(wildcards);
		
		// TODO support more options, size/date, folder filter
		return new Matches(new File(cwd), filefilter, 
				((recursive != null) && recursive) ? TrueFileFilter.TRUE : FalseFileFilter.FALSE);		
	}
	*/

	@Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	FileSystemFile nn = (FileSystemFile)n;
		nn.driver = this.driver;
    }
    
	@Override
	public Struct deepCopy() {
		FileSystemFile cp = new FileSystemFile();
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	public void dispose() {
		// TODO support this!!!
		super.dispose();
	}

	@Override
	public FuncResult<Struct> getOrAllocateField(String name) {
		if ("TextReader".equals(name)) {
			FuncResult<Struct> res = new FuncResult<Struct>();
			res.setResult(new FileSystemTextReader(this));
			return res;
		}
		
		return super.getOrAllocateField(name);
	}
	
	@Override
	public void operation(final StackEntry stack, XElement code) {
		if ("Hash".equals(code.getName())) {
			String meth = stack.stringFromElement(code, "Method");
			
	        final Struct var = stack.refFromElement(code, "Target");

			if (var instanceof ScalarStruct) { 				
				this.hash(meth, new FuncCallback<String>() {					
					@Override
					public void callback() {
						((ScalarStruct)var).adaptValue(this.getResult());
						stack.resume();
					}
				});
				
				return;
			}
			else {
				stack.log().error(1, "Invalid hash target!");
			}
			
			stack.resume();
			return;
		}
		
		if ("Rename".equals(code.getName())) {
			String val = stack.stringFromElement(code, "Value");
			
			// TODO support other methods
			if (StringUtil.isEmpty(val)) {
				// TODO log
				stack.resume();
				return;
			}
			
			Path dest = this.localpath.getParent().resolve(val);
			
			try {
				Files.move(this.localpath, dest);
				
				this.localpath = dest;
				this.refreshProps();
			} 
			catch (IOException x) {
				// TODO catch?
			}
			
			stack.resume();
			return;
		}

		// this is kind of a hack - may want to re-evaluate this later
		// used by NCC provisioning
		if ("WriteText".equals(code.getName())) {
			String text = code.getText();
			
	        Struct content = StringUtil.isNotEmpty(text) 
	        		? stack.resolveValue(text)
	        		: stack.refFromElement(code, "Target");
	        
	        if (content != null) {
	        	OperationResult or = IOUtil.saveEntireFile(this.localpath, Struct.objectToString(content));
	        	
	        	stack.log().copyMessages(or);
	        	
	        	this.refreshProps();
	        }
		
			stack.resume();
			return;
		}

		// this is kind of a hack - may want to re-evaluate this later
		// used by NCC provisioning
		if ("ReadText".equals(code.getName())) {
			if (this.getFieldAsBooleanOrFalse("Exists")) {
		        final Struct var = stack.refFromElement(code, "Target");
	
		        //System.out.println("e: " + var);
		        
				if (var instanceof NullStruct) {					
			        String handle = stack.stringFromElement(code, "Handle");

					if (handle != null) 
			            stack.addVariable(handle, new StringStruct(IOUtil.readEntireFile(this.localpath.toFile())));
					
					// TODO log
				}			
				else if (var instanceof ScalarStruct) {					
					((ScalarStruct)var).adaptValue(IOUtil.readEntireFile(this.localpath.toFile()));
				}
				else {
					// TODO log
				}
			}
			
			stack.resume();
			return;
		}

		if ("Delete".equals(code.getName())) {
			try {
				Files.deleteIfExists(this.localpath);
			} 
			catch (IOException x) {
				// TODO Auto-generated catch block
			}
			
	    	this.refreshProps();
	    	
			stack.resume();
			return;
		}
		
		/*
		if ("ScanFilter".equals(code.getName())) {
			String path = stack.stringFromElement(code, "Path");
			
			...
			
			if (StringUtil.isEmpty(path)) {
				// TODO log
				stack.resume();
				return;
			}
			
			this.cwd = new File(path);
			
			stack.resume();
			return;
		}
		*/
		
		super.operation(stack, code);
	}

	// TODO use DataStreamChannel instead
	@Override
	public void copyTo(OutputStream out, OperationCallback callback) {
		try {
			Files.copy(this.localpath, out);
			out.flush();
			out.close();
		} 
		catch (IOException x) {
			callback.error(1, "Unable to write file");		// TODO codes
		}		
		finally {
            IOUtil.closeQuietly(out);			
		}
		
		callback.completed();
	}
	
	public class DestinationDriver implements IFileStoreStreamDriver {						
		protected FileChannel fchannel = null;
		protected DataStreamChannel channel = null;
		protected Path file = null;
		protected ReentrantLock accesslock = new ReentrantLock();
		
		protected CommonPath path = null;
		protected long expectedsize = 0;
		protected long writtensize = 0;

		/**
		 * At this point we don't have a channel yet, so we don't need to communicate any errors to a channel in this method
		 */
		@Override
		public void init(DataStreamChannel channel, OperationCallback or) {
			this.channel = channel;
			
			RecordStruct rec = channel.getBinding();
			
			this.path = new CommonPath(rec.getFieldAsString("FilePath"));
			this.expectedsize = rec.getFieldAsInteger("FileSize", 0);
			
			if (!this.path.isAbsolute()) {
				or.error(1, "Path must be absolute");
				or.completed();
				return;
			}
			
			this.file = FileSystemFile.this.driver.resolveToLocalPath(this.path);
			
			boolean exists = Files.exists(this.file);
			or.info("Opening " + this.file + " for write - check exists: " + exists);
			
			OperationResult mdres = FileUtil.confirmOrCreateDir(this.file.getParent());
			
			or.copyMessages(mdres);
			
			if (mdres.hasErrors()) {
	        	or.error("FS failed to open file: " + this.file);
	        	or.completed();				
			}
			
	        try {
				boolean append = rec.getFieldAsBooleanOrFalse("Append");
	        	
	        	if (append && exists) {
	        		//or.info("Appending to " + this.file + " initial result for size: " + Files.size(this.file));
	        		
	        		// TODO maybe put the local locking back in we had before? - useful here and for integrity checks - not useful in distributed deployment?
	        		// experience shows that we are not always getting the correct size, if we wait a but maybe it will flush out? 
	        		Thread.sleep(5000);
	        		
	        		this.fchannel = FileChannel.open(this.file, StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
	        		
	        		// this should be a reliable way to get position - better than Files.size hopefully
	        		long size = this.fchannel.position();
	        		
	        		or.info("Appending to " + this.file + " current size: " + size);
	        		FileSystemFile.this.setField("Size", size);
	        		
	        		if (this.expectedsize < size) {
	        			this.fchannel.close();
	        			or.error("File size exceeds the Expected Size");
	        			return;
	        		}
	        		
	        		if (this.expectedsize == size) 
	        			or.warn("Resume attempted on an already completed upload.  File size and Expected Size match.");
	        	}
	        	else {
	        		// better than delete - you never know when the delete will complete, but this will do it all - remove and then write
	        		this.fchannel = FileChannel.open(this.file, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
	        		FileSystemFile.this.setField("Size", 0);
	        	}
	        } 
	        catch (IOException x) {
	        	or.error(1, "FS failed to open file: " + x);
	        }
	        catch (InterruptedException x) {
	        	or.error(1, "FS failed to open file: " + x);
	        }
	        
			or.completed();
		}
		
		@Override
		public void nextChunk() {
			// ignore, meaningless
		}
		
		@Override
		public void message(StreamMessage msg) {
			this.channel.touch();
			
	    	// TODO fill in this.channel.setProgress
	    	
	    	// would only happen if message after Final or after cancel
	    	if (this.fchannel == null) {
	    		this.channel.error(1, "Got message after final or cancel");
            	msg.release();
	    		return;
	    	}
	    	
	    	if (msg.hasData()) {
	            ByteBuf bb = msg.getData(); 
	    		
	    		if (bb.nioBufferCount() > 0) {
	    			// discourage close during write - just in case there is an abort/melt-down
	    			this.accesslock.lock();
	    			
					try {
						this.writtensize += this.fchannel.write(bb.nioBuffers());

						if (this.expectedsize > 0)
							this.channel.setAmountCompleted((int)(this.writtensize * 100 / this.expectedsize));
					} 
	    			catch (IOException x) {
	    	    		this.channel.error(1, "Error writing file");
	    				this.channel.send(MessageUtil.streamError(1, "File write error!"));
	    	    		this.flushClose("UploadError");
					}
					finally {
						this.accesslock.unlock();
					}
	    		}
	    	}
	    	
	    	if (msg.isFinal()) {
	    		// let them know we ended as expected
				this.channel.send(MessageUtil.streamFinal());
				
				// must come after we send last message, closing will remove the channel
	    		this.flushClose("UploadComplete");				
	    	}
			
	    	// now we are done with the buffer, if any
        	msg.release();
		}
		
		public void flushClose(String event) {
			this.accesslock.lock();
			
			try {
		    	// we are done with the file
		    	if (this.fchannel != null)
					try {
						this.fchannel.force(true);
						this.fchannel.close();
					} 
		    		catch (IOException x) {
		    			Logger.error("Destination driver unable to close file " + this.file + " error: " + x);
					}
			}
			finally {
		    	this.fchannel = null;
		    	
				this.accesslock.unlock();
			}
			
			this.channel.complete();
		}

		@Override
		public void cancel() {
			this.flushClose("UploadError");
		}
	}	
	
	public class SourceDriver implements IFileStoreStreamDriver {			
		/**
		 * 
		 */
		protected AsynchronousFileChannel sbc = null;
		protected DataStreamChannel channel = null;
		protected Path file = null;
		protected ReentrantLock closelock = new ReentrantLock();
		protected CommonPath path = null;
		protected long offset= 0;

		@Override
		public void init(DataStreamChannel channel, OperationCallback or) {
			this.channel = channel;
			
			RecordStruct rec = channel.getBinding();
			
			this.path = new CommonPath(rec.getFieldAsString("FilePath"));
			this.offset = rec.getFieldAsInteger("Offset", 0);
			
			if (!this.path.isAbsolute()) {
				or.error(1, "Path must be absolute");
				or.completed();
				return;
			}
			
			this.file = FileSystemFile.this.driver.resolveToLocalPath(this.path);
			
	        if (!Files.exists(this.file)) {
	    		or.error(1, "FS failed to find file: " + this.file);
				or.completed();
				return;
	        }
	        
	        if (Files.isDirectory(this.file)) {
	        	or.error(1, "FS found directory: " + this.file);
				or.completed();
				return;
	        }
	        
			or.info("Opening " + this.file + " for read");
	        
	        try {
	        	this.sbc = AsynchronousFileChannel.open(this.file);
	        	
	        	// TODO skip to offset
	        } 
	        catch (IOException x) {
	        	or.error(1, "FS failed to open file: " + x);
	        }
			
			or.completed();
		}
		
		@Override
		public void nextChunk() {
			// ignore, meaningless
		}
		
		@Override
		public void message(final StreamMessage msg) {
	    	if (this.sbc == null) {
	    		this.channel.error(1, "Got message after final or cancel");
	    		return;
	    	}
	        
	    	if (!msg.isStart()) {
	    		this.channel.error(1, "Got message other than Start - expected Start");
	    		SourceDriver.this.channel.send(MessageUtil.streamError(1, "Invalid request - channel cancelled!"));
	            this.channel.close();
	    		return;
	    	}
	    	
	        try {
	            final ByteBuffer buf = ByteBuffer.allocate(64 * 1024);
	            final long fsize = Files.size(this.file);
				final AtomicLong amtleft = new AtomicLong(fsize - this.offset);

				this.sbc.read(buf, 0, this.sbc, new CompletionHandler<Integer, AsynchronousFileChannel>() {
	            	long pos = 0;
	            	long seq = 0;
	            	
					@Override
					public void completed(Integer result, AsynchronousFileChannel sbc) {
			    		if (SourceDriver.this.channel.isClosed())
			    			return;
						
			    		SourceDriver.this.channel.touch();
						
						if (result == -1) {
							SourceDriver.this.flushClose("DownloadComplete");
							SourceDriver.this.channel.info(0, "File sent!!");
							
							return;
						}
				    	
						SourceDriver.this.channel.setAmountCompleted((int)((fsize - amtleft.get()) * 100 / fsize));

						if (result > 0) {
							this.pos += result;
				            
							amtleft.getAndAdd(-result);
							
							StreamMessage b = new StreamMessage(amtleft.get() <= 0 ? "Final" : "Block", buf);
							b.setField("Sequence", seq);
							
							OperationResult sr = SourceDriver.this.channel.send(b);
					        
							if (sr.hasErrors()) {
								SourceDriver.this.flushClose("DownloadError");
								SourceDriver.this.channel.info(0, "File sending aborted!!");
								
								return;
							}
							
							seq++;
					        buf.clear();
						}
						
						// TODO add throttling options - put the read in "future" schedule
						sbc.read(buf, this.pos, sbc, this);
					}

					@Override
					public void failed(Throwable x, AsynchronousFileChannel sbc) {
						SourceDriver.this.channel.error(1, "Server Stream failed to read file: " + x);		            
						SourceDriver.this.channel.send(MessageUtil.streamError(1, "File download read error!"));		            
						SourceDriver.this.channel.abort();		
						
						// cancel will be triggered by abort, don't close here
					}
				});
			} 
	        catch (IOException x) {
	        	SourceDriver.this.channel.error(1, "Server Stream failed to read file: " + x);            
	        	SourceDriver.this.channel.send(MessageUtil.streamError(1, "File download read error!"));            
	        	SourceDriver.this.channel.abort();						
				
				// cancel will be triggered by abort, don't close here
			}
		}
		
		@Override
		public void cancel() {
	        this.flushClose("DownloadError");
		}
		
		public void flushClose(String event) {
			this.closelock.lock();
			
			try {
		    	// we are done with the file
		    	if (this.sbc != null)
					try {
						this.sbc.close();
					} 
		    		catch (IOException x) {
		    			SourceDriver.this.channel.error("Source driver unable to close file " + this.file + " error: " + x);
					}
			}
			finally {
		    	this.sbc = null;
		    	
				this.closelock.unlock();
			}
			
			this.channel.complete();
		}
	}

	@Override
	public void openRead(final DataStreamChannel channel, final FuncCallback<RecordStruct> callback) {
		final SourceDriver d = new SourceDriver();
		
		d.init(channel, new OperationCallback() {			
			@Override
			public void callback() {
				callback.copyMessages(this);
				
				if (!this.hasErrors()) {
					channel.setDriver(d);
					
					RecordStruct resp = new RecordStruct();
					resp.setField("Hub", OperationContext.getHubId());
					resp.setField("Session", channel.getSessionId());
					resp.setField("Channel", channel.getId());
					resp.setField("Size", FileSystemFile.this.getFieldAsInteger("Size"));
					
					callback.setResult(resp);
				}
				
				callback.completed();
			}
		});
	}
	
	@Override
	public void openWrite(final DataStreamChannel channel, final FuncCallback<RecordStruct> callback) {
		try {
			Files.createDirectories(this.localpath.getParent());
		} 
		catch (IOException x) {
			callback.error(1, "Unable to create destination folder path: " + x);
			callback.completed();
			return;
		}
		
		final DestinationDriver d = new DestinationDriver();
		
		d.init(channel, new OperationCallback() {			
			@Override
			public void callback() {
				callback.copyMessages(this);
				
				// we cannot get reliable size info this way because windows is sometimes too slow
				// about reporting file size.  we need to get file size instead by allowing dest driver 
				// to set size for us
				//FileSystemFile.this.refreshProps();
				
				if (!this.hasErrors()) {
					channel.setDriver(d);
					
					RecordStruct resp = new RecordStruct();
					resp.setField("Hub", OperationContext.getHubId());
					resp.setField("Session", channel.getSessionId());
					resp.setField("Channel", channel.getId());
					resp.setField("Size", FileSystemFile.this.getFieldAsInteger("Size"));
					
					callback.setResult(resp);
				}
				
				callback.completed();
			}
		});
	}

	@Override
	public void hash(String method, FuncCallback<String> callback) {
		try {
			FuncResult<String> res = HashUtil.hash(method, Files.newInputStream(this.localpath));
			callback.copyMessages(res); 
			
			if (!res.hasErrors())
				callback.setResult(res.getResult());
		}
		catch (Exception x) {
			callback.error(1, "Unable to read file for hash: " + x);
		}
		
		callback.completed();
	}

	// TODO use DataStreamChannel instead
	@Override
	public void getInputStream(FuncCallback<InputStream> callback) {
		try {
			callback.setResult(Files.newInputStream(this.localpath));
		}
		catch (Exception x) {
			// TODO log
		}
		
		callback.completed();
	}

	@Override
	public void rename(String name, OperationCallback callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void remove(OperationCallback callback) {
		if (this.exists()) {
			if (this.isFolder()) {
				FileUtil.deleteDirectory(callback, this.localpath);
			}
			else  {
				try {
					Files.delete(this.localpath);
				}
				catch (Exception x) {
					callback.error("Unable to remove file: " + this.getPath() + " - Error: " + x);
				}
			}
		}
		
		callback.completed();
	}

	@Override
	public void setModificationTime(DateTime time, OperationCallback callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getAttribute(String name, FuncCallback<Struct> callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAttribute(String name, Struct value,
			OperationCallback callback) {
		// TODO Auto-generated method stub
		
	}
}
