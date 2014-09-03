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

public class S3TextReader { /* extends RecordStruct implements ITextReader {
	protected S3File file = null;
	
	public S3TextReader() {
		this.setType(Hub.instance.getSchema().getType("dciS3TextReader"));
	}

	public S3TextReader(S3File file) {
		this();
		
		this.file = file;
	}
	
	@Override
	public IAsyncIterable<Struct> getItemsAsync() {
		if (this.file == null)
			return null;
		
		return new TextReader();		
	}
	
	public class TextReader implements IAsyncIterable<Struct>, IAsyncIterator<Struct> {
		protected InputStream zin = null;		// TODO how/when does this close?
		protected LineIterator lineit = null;
		protected String next = null;

		@Override
		public IAsyncIterator<Struct> iterator() {
			return this;
		}

		public void init(final OperationCallback callback) {
			if (this.zin != null) {
				callback.completed();
				return;
			}
			
			S3TextReader.this.file.getInputStream(new FuncCallback<InputStream>() {				
				@Override
				public void callback() {
					TextReader.this.zin = this.getResult();		
					
					try {
						TextReader.this.lineit = IOUtils.lineIterator(TextReader.this.zin, "UTF-8");		// TODO support others...
					}
					catch (Exception x) {
						// TODO log
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
					callback.setResult(TextReader.this.lineit.hasNext());
					callback.completed();
				}
			});
		}

		@Override
		public void next(final FuncCallback<Struct> callback) {
			this.init(new OperationCallback() {
				@Override
				public void callback() {
					callback.setResult(new StringStruct(TextReader.this.lineit.next()));
					callback.completed();
				}
			});
		}
	}
	
    @Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	S3TextReader nn = (S3TextReader)n;
		nn.file = this.file;
    }
    
	@Override
	public Struct deepCopy() {
		S3TextReader cp = new S3TextReader();
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
