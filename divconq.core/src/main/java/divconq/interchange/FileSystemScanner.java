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

import java.io.File;
import java.util.Iterator;

import divconq.hub.Hub;
import divconq.lang.FuncCallback;
import divconq.script.StackEntry;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.xml.XElement;

public class FileSystemScanner extends RecordStruct implements IFileStoreScanner {
	protected FileSystemDriver driver = null;
	
	public FileSystemScanner() {
		this.setType(Hub.instance.getSchema().getType("dciFileSystemScanner"));
	}

	public FileSystemScanner(FileSystemDriver driver) {
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
	
	// TODO change to lazy stream
	@Override
	public Iterable<Struct> getItems() {
		if (this.driver == null)
			return null;
		
		/* TODO
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
				*/
		
		return null;
	}
	
	// TODO consider using Path instead of File 
	public class Matches implements Iterable<Struct>, Iterator<Struct> {
		protected Iterator<File> itr = null;
		
		/* TODO
		public Matches(File folder, IOFileFilter filefilter, IOFileFilter folderfilter) {
			this.itr = FileUtils.iterateFiles(folder, filefilter, folderfilter);
		}
		*/

		@Override
		public Iterator<Struct> iterator() {
			return this;
		}

		@Override
		public boolean hasNext() {
			return this.itr.hasNext();
		}

		@Override
		public Struct next() {
			return new FileSystemFile(FileSystemScanner.this.driver, this.itr.next().toPath());
		}

		@Override
		public void remove() {
			this.itr.remove();
		}
	}
	
    @Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	FileSystemScanner nn = (FileSystemScanner)n;
		nn.driver = this.driver;
    }
    
	@Override
	public Struct deepCopy() {
		FileSystemScanner cp = new FileSystemScanner();
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
