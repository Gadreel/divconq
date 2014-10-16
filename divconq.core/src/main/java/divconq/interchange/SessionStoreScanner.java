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

public class SessionStoreScanner { /* extends RecordStruct implements IFileStoreScanner {
	protected SessionStoreDriver driver = null;
	
	public SessionStoreScanner() {
		this.setType(Hub.instance.getSchema().getType("dciSessionStoreScanner"));
	}

	public SessionStoreScanner(SessionStoreDriver driver) {
		this();
		
		this.driver = driver;
	}
	
	@Override
	public IAsyncIterable<Struct> getItemsAsync() {
		return new Matches();		
	}
	
	public class Matches implements IAsyncIterable<Struct>, IAsyncIterator<Struct> {
		protected Iterator<SessionStoreFile> itr = null;
		
		public Matches() {
		}

		@Override
		public IAsyncIterator<Struct> iterator() {
			return this;
		}

		public void init(final OperationCallback callback) {
			if (this.itr != null) {
				callback.completed();
				return;
			}
			
			SessionStoreScanner.this.scan(new FuncCallback<RecordStruct>() {				
				@Override
				public void callback() {
					ListStruct matches = this.getResult().getFieldAsList("Matches");
					
					if (matches != null) {
						List<SessionStoreFile> list = new ArrayList<SessionStoreFile>();
						
						for (Struct m : matches.getItems()) {
							list.add(new SessionStoreFile(SessionStoreScanner.this.driver, (RecordStruct) m)); 
						}
						
						Matches.this.itr = list.iterator();
					}
					
					callback.completed();
				}
			});
		}
		
		@Override
		public void hasNext(final FuncCallback<Boolean> callback) {
			this.init(new OperationCallback() {
				@Override
				public void callback() {
					callback.setResult(Matches.this.itr.hasNext());
					callback.completed();
				}
			});
		}

		@Override
		public void next(final FuncCallback<Struct> callback) {
			this.init(new OperationCallback() {
				@Override
				public void callback() {
					callback.setResult(Matches.this.itr.next());
					callback.completed();
				}
			});
		}
	}
	
    @Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	SessionStoreScanner nn = (SessionStoreScanner)n;
		nn.driver = this.driver;
    }
    
	@Override
	public Struct deepCopy() {
		SessionStoreScanner cp = new SessionStoreScanner();
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
	public void scan(final FuncCallback<RecordStruct> callback) {
		Message msg = new Message("Session", "FileStore", "Scan", this);
		msg.setField("Tag", this.driver.getFieldAsString("Session"));
		
		this.driver.session.sendMessage(msg, new ServiceResult() {				
			@Override
			public void callback() {
				if (callback == null)
					return;
				
				callback.copyMessages(this);
				callback.setResult(this.getResultAsRec());
				callback.completed();
			}
		});
	}
	
	@Override
	public void operation(StackEntry stack, XElement code) {
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
		
		super.operation(stack, code);
	}
	*/
}
