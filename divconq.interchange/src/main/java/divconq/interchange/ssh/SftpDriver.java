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
package divconq.interchange.ssh;

public class SftpDriver { /*extends RecordStruct implements IFileStoreDriver {
	protected String password = null;
	protected String passphrase = null;
	protected Session session = null;
	protected CallbackQueue<ChannelSftp> channels = null;

	@Override
	public void setMimeProvider(IMimeProvider v) {
		// TODO Auto-generated method stub
		
	}
	
	public SftpDriver() {
		this.setField("Scanner", new SftpScanner(this));
		this.setField("RootFolder", ".");
		
		this.channels = new CallbackQueue<ChannelSftp>();
		
		this.channels.setWatcher(this.channels.new QueueWatcher() {			
			@Override
			public void disposed(ChannelSftp res) {
				res.exit();
			}
		});
	}
	
    @Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	//FileSystemDriver nn = (FileSystemDriver)n;
    	//nn.cwd = this.cwd;
    }
    
	@Override
	public Struct deepCopy() {
		SftpDriver cp = new SftpDriver();
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	public void dispose() {
		this.close(null);
		
		super.dispose();
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
	public void connect(RecordStruct params, OperationCallback callback) {
		// TODO clean and start new this.channels.dispose()
		
		if (this.session != null) {
			this.session.disconnect();
			this.session = null;
		}
		
		try {
			JSch jsch = new JSch();

			String host = this.getFieldAsString("Host");
			
			Long pt = this.getFieldAsInteger("Port");
			int port = (pt == null) ? 22 : pt.intValue();
			
			String user = this.getFieldAsString("User");
			this.password = this.getFieldAsString("Password");
			this.passphrase = this.getFieldAsString("Passphrase");
			
			// TODO 
			/*
			String keyfile = this.getFieldAsString("Key");
			
			if (StringUtil.isNotBlank(keyfile))
				jsch.addIdentity(keyfile, Sftp.passphrase);
			* /

			this.session = jsch.getSession(user, host, port);

			// username and password will be given via UserInfo interface.
			UserInfo ui = new MyUserInfo();
			
			this.session.setUserInfo(ui);
			this.session.connect();

			Channel channel = this.session.openChannel("sftp");
			channel.connect();
			
			if (this.hasField("RootFolder")) 
				((ChannelSftp) channel).cd(this.getFieldAsString("RootFolder"));;
			
			this.setField("RootFolder", ((ChannelSftp) channel).pwd());
			
			this.channels.add((ChannelSftp) channel);
		}
		catch (Exception x) {
			// TODO
		}
		
		if (callback == null)
			return;
		
		System.out.println("cwd: " + this.getFieldAsString("RootFolder"));
		
		callback.completed();
	}
	
	@Override
	public void close(OperationCallback callback) {
		this.channels.dispose();
		
		if (this.session != null) {
			this.session.disconnect();
			this.session = null;
		}
		
		if (callback == null)
			return;
		
		callback.completed();
	}
	
	public void getChannel(FuncCallback<ChannelSftp> callback) {
		this.channels.pop(callback);
	}
	
	public void releaseChannel(ChannelSftp channel) {
		this.channels.add(channel);
	}
	
	@Override
	public void operation(final StackEntry stack, final XElement codeEl) {
		if ("Connect".equals(codeEl.getName())) {
			this.connect(null, new OperationCallback() {				
				@Override
				public void callback() {
					stack.resume();
				}
			});

			return;
		}
		
		if ("Close".equals(codeEl.getName())) {
			this.close(new OperationCallback() {				
				@Override
				public void callback() {
					stack.resume();
				}
			});

			return;
		}
		
		if ("Put".equals(codeEl.getName())) {
			
			// TODO integrate with put method below
			
			Struct src = stack.refFromElement(codeEl, "Source");

			if (!(src instanceof IFileStoreFile) && ! (src instanceof RecordStruct)) {
				// TODO log wrong type
				stack.resume();
				return;
			}
			
			boolean relative = stack.boolFromElement(codeEl, "Relative", true);
			
			this.put((IFileStoreFile)src, relative, new FuncCallback<IFileStoreFile>() {				
				@Override
				public void callback() {
					// TODO check errors
					
			        String handle = stack.stringFromElement(codeEl, "Handle");

					if (handle != null) 
			            stack.addVariable(handle, (Struct) this.getResult());
					
					stack.resume();
				}
			});
			
			return;
		}
		
		
		if ("PutAll".equals(codeEl.getName())) {
			
			// TODO integrate with put method below
			
			Struct src = stack.refFromElement(codeEl, "Source");

			if (!(src instanceof IItemCollection)) {
				// TODO log wrong type
				stack.resume();
				return;
			}
			
			boolean relative = stack.boolFromElement(codeEl, "Relative", true);
			
			this.putAll((IItemCollection)src, relative, new OperationCallback() {				
				@Override
				public void callback() {
					// TODO check errors
					System.out.println("done");
					
					stack.resume();
				}
			});
			
			return;
		}
		
		/*
		if ("GetInfo".equals(codeEl.getName())) {
			String path = stack.stringFromElement(codeEl, "Path");

			if (StringUtil.isEmpty(path)) {
				// TODO log missing
				stack.resume();
				return;
			}
			
	        String handle = stack.stringFromElement(codeEl, "Handle");

			if (handle != null) 
	            stack.addVariable(handle, new SftpFile(SftpDriver.this, new RecordStruct(new FieldStruct("Path", path))));
			
			stack.resume();
			return;
		}
		
		if ("Put".equals(codeEl.getName())) {
			Struct src = stack.refFromElement(codeEl, "Source");

			if (src == null) {
				// TODO log missing
				stack.resume();
				return;
			}

			if (!(src instanceof IFileStoreFile) && ! (src instanceof RecordStruct)) {
				// TODO log wrong type
				stack.resume();
				return;
			}
			
			RecordStruct rsrc = (RecordStruct)src;
			final IFileStoreFile ssrc = (IFileStoreFile)src;
			boolean relative = stack.boolFromElement(codeEl, "Relative", true);
			
			String cwd = this.getFieldAsString("RootFolder");
			
			String dfilepath = cwd + "/" + (relative ? rsrc.getFieldAsString("Path") : rsrc.getFieldAsString("Name"));
			
			System.out.println("copied to: " + dfilepath);
			
			final File dest = new File(dfilepath);
			dest.getParentFile().mkdirs();
			
			try {
				final FileOutputStream out = new FileOutputStream(dest);
				
				ssrc.copyTo(out, new OperationCallback() {				
					@Override
					public void callback() {
						// TODO improve, check abort, etc
						
						try {
							out.close();
						} 
						catch (IOException x) {
						}
						
				        String handle = stack.stringFromElement(codeEl, "Handle");

						if (handle != null) 
				            stack.addVariable(handle, new SftpFile(SftpDriver.this, dest));
						
						stack.resume();
					}
				});
				
				return;
			}
			catch (Exception x) {
				// TODO
				//ssrc.abort();
			}			
			
			stack.resume();
			return;
		}
		* /
		
		/*
		if ("ChangeDirectory".equals(code.getName())) {
			String path = stack.stringFromElement(code, "Path");
			
			if (StringUtil.isEmpty(path)) {
				// TODO log
				stack.resume();
				return;
			}
			
			this.cwd = new File(path);
			
			stack.resume();
			return;
		}
		
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
		* /
		
		//System.out.println("fs operation: " + code);
		
		super.operation(stack, codeEl);
	}

	@Override
	public IFileStoreFile getFile(RecordStruct file) {
		return null;   // TODO new SftpFile(this, file);
	}


	public class MyUserInfo implements UserInfo {
		public String getPassword() {
			return SftpDriver.this.password;
		}

		public boolean promptYesNo(String str) {
			return true;
		}

		public String getPassphrase() {
			return SftpDriver.this.passphrase;
		}

		public boolean promptPassphrase(String message) {
			return true;
		}

		public boolean promptPassword(String message) {
			return true;
		}

		public void showMessage(String message) {
			System.out.println(message);		// TODO
		}
	}


	@Override
	public void getFileDetail(String path, FuncCallback<IFileStoreFile> callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getRootFolder(FuncCallback<String> callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setRootFolder(String path, OperationCallback callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addFolder(String path, OperationCallback callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeFolder(String path, OperationCallback callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void queryFeatures(FuncCallback<RecordStruct> callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void customCommand(RecordStruct params, FuncCallback<RecordStruct> callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IFileStoreScanner getScanner() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void getFolderListing(String path, FuncCallback<List<IFileStoreFile>> callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getFolderListing2(String path, FuncCallback<ListStruct> callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void put(IFileStoreFile source, boolean relative, final FuncCallback<IFileStoreFile> callback) {
		if (source == null) {
			// TODO log missing
			callback.completed();
			return;
		}

		if (! (source instanceof RecordStruct)) {
			// TODO log wrong type
			callback.completed();
			return;
		}
		
		RecordStruct rsrc = (RecordStruct)source;
		
		String cwd = this.getFieldAsString("RootFolder");
		
		final String dfilepath = cwd + "/" + (relative ? rsrc.getFieldAsString("Path") : rsrc.getFieldAsString("Name"));
		int pos = dfilepath.lastIndexOf('/');
		final String dfileparent = dfilepath.substring(0, pos);
		
		source.getInputStream(new FuncCallback<InputStream>() {			
			@Override
			public void callback() {
				final InputStream in = this.getResult();
				
				SftpDriver.this.getChannel(new FuncCallback<ChannelSftp>() {						
					@Override
					public void callback() {
						ChannelSftp channel = this.getResult();
						
						try {
							System.out.println("copied to sftp: " + dfilepath);
							
							// TODO mkdirs
							// dest.getParentFile().mkdirs();
							
							channel.mkdir(dfileparent);
							
							channel.put(in, dfilepath);
							
							@SuppressWarnings("rawtypes")
							Vector flist = channel.ls(dfilepath);
							
							if (flist.size() < 1) {
								// TODO log
							}
							else {	
								LsEntry entry = (LsEntry)flist.get(0);					
								callback.setResult(new SftpFile(SftpDriver.this, entry, dfilepath));
							}
							
							callback.completed();
						}
						catch (Exception x) {
							// TODO
							//ssrc.abort();
							
							callback.completed();
						}			
						finally {
							SftpDriver.this.releaseChannel(channel);
						}
					}
				});
			}
		});
	}

	@Override
	public void putAll(IItemCollection files, boolean relative, OperationCallback callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void put(InputStream in, long size, IFileStoreFile dest, boolean relative,
			OperationCallback callback) {
		// TODO Auto-generated method stub
		
	}
	*/
}
