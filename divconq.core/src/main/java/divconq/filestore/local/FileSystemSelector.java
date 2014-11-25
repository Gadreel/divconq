package divconq.filestore.local;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import divconq.ctp.CtpAdapter;
import divconq.ctp.WriteMoreFuture;
import divconq.ctp.net.CtpReadWork;
import divconq.filestore.FileCollection;
import divconq.filestore.IFileSelector;
import divconq.filestore.IFileStoreFile;
import divconq.filestore.ISyncFileCollection;
import divconq.filestore.select.FileMatcherFile;
import divconq.filestore.select.FileSelection;
import divconq.filestore.select.FileSelectionMode;
import divconq.lang.op.FuncCallback;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.struct.Struct;
import divconq.work.TaskRun;

public class FileSystemSelector extends FileCollection implements IFileSelector, ISyncFileCollection {
	protected FileSystemDriver driver = null;
	protected FileSelection selection = null;
	protected AtomicReference<String> lastValue = new AtomicReference<>();

	@Override
	public FileSelection selection() {
		return this.selection;
	}
	
	public FileSystemSelector() {
	}

	/* generic approach to read, may need this for gneric collection...
		AtomicReference<ChannelFutureListener> contRef = new AtomicReference<>();
			
		Task t = new Task()
			.withWork(new IWork() {												
			@Override
			public void run(TaskRun trun) {
					FuncCallback<IFileStoreFile> selectNext = new FuncCallback<IFileStoreFile>() {
						@Override
						public void callback() {
							try {
								//System.out.println("next file");
								
								if (this.isEmptyResult()) {
									//System.out.println("empty file");
								tunnel.sendCommand(new FinalCommand());
								trun.complete();
								return;
								}
								
								//System.out.println("got file: " + this.getResult().getName());
								
							BlockCommand cmd = new BlockCommand();
      						
							//cmd.setPath(this.getResult().getPath());
							cmd.setPath(this.getResult().path().subpath(lastSelector.selection().relativeTo()).toString());
							cmd.setEof(true);
								
								//System.out.println("Path: " + this.getResult().getPath());
								//System.out.println("RelativePath: " + this.getResult().path().subpath(selector.selection().relativeTo()));
								
      						tunnel.sendCommand(cmd, contRef.get());
							}
							catch (Exception x) {
								System.out.println("Ctp-F Server error: " + x);
							}
						}
					};
					
					lastSelector.next(selectNext);
			}
		});
		
		TaskRun tr = new TaskRun(t);
		
		contRef.set(new ChannelFutureListener() {										
		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			Hub.instance.getWorkPool().execute(new Runnable() {
				@Override
				public void run() {
					// TODO more efficient approach?
						//lastSelector.next(selectNext);
					tr.resume();
				}
			});
		}
	}); 
		
		tr.resume();
*/
	
	@Override
	public void read(CtpAdapter adapter) {
		// TODO make sure we are in the session context here
		
		CtpReadWork rw = new CtpReadWork();
		rw.setAdapter(adapter);
		rw.setSelector(this);
		
		TaskRun tr = new TaskRun();
		
		rw.setFuture(new WriteMoreFuture(tr)); 
		
		tr.getTask().withWork(rw);
		
		tr.resume();
	}
	
	public FileSystemSelector(FileSystemDriver driver, FileSelection selection) {
		this.driver = driver;
		this.selection = selection;
		this.basePath = selection.relativeTo();
	}

	// TODO some day find a way to improve this to be more efficient under large loads
	public void collectAll() {
		// don't collect more than once
		if (this.collection != null)
			return;
		
		this.collection = new ArrayList<>();
		
		List<FileMatcherFile> slist = this.selection.searchList();
		
		for (FileMatcherFile file : slist) {
			Path pfile = this.driver.resolveToLocalPath(file.expandedPath());
	
			try {
				if (Files.isDirectory(pfile) && (FileSystemSelector.this.selection.getMode() != FileSelectionMode.Detail)) {
					Files.walkFileTree(pfile, new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult preVisitDirectory(Path sfolder, BasicFileAttributes attrs) throws IOException {
							// TODO optimize so that we don't go down too many folder levels, look at recursion value
							
							if (!sfolder.equals(pfile))
								FileSystemSelector.this.tryAdd(sfolder);
								
							return FileVisitResult.CONTINUE;
						}
						
						@Override
						public FileVisitResult visitFile(Path sfile, BasicFileAttributes attrs) throws IOException {
							FileSystemSelector.this.tryAdd(sfile);
							
							return FileVisitResult.CONTINUE;
						}
					});
				}
				else {
					this.tryAdd(pfile);
				}
			}
			catch (IOException x) {
				OperationContext.get().error("Unable to collect file: " + pfile + ", error: " + x);
			}
		}
		
		// TODO support sorting/etc
	}
	
	public void tryAdd(Path lfile) {
		IFileStoreFile file = new FileSystemFile(this.driver, lfile);
		
		// be sure we don't confuse next value with previous
		this.lastValue.set(null);
		
		if (this.selection.approve(file, this.lastValue))
			this.collection.add(file);		// TODO track lastValue too
	}

	@Override
	public FuncResult<IFileStoreFile> next() {
		if (this.collection == null)
			this.collectAll();
		
		FuncResult<IFileStoreFile> res = new FuncResult<>();
		
		if ((this.collection != null) && (this.pos < this.collection.size())) {
			res.setResult(collection.get(this.pos));
			this.pos++;
		}
		
		return res;
	}
	
	@Override
	public void next(FuncCallback<IFileStoreFile> callback) {
		if (this.collection == null)
			this.collectAll();
		
		super.next(callback);		
	}
	
	@Override
	public void forEach(FuncCallback<IFileStoreFile> callback) {
		if (this.collection == null)
			this.collectAll();
		
		super.forEach(callback);
	}
	
    @Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	FileSystemSelector nn = (FileSystemSelector)n;
		nn.driver = this.driver;
		nn.selection = this.selection;
    }
    
	@Override
	public Struct deepCopy() {
		FileSystemSelector cp = new FileSystemSelector();
		this.doCopy(cp);
		return cp;
	}
}
