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
package divconq.interchange.s3;

public class S3File { /*extends RecordStruct implements IFileStoreFile {
	protected S3Driver driver = null;
	//protected LsEntry file = null;
	protected FileInputStream input = null;
	protected long offset = 0;
	
	// TODO check for removed before doing operations requiring the file
	protected boolean removed = false;
		
	public S3File() {
		this.setType(Hub.instance.getSchema().getType("dciS3File"));
	}
	
    public S3File(S3Driver driver, RecordStruct rec) {
		this();
		
		this.driver = driver;
		
		this.copyFields(rec);
		
		//String cwd = driver.getFieldAsString("RootFolder");
		//this.file = new File(cwd + "/" + this.getFieldAsString("Path"));
	}

	/*
	public S3File(S3Driver driver, LsEntry file, String fullpath) {
		this();
		
		this.driver = driver;
		this.file = file;
		
		SftpATTRS attrs = file.getAttrs();
		
		this.setField("Name", file.getFilename());
		this.setField("Size", attrs.getSize());
		this.setField("Modified", new DateTime(((long) attrs.getMTime()) * 1000));
		
		String cwd = driver.getFieldAsString("RootFolder");
		
		this.setField("Path", fullpath.substring(cwd.length() + 1).replace('\\', '/'));
		this.setField("FullPath", fullpath);
		this.setField("IsFolder", file.getAttrs().isDir());
	}
	*/
	
	/*
    public SftpFile(SftpDriver driver, RecordStruct rec) {
		this();
		
		this.driver = driver;
		
		((RecordStruct) this).copyFields(rec);
		
		String cwd = driver.getFieldAsString("RootFolder");
		this.file = new File(cwd + "/" + this.getFieldAsString("Path"));
		
		if (this.file.exists()) {
			// ignore what the caller told us, these are the right values:
			this.setField("Name", this.file.getName());
			this.setField("Size", this.file.length());
			this.setField("Modified", new DateTime(this.file.lastModified()));
			this.setField("IsFolder", this.file.isDirectory());
		}
	}
	* /

	@Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	S3File nn = (S3File)n;
		nn.driver = this.driver;
    }
    
	@Override
	public Struct deepCopy() {
		S3File cp = new S3File();
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
		if ("TextReader".equals(name) && !this.removed) {
			FuncResult<Struct> res = new FuncResult<Struct>();
			res.setResult(new S3TextReader(this));
			return res;
		}
		
		return super.getOrAllocateField(name);
	}
	
	/*
	@Override
	public void toBuilder(ICompositeBuilder builder) throws BuilderStateException {
		builder.startRecord();
		
		for (FieldStruct f : this.fields.values()) 
			f.toBuilder(builder);
		
		// TODO add in FS specific fields
		
		builder.endRecord();
	}
	
	@Override
	public Struct select(PathPart... path) {
		if (path.length > 0) {
			PathPart part = path[0];
			
			if (part.isField()) {			
				String fld = part.getField();
				
				if ("Scanner".equals(fld))
					return this.search;
			}			
		}
		
		return super.select(path);
	}
	* /
	
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
				// TODO log
			}
			
			stack.resume();
			return;
		}
		
		super.operation(stack, code);
	}

	@Override
	public void copyTo(final OutputStream out, final OperationCallback callback) {
		this.getInputStream(new FuncCallback<InputStream>() {			
			@Override
			public void callback() {
				// TODO check for errors
				
				InputStream in = this.getResult();
				
				try {
					IOUtils.copy(in, out);
				} 
				catch (IOException x) {
					callback.error(1, "Unable to write file");		// TODO codes
				}
				finally {
		            IOUtils.closeQuietly(in);
		            IOUtils.closeQuietly(out);
				}
				
				callback.completed();
			}
		});
	}
	
	public class DestinationDriver implements IDestinationDriver {						
		protected FileOutputStream out = null;
		protected ISessionStream source = null;
		protected Session session = null;
		
		@Override
		public void setSource(ISessionStream v) {
			this.source = v;
		}
		
		public DestinationDriver(Session session) {
			this.session = session;
			
			/* TODO
			try {
				this.out = new FileOutputStream(SftpFile.this.file);
			} 
			catch (FileNotFoundException x) {
				// TODO 
				System.out.println("Error while creating consumer");
			}
			* /
		}

		@Override
		public void present(Block data) {
			try {
				if (data.isAbort()) {
					this.session.getAdapter().releaseSource();
					this.source = null;
					System.out.println("Error while uploading, aborted!");		// TODO
					this.out.close();
					return;
				}
				
				Memory mem = data.getData();
				
				if (mem != null)
					this.out.write(mem.toArray());

				if (data.isDone()) {
					this.out.flush();
					this.out.close();
				}
				
				/* TODO - this should be in here in some way #NETTY
				Message rmsg = new Message();
				rmsg.setField("Op", data.isDone() ? "Done" : "Next");
				this.source.reaction(rmsg);		// give me more
				* /

				if (data.isDone()) {
					this.session.getAdapter().releaseSource();
					this.source = null;
				}
			} 
			catch (IOException x) {
				// TODO 
				System.out.println("Error while uploading");
			}
		}
	}	
	
	public class SourceDriver implements ISourceDriver {						
		protected FileInputStream in = null;
		protected ISessionStream dest = null;
		protected long offset = 0;
		
		@Override
		public void setDestination(ISessionStream v) {
			this.dest = v;
		}
		
		public SourceDriver(Session session) {
			/* TODO
			try {
				this.in = new FileInputStream(SftpFile.this.file);
			} 
			catch (FileNotFoundException x) {
				// TODO 
				System.out.println("Error while creating consumer");
			}
			* /
		}

		@Override
		public void collect(FuncCallback<Block> callback) {
			try {
				Block b = new Block();
				
				Memory mem = new Memory(16384);		// TODO config 
				b.setData(mem);
				b.setOffset(this.offset);

				int amt = mem.copyFromStream(this.in, 16384);

				if (amt < 16384) {
					b.setDone(true);
					this.in.close();
				}

				this.offset += amt;
				
				mem.setPosition(0);
				
				callback.setResult(b);
			} 
			catch (IOException x) {
				// TODO 
				System.out.println("Error while downloading");
			}
			
			callback.completed();
		}

		@Override
		public void abort() {
			try {
				this.in.close();
			} 
			catch (IOException x) {
				// TODO 
				System.out.println("Error while downloading");
			}
		}
	}

	@Override
	public void openRead(Session session, final FuncCallback<ISourceDriver> callback) {
		callback.setResult(new SourceDriver(session));
		callback.completed();
	}	

	@Override
	public void openWrite(Session session, final FuncCallback<IDestinationDriver> callback) {
		// TODO this.file.getParentFile().mkdirs();
		
		callback.setResult(new DestinationDriver(session));
		callback.completed();
	}

	@Override
	public void hash(String method, FuncCallback<String> callback) {
		/* TODO
		try {
			// TODO support something other than Sha2 - see params
			callback.setResult(HashUtil.getSha2(new FileInputStream(this.file)));
		}
		catch (Exception x) {
			// TODO log
		}
		* /
		
		callback.completed();
	}

	@Override
	public void getInputStream(final FuncCallback<InputStream> callback) {
		if (this.removed) {
			// TODO log
			
			callback.completed();
			return;
		}

		/* TODO
		S3File.this.driver.getChannel(new FuncCallback<ChannelSftp>() {						
			@Override
			public void callback() {
				ChannelSftp channel = this.getResult();
				
				try {
					callback.setResult(channel.get(S3File.this.getFieldAsString("FullPath")));
				}
				catch (Exception x) {
					// TODO log
				}
				finally {
					S3File.this.driver.releaseChannel(channel);
				}
				
				callback.completed();
			}
		});		
		* /
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
		String ext = this.getFieldAsString("Name");
		
		int pos = ext.lastIndexOf('.');
		
		if (pos > 0)
			ext = ext.substring(pos + 1);
		
		return ext;
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
		return this.getFieldAsInteger("Size");
	}

	@Override
	public boolean isFolder() {
		return this.getFieldAsBoolean("IsFolder");
	}

	// is not a move
	@Override
	public void rename(final String name, final OperationCallback callback) {
		if (this.removed) {
			// TODO log
			callback.completed();
			return;
		}
		
		/* TODO
		S3File.this.driver.getChannel(new FuncCallback<ChannelSftp>() {						
			@Override
			public void callback() {
				ChannelSftp channel = this.getResult();
				
				// TODO check for '/' or '.' or '..' - only allow a name here not a path or a move
				
				try {
					String newpath = S3File.this.getFieldAsString("FullPath");
					
					int pos = newpath.lastIndexOf('/');
					
					if (pos > 0)
						newpath = newpath.substring(0, pos);
					
					if (newpath.length() > 1)
						newpath += "/";
					
					newpath += name;
					
					channel.rename(S3File.this.getFieldAsString("FullPath"), newpath);
					
					S3File.this.setField("Name", name);
					S3File.this.setField("FullPath", newpath);
					
					newpath = S3File.this.getFieldAsString("Path");
					
					pos = newpath.lastIndexOf('/');
					
					if (pos > 0)
						newpath = newpath.substring(0, pos);
					
					if (newpath.length() > 1)
						newpath += "/";
					
					newpath += name;
					
					S3File.this.setField("Path", newpath);
				}
				catch (Exception x) {
					// TODO log
				}
				finally {
					S3File.this.driver.releaseChannel(channel);
				}
				
				callback.completed();
			}
		});		
		* /
	}

	@Override
	public void remove(final OperationCallback callback) {
		if (this.removed) {
			// TODO log
			
			callback.completed();
			return;
		}

		/* TODO
		S3File.this.driver.getChannel(new FuncCallback<ChannelSftp>() {						
			@Override
			public void callback() {
				ChannelSftp channel = this.getResult();
				
				try {
					channel.rm(S3File.this.getFieldAsString("FullPath"));
					S3File.this.removed = true;
				}
				catch (Exception x) {
					// TODO log
				}
				finally {
					S3File.this.driver.releaseChannel(channel);
				}
				
				callback.completed();
			}
		});		
		* /
	}

	@Override
	public void setModificationTime(final DateTime time, final OperationCallback callback) {
		if (this.removed) {
			// TODO log
			
			callback.completed();
			return;
		}

		/* TODO
		S3File.this.driver.getChannel(new FuncCallback<ChannelSftp>() {						
			@Override
			public void callback() {
				ChannelSftp channel = this.getResult();
				
				try {
					int t = (int) (time.getMillis() / 1000);
					S3File.this.file.getAttrs().setACMODTIME(t, t);
					channel.setStat(S3File.this.getFieldAsString("FullPath"), S3File.this.file.getAttrs());
					S3File.this.setField("Modified", time);
				}
				catch (Exception x) {
					// TODO log
				}
				finally {
					S3File.this.driver.releaseChannel(channel);
				}
				
				callback.completed();
			}
		});		
		* /
	}

	@Override
	public void getAttribute(String name, FuncCallback<Struct> callback) {
		if ("Modified".equals(name)) {
			callback.setResult(new DateTimeStruct(this.getMofificationTime()));
			callback.completed();
			return;
		}
		
		/*
		if (this.removed) {
			// TODO log
		}
		else {
			try {
				callback.setResult(this.driver.channel.get(this.getFieldAsString("FullPath")));
			}
			catch (Exception x) {
				// TODO log
			}
		}
		* /
		
		callback.completed();
	}

	@Override
	public void setAttribute(String name, Struct value, OperationCallback callback) {
		if ("Modified".equals(name)) {
			this.setModificationTime(Struct.objectToDateTime(value), callback);
			callback.completed();
			return;
		}
		
		/*
		if (this.removed) {
			// TODO log
		}
		else {
			try {
				callback.setResult(this.driver.channel.get(this.getFieldAsString("FullPath")));
			}
			catch (Exception x) {
				// TODO log
			}
		}
		* /
		
		callback.completed();
	}	
	*/
}
