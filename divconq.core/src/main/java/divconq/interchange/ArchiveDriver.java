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

public class ArchiveDriver {  //extends RecordStruct implements IFileStoreDriver {
	/*
	protected IFileStoreFile archive = null;
	
	@Override
	public void setMimeProvider(IMimeProvider v) {
		// TODO Auto-generated method stub
		
	}
	
	public ArchiveDriver() {
		this.setField("Scanner", new ArchiveScanner(this));
		this.setField("RootFolder", ".");
	}
	
    @Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	//FileSystemDriver nn = (FileSystemDriver)n;
    	//nn.cwd = this.cwd;
    }
    
	@Override
	public Struct deepCopy() {
		ArchiveDriver cp = new ArchiveDriver();
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
	public void connect(RecordStruct params, OperationCallback callback) {
		this.archive = (IFileStoreFile) params;
		
		System.out.println("cwd: " + this.getFieldAsString("RootFolder"));
		
		if (callback == null)
			return;
		
		callback.completed();
	}
	
	@Override
	public void close(OperationCallback callback) {
		// TODO reset scanner
		
		if (callback == null)
			return;
		
		callback.completed();
	}
	
	@Override
	public void operation(final StackEntry stack, final XElement codeEl) {
		if ("Connect".equals(codeEl.getName())) {
			RecordStruct file = (RecordStruct) stack.refFromElement(codeEl, "File");
			
			this.connect(file, new OperationCallback() {				
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
		
		if ("GetInfo".equals(codeEl.getName())) {
			String path = stack.stringFromElement(codeEl, "Path");

			if (StringUtil.isEmpty(path)) {
				// TODO log missing
				stack.resume();
				return;
			}
			
	        String handle = stack.stringFromElement(codeEl, "Handle");

			if (handle != null) 
	            stack.addVariable(handle, new ArchiveFile(ArchiveDriver.this, new RecordStruct(new FieldStruct("Path", path))));
			
			stack.resume();
			return;
		}
		
		if ("Put".equals(codeEl.getName())) {
			/* TODO
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
				            stack.addVariable(handle, new ArchiveFile(ArchiveDriver.this, dest));
						
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
			* /
		}
		
		super.operation(stack, codeEl);
	}

	@Override
	public IFileStoreFile getFile(RecordStruct file) {
		return new ArchiveFile(this, file);
	}

	public void getEntry(final String path, final FuncCallback<ArchiveEntry> callback) {
		this.archive.getInputStream(new FuncCallback<InputStream>() {			
			@Override
			public void callback() {
				InputStream in = this.getResult();
				
				ZipArchiveInputStream zin = new ZipArchiveInputStream(in);
				
				try {
					ArchiveEntry ze = zin.getNextEntry();
					
					while (ze != null) {						
						String name = ze.getName();
						
						if (name.equals(path)) {
							callback.setResult(ze);
							break;
						}
						
						ze = zin.getNextEntry();
					}
					
				} 
				catch (IOException x) {
					// TODO log
				}
				finally {
					try {
						zin.close();
					} 
					catch (IOException x) {
					}
				}
				
				callback.completed();
			}
		});
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
