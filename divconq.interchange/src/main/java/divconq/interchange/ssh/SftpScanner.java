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

public class SftpScanner { /* extends RecordStruct implements IFileStoreScanner {
	protected SftpDriver driver = null;
	protected volatile int totalFolders = 0;
	protected volatile int totalFiles = 0;
	protected volatile int matchedFiles = 0;
	
	public SftpScanner() {
		this.setType(Hub.instance.getSchema().getType("dciSftpScanner"));
	}

	public SftpScanner(SftpDriver driver) {
		this();
		
		this.driver = driver;
	}
	
	@Override
	public void scan(FuncCallback<RecordStruct> callback) {
		// TODO support offset/max and paging
		ListStruct res = new ListStruct();
		
		Iterable<Struct> files = this.getItems();
		
		// collect all for now...
		
		if (files != null) 
			for (Struct file : files) 
				res.addItem(file);
		
		callback.setResult(
			new RecordStruct(
				new FieldStruct("Total", res.getSize()),
				new FieldStruct("Offset", 0),
				new FieldStruct("Matches", res)
			)
		);
		
		callback.completed();
	}
	
	@Override
	public IAsyncIterable<Struct> getItemsAsync() {
		this.totalFolders = 0;
		this.totalFiles = 0;
		this.matchedFiles = 0;
		
		if (this.driver == null)
			return null;
		
		String cwd = this.driver.getFieldAsString("RootFolder");
		Boolean recursive = this.getFieldAsBoolean("Recursive");
		ListStruct match = this.getFieldAsList("MatchFiles");

		List<String> wildcards = new ArrayList<String>();
		
		if (match != null) 
			for (Struct s : match.getItems()) 
				wildcards.add(((StringStruct)s).getValue());
		
		return new Matches(cwd, recursive, wildcards);		
	}
	
	public class Matches implements IAsyncIterable<Struct>, IAsyncIterator<Struct> {
		protected boolean scanned = false;
		protected Queue<SftpFile> files = new LinkedList<SftpFile>();
		
		protected String cwd = null;
		protected boolean recursive = false;
		protected List<String> wildcards = null;
		
		// TODO improve performance - memory - only scan one folder at a time
		
		public Matches(String cwd, boolean recursive, List<String> wildcards) {
			this.cwd = cwd;
			this.recursive = recursive;
			this.wildcards = wildcards;
		}

		@Override
		public IAsyncIterator<Struct> iterator() {
			return this;
		}

		public void init(final OperationCallback callback) {
			if (this.scanned) {
				callback.completed();
				return;
			}
			
			SftpScanner.this.driver.getChannel(new FuncCallback<ChannelSftp>() {						
				@Override
				public void callback() {
					ChannelSftp channel = this.getResult();
					
					Matches.this.loadFolder(channel, Matches.this.cwd);
					Matches.this.scanned = true;
					callback.completed();

					SftpScanner.this.driver.releaseChannel(channel);
				}
			});
		}
		
		public void loadFolder(ChannelSftp channel, String path) {
			try {
				@SuppressWarnings("rawtypes")
				Vector matches = channel.ls(path);
				
				for (Object o : matches) {
					LsEntry entry = (LsEntry) o;

					SftpATTRS eattrs = entry.getAttrs();					
					String ename = entry.getFilename();
					
					if ("..".equals(ename) || ".".equals(ename))
						continue;
					
					if (eattrs.isDir()) {
						SftpScanner.this.totalFolders++;
						
						if (this.recursive)
							this.loadFolder(channel, path + "/" + ename);
						
						continue;
					}
					
					SftpScanner.this.totalFiles++;					
					
					for (String wc : Matches.this.wildcards)
						if (FilenameUtils.wildcardMatch(ename, wc)) {
							this.files.add(new SftpFile(SftpScanner.this.driver, entry, path + "/" + ename));
							SftpScanner.this.matchedFiles++;					
							break;
						}
				}
			} 
			catch (SftpException x) {
				// TODO
			}
		}
		
		@Override
		public void hasNext(final FuncCallback<Boolean> callback) {
			this.init(new OperationCallback() {
				@Override
				public void callback() {
					callback.setResult(Matches.this.files.peek() != null);
					callback.completed();
				}
			});
		}

		@Override
		public void next(final FuncCallback<Struct> callback) {
			this.init(new OperationCallback() {
				@Override
				public void callback() {
					callback.setResult(Matches.this.files.poll());
					callback.completed();
				}
			});
		}
	}

	@Override
	public FuncResult<Struct> getOrAllocateField(String name) {
		if ("TotalFolders".equals(name)) {
			FuncResult<Struct> res = new FuncResult<Struct>();
			res.setResult(new IntegerStruct(this.totalFolders));
			return res;
		}
		
		if ("TotalFiles".equals(name)) {
			FuncResult<Struct> res = new FuncResult<Struct>();
			res.setResult(new IntegerStruct(this.totalFiles));
			return res;
		}
		
		if ("MatchFiles".equals(name)) {
			FuncResult<Struct> res = new FuncResult<Struct>();
			res.setResult(new IntegerStruct(this.matchedFiles));
			return res;
		}
		
		return super.getOrAllocateField(name);
	}
	
    @Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	SftpScanner nn = (SftpScanner)n;
		nn.driver = this.driver;
    }
    
	@Override
	public Struct deepCopy() {
		SftpScanner cp = new SftpScanner();
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
