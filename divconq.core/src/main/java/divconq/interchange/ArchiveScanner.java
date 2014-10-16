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

public class ArchiveScanner { /* extends RecordStruct implements IFileStoreScanner {
	protected ArchiveDriver driver = null;
	protected volatile int totalFolders = 0;
	protected volatile int totalFiles = 0;
	protected volatile int matchedFiles = 0;
	
	public ArchiveScanner() {
		this.setType(Hub.instance.getSchema().getType("dciArchiveScanner"));
	}

	public ArchiveScanner(ArchiveDriver driver) {
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
		protected ArchiveInputStream zin = null;		// TODO how/when does this close?
		protected ArchiveEntry next = null;
		protected String cwd = null;
		protected boolean recursive = false;
		protected List<String> wildcards = null;
		
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
			if (this.zin != null) {
				callback.completed();
				return;
			}
			
			ArchiveScanner.this.driver.archive.getInputStream(new FuncCallback<InputStream>() {				
				@Override
				public void callback() {
					Matches.this.zin = new ZipArchiveInputStream(this.getResult());		// TODO not just ZIP
					callback.completed();
				}
			});
		}
		
		@Override
		public void hasNext(final FuncCallback<Boolean> callback) {
			this.init(new OperationCallback() {
				@Override
				public void callback() {
					callback.setResult(false);
					
					try {
						Matches.this.next = Matches.this.zin.getNextEntry();
						String wd = Matches.this.cwd + "/";
						boolean fnd = false;
						
						while (Matches.this.next != null) {
							if (Matches.this.next.isDirectory()) 
								ArchiveScanner.this.totalFolders++;
							else {
								ArchiveScanner.this.totalFiles++;
								
								String ename = Matches.this.next.getName();
								
								// TODO respect recursive flag
								if (ename.startsWith(wd)) {
									int pos = ename.lastIndexOf('/');
									
									if (pos != -1)
										ename = ename.substring(pos + 1);
									
									for (String wc : Matches.this.wildcards)
										if (FilenameUtils.wildcardMatch(ename, wc)) {
											fnd = true;
											ArchiveScanner.this.matchedFiles++;
											break;
										}
									
									if (fnd)
										break;
								}
							}
							
							Matches.this.next = Matches.this.zin.getNextEntry();
						}
						
						if (Matches.this.next == null) 
							Matches.this.zin.close();
						else
							callback.setResult(true);
					} 
					catch (IOException x) {
						// TODO log
					}
					
					callback.completed();
				}
			});
		}

		@Override
		public void next(final FuncCallback<Struct> callback) {
			this.init(new OperationCallback() {
				@Override
				public void callback() {
					callback.setResult(new ArchiveFile(ArchiveScanner.this.driver, Matches.this.next));
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
    	
    	ArchiveScanner nn = (ArchiveScanner)n;
		nn.driver = this.driver;
    }
    
	@Override
	public Struct deepCopy() {
		ArchiveScanner cp = new ArchiveScanner();
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	public void dispose() {
		// TODO support this!!!
		super.dispose();
	}
	
	@Override
	public void operation(StackEntry stack, XElement code) {
		
		super.operation(stack, code);
	}
	*/
}
