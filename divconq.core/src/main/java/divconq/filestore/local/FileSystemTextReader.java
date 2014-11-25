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
package divconq.filestore.local;

import java.io.InputStream;

import divconq.filestore.ITextReader;
import divconq.hub.Hub;
import divconq.io.LineIterator;
import divconq.lang.op.FuncCallback;
import divconq.lang.op.OperationCallback;
import divconq.script.StackEntry;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.struct.scalar.StringStruct;
import divconq.util.IAsyncIterable;
import divconq.util.IAsyncIterator;
import divconq.xml.XElement;

public class FileSystemTextReader extends RecordStruct implements ITextReader {
	protected FileSystemFile file = null;
	
	public FileSystemTextReader() {
		this.setType(Hub.instance.getSchema().getType("dciFileSystemTextReader"));
	}

	public FileSystemTextReader(FileSystemFile file) {
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
				callback.complete();
				return;
			}
			
			/* TODO
			FileSystemTextReader.this.file.getInputStream(new FuncCallback<InputStream>() {				
				@Override
				public void callback() {
					TextReader.this.zin = this.getResult();		
					
					try {
						TextReader.this.lineit = IOUtil.lineIterator(TextReader.this.zin, Charset.forName("UTF-8"));		
					}
					catch (Exception x) {
						// TODO log
					}
					
					callback.completed();
				}
			});
			*/
		}
		
		@Override
		public void hasNext(final FuncCallback<Boolean> callback) {
			this.init(new OperationCallback() {
				@Override
				public void callback() {
					callback.setResult(TextReader.this.lineit.hasNext());
					callback.complete();
				}
			});
		}

		@Override
		public void next(final FuncCallback<Struct> callback) {
			this.init(new OperationCallback() {
				@Override
				public void callback() {
					callback.setResult(new StringStruct(TextReader.this.lineit.next()));
					callback.complete();
				}
			});
		}
	}
	
    @Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	FileSystemTextReader nn = (FileSystemTextReader)n;
		nn.file = this.file;
    }
    
	@Override
	public Struct deepCopy() {
		FileSystemTextReader cp = new FileSystemTextReader();
		this.doCopy(cp);
		return cp;
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
	*/
	
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
		*/
		
		super.operation(stack, code);
	}
}
