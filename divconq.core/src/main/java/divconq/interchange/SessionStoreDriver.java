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

public class SessionStoreDriver { /*extends RecordStruct implements IFileStoreDriver {
	protected IApiSession session = null;
	
	@Override
	public void setMimeProvider(IMimeProvider v) {
		// TODO Auto-generated method stub
		
	}
	
	public SessionStoreDriver() {
		this.setField("Scanner", new SessionStoreScanner(this));
	}
	
    @Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	//FileSystemDriver nn = (FileSystemDriver)n;
    	//nn.cwd = this.cwd;
    }
    
	@Override
	public Struct deepCopy() {
		SessionStoreDriver cp = new SessionStoreDriver();
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	public void dispose() {
		// TODO support this!!!
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
	public void connect(RecordStruct params, final OperationCallback callback) {
		String sid = this.getFieldAsString("Session");
		
		// TODO if sid = remote session then use RemoteSession 
		
		Session sess = Hub.instance.getSessions().lookup(sid);
		
		this.session = new LocalSession();
		((LocalSession)this.session).init(sess, null);
		
		Message msg = new Message("Session", "FileStore", "Connect", this);
		msg.setField("Tag", this.getFieldAsString("Session"));

		this.session.sendMessage(msg, new ServiceResult() {				
			@Override
			public void callback() {
				if (callback == null)
					return;
				
				callback.copyMessages(this);
				callback.completed();
			}
		});
	}
	
	@Override
	public void close(final OperationCallback callback) {
		if (this.session == null) {
			if (callback != null)
				callback.completed();
			
			return;
		}
		
		Message msg = new Message("Session", "FileStore", "Close", this);
		msg.setField("Tag", this.getFieldAsString("Session"));
		
		this.session.sendMessage(msg, new ServiceResult() {				
			@Override
			public void callback() {
				if (callback == null)
					return;
				
				callback.copyMessages(this);
				callback.completed();
			}
		});
	}
	
	@Override
	public void operation(final StackEntry stack, final XElement codeEl) {
		if ("Connect".equals(codeEl.getName())) {
			this.connect(null, new OperationCallback() {				
				@Override
				public void callback() {
					System.out.println("connected: " + this);
					
					if (this.hasErrors()) {
						// TODO log
						System.out.println("could not connect to session");
					}
					
					stack.resume();
				}
			});
			
			return;
		}
		
		if ("Close".equals(codeEl.getName())) {
			this.close(new OperationCallback() {				
				@Override
				public void callback() {
					System.out.println("closed: " + this);
					
					if (this.hasErrors()) {
						// TODO log
						System.out.println("could not close to session");
					}
					
					stack.resume();
				}
			});
			
			return;
		}
		
		if ("Put".equals(codeEl.getName())) {
			Struct src = stack.refFromElement(codeEl, "Source");

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
			
			System.out.println("copying to session: " + dfilepath);
			
			final RecordStruct dest = new RecordStruct();
			dest.copyFields(rsrc);
			//dest.setField("Path", dfilepath);
			
			Message msg = new Message("Session", "FileStore", "OpenWrite", dest);
			msg.setField("Tag", this.getFieldAsString("Session"));
			
			this.session.sendMessage(msg, new ServiceResult() {			
				@Override
				public void callback() {
					if (this.hasErrors()) {
						// TODO remove
						System.out.println("OpenWrite Error: " + this.getMessages());
						
						stack.resume();
					}

					ssrc.getInputStream(new FuncCallback<InputStream>() {							
						@Override
						public void callback() {
							InputStream in = this.getResult();
							
							// TODO check if we can get inputstream
							
							OperationResult rmsg = SessionStoreDriver.this.session.sendStream(in, dest.getFieldAsInteger("Size"));		// TODO support async version of receiveStream
							
							// TODO improve, check abort, etc
							
							try {
								in.close();
							} 
							catch (IOException x) {
							}
							
							if (!rmsg.hasErrors()) {
						        String handle = stack.stringFromElement(codeEl, "Handle");
		
								if (handle != null) 
						            stack.addVariable(handle, new SessionStoreFile(SessionStoreDriver.this, dest));
							}
							
							stack.resume();
						}
					});
				}
			});
			
			
			//final File dest = new File(dfilepath);
			//dest.getParentFile().mkdirs();

			/*
			try {
				this.session.sendStream(in, size);
				
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
				            stack.addVariable(handle, new SessionStoreFile(SessionStoreDriver.this, dest));
						
						stack.resume();
					}
				});
				
				return;
			}
			catch (Exception x) {
				// TODO
				//ssrc.abort();
			}			
			* /
			
			//stack.resume();
			return;
		}
		
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
		return new SessionStoreFile(this, file);
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
	public void customCommand(RecordStruct params,
			FuncCallback<RecordStruct> callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IFileStoreScanner getScanner() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void getFolderListing(String path,
			FuncCallback<List<IFileStoreFile>> callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getFolderListing2(String path, FuncCallback<ListStruct> callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void put(IFileStoreFile source, boolean relative, FuncCallback<IFileStoreFile> callback) {
		// TODO Auto-generated method stub
		
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
