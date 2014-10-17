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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import divconq.hub.Hub;
import divconq.lang.FuncCallback;
import divconq.lang.OperationResult;
import divconq.struct.Struct;

/**
 * Eventually we may shift to a "yield" approach, for now collects all files
 * 
 * @author andy
 *
 */
public class FileSystemScanner extends FileCollection implements IFileStoreScanner {
	protected FileSystemDriver driver = null;
	protected FileSystemFile folder = null;
	
	public FileSystemScanner() {
		if (Hub.instance.getSchema() != null)
			this.setType(Hub.instance.getSchema().getType("dciFileSystemScanner"));
	}

	public FileSystemScanner(FileSystemDriver driver) {
		this();
		
		this.driver = driver;
		this.basePath = driver.resolvePath(CommonPath.ROOT);
	}

	public FileSystemScanner(FileSystemFile folder) {
		this();
		
		this.driver = folder.driver();
		this.folder = folder;
		this.basePath = folder.path();
	}
	
	public void collectAll(OperationResult or) {
		// don't collect more than once
		if (this.collection != null)
			return;
		
		this.collection = new ArrayList<>();
		
		// TODO support filters/sorting/etc
		
		Path folder = (this.folder == null) ? this.driver.localPath() : this.folder.localPath();
		
		// collect all for now...may be more efficient later

		try {
			Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path sfolder, BasicFileAttributes attrs) throws IOException {
					if (!sfolder.equals(folder))
						FileSystemScanner.this.collection.add(new FileSystemFile(FileSystemScanner.this.driver, sfolder));
					
					return FileVisitResult.CONTINUE;
				}
				
				@Override
				public FileVisitResult visitFile(Path sfile, BasicFileAttributes attrs) throws IOException {
					FileSystemScanner.this.collection.add(new FileSystemFile(FileSystemScanner.this.driver, sfile));
					
					return FileVisitResult.CONTINUE;
				}
			});
		}
		catch (IOException x) {
			or.error("Unable to delete directory: " + folder + ", error: " + x);
		}
	}
	
	@Override
	public void next(FuncCallback<IFileStoreFile> callback) {
		this.collectAll(callback);
		
		super.next(callback);		
	}
	
	// TODO change to lazy stream
	/* TODO
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
				* /
		
		return null;
	}
	
	// TODO consider using Path instead of File 
	public class Matches implements Iterable<Struct>, Iterator<Struct> {
		protected Iterator<File> itr = null;
		
		/* TODO
		public Matches(File folder, IOFileFilter filefilter, IOFileFilter folderfilter) {
			this.itr = FileUtils.iterateFiles(folder, filefilter, folderfilter);
		}
		* /

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
	*/
	
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
}
