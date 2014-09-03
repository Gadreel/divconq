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

public class SessionStoreFile { /* extends RecordStruct implements IFileStoreFile {
	protected SessionStoreDriver driver = null;
	protected FileInputStream input = null;
	protected long offset = 0;
		
	public SessionStoreFile() {
		this.setType(Hub.instance.getSchema().getType("dciSessionStoreFile"));
	}

	public SessionStoreFile(SessionStoreDriver driver, RecordStruct file) {
		this();
		
		this.driver = driver;
		this.copyFields(file);
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
	* /
	
    @Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	SessionStoreFile nn = (SessionStoreFile)n;
		nn.driver = this.driver;
    }
    
	@Override
	public Struct deepCopy() {
		SessionStoreFile cp = new SessionStoreFile();
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
	public void operation(final StackEntry stack, XElement code) {
		if ("Hash".equals(code.getName())) {
			String meth = stack.stringFromElement(code, "Method");
			
			// TODO support other methods
			if (StringUtil.isEmpty(meth) || !"Sha2".equals(meth)) {
				// TODO log
				stack.resume();
				return;
			}
			
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
		Message msg = new Message("Session", "FileStore", "OpenRead", this);
		msg.setField("Tag", this.driver.getFieldAsString("Session"));
		
		this.driver.session.sendMessage(msg, new ServiceResult() {			
			@Override
			public void callback() {
				callback.copyMessages(this);
				
				if (this.hasErrors()) {
					// TODO remove
					System.out.println("Set Destination Error: " + this.getMessages());
				}
				else {
					OperationResult rmsg = SessionStoreFile.this.driver.session.receiveStream(out);		// TODO support async version of receiveStream
					callback.copyMessages(rmsg);
				}
				
				callback.completed();
			}
		});
	}
	
	@Override
	public void hash(String method, final FuncCallback<String> callback) {
		Message msg = new Message("Session", "FileStore", "FileHash", this);
		msg.setField("Tag", this.driver.getFieldAsString("Session"));
		
		this.driver.session.sendMessage(msg, new ServiceResult() {			
			@Override
			public void callback() {
				callback.copyMessages(this);
				
				if (this.hasErrors()) {
					// TODO remove
					System.out.println("Hash Error: " + this.getMessages());
				}
				else {
					callback.setResult(this.getResultAsString());
				}
				
				callback.completed();
			}
		});
	}

	@Override
	public void openRead(Session session, final FuncCallback<ISourceDriver> callback) {
		/* TODO
		Message msg = new Message("Session", "FileStore", "OpenRead", this);
		msg.setField("Tag", this.driver.getFieldAsString("Session"));
		
		this.driver.session.sendMessage(msg, new ServiceResult() {			
			@Override
			public void callback() {
				callback.copyMessages(this);
				
				if (this.hasErrors()) {
					// TODO remove
					System.out.println("Hash Error: " + this.getMessages());
				}
				else {
					callback.setResult(this.getResultAsString());
				}
				
				callback.completed();
			}
		});
		* /
	}

	@Override
	public void openWrite(Session session, final FuncCallback<IDestinationDriver> callback) {
		/* TODO
		Message msg = new Message("Session", "FileStore", "OpenWrite", this);
		msg.setField("Tag", this.driver.getFieldAsString("Session"));
		
		this.driver.session.sendMessage(msg, new ServiceResult() {			
			@Override
			public void callback() {
				callback.copyMessages(this);
				
				if (this.hasErrors()) {
					// TODO remove
					System.out.println("Hash Error: " + this.getMessages());
				}
				else {
					callback.setResult(this.getResultAsString());
				}
				
				callback.completed();
			}
		});
		* /
	}

	@Override
	public void getInputStream(FuncCallback<InputStream> callback) {
		// TODO download to temp and then return - OR - stream through...
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getExtension() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getFullPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DateTime getMofificationTime() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isFolder() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void rename(String name, OperationCallback callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void remove(OperationCallback callback) {
		// TODO Auto-generated method stub
		
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
	*/
}
