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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import divconq.lang.FuncCallback;
import divconq.lang.OperationCallback;
import divconq.lang.OperationResult;
import divconq.script.StackEntry;
import divconq.struct.FieldStruct;
import divconq.struct.IItemCollection;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.FileUtil;
import divconq.util.IOUtil;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class FileSystemDriver extends RecordStruct implements IFileStoreDriver {

	//protected HashSet<CommonPath> locallocks = new HashSet<>();
	//protected ReentrantLock locallockslock = new ReentrantLock();
	
	@Override
	public void setMimeProvider(IMimeProvider v) {
		// TODO Auto-generated method stub
		
	}
	
	public FileSystemDriver() {
		this.setField("Scanner", new FileSystemScanner(this));
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
		FileSystemDriver cp = new FileSystemDriver();
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
	public void connect(RecordStruct params, OperationCallback callback) {
		// nothing to do
		
		if (callback == null)
			return;
		
		//System.out.println("cwd: " + this.getFieldAsString("RootFolder"));
		
		callback.completed();
	}
	
	public Path resolveToLocalPath(CommonPath path) {
		// client must interact with us using absolute paths
		if (!path.isAbsolute())
			return null;
		
		// TODO make sure the resolved path is lower than the root (fspath) path, no access to higher folders allowed via this api
		
		Path wd = Paths.get(this.getFieldAsString("RootFolder"));
		
		return wd.resolve(path.toString().substring(1));
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
			this.connect(null, new OperationCallback() {				
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
		
		if ("AllocateTempDir".equals(codeEl.getName())) {
			File tfolder = FileUtil.allocateTempFolder();
			
			try {
				this.setField("RootFolder", tfolder.getCanonicalPath());
			} 
			catch (IOException x) {
				// TODO Auto-generated catch block
			}
			
			stack.resume();
			return;
		}
		
		if ("Delete".equals(codeEl.getName())) {
			FileUtil.deleteDirectory(stack.log(), Paths.get(this.getFieldAsString("RootFolder")));
			
			stack.resume();
			return;
		}
		
		if ("MakeDir".equals(codeEl.getName())) {
			stack.log().copyMessages(FileUtil.confirmOrCreateDir(Paths.get(this.getFieldAsString("RootFolder"))));
			stack.resume();
			return;
		}
		
		if ("GetInfo".equals(codeEl.getName())) {
			String path = stack.stringFromElement(codeEl, "Path");

			if (StringUtil.isEmpty(path)) {
				// TODO log missing
				stack.resume();
				return;
			}
			
			boolean absolute = stack.boolFromElement(codeEl, "Absolute", false);
			
	        String handle = stack.stringFromElement(codeEl, "Handle");

			if (handle != null) 
				if (absolute)
					stack.addVariable(handle, new FileSystemFile(FileSystemDriver.this, Paths.get(path)));
				else
					stack.addVariable(handle, new FileSystemFile(FileSystemDriver.this, new RecordStruct(new FieldStruct("Path", path))));
			
			stack.resume();
			return;
		}
		
		if ("Put".equals(codeEl.getName())) {
			
			// TODO integrate with put method below
			
			Struct src = stack.refFromElement(codeEl, "Source");

			if (!(src instanceof IFileStoreFile) && ! (src instanceof RecordStruct)) {
				// TODO log wrong type
				stack.resume();
				return;
			}
			
			boolean relative = stack.boolFromElement(codeEl, "Relative", true);
			
			this.put((IFileStoreFile)src, relative, new FuncCallback<IFileStoreFile>() {				
				@Override
				public void callback() {
					// TODO check errors
					
			        String handle = stack.stringFromElement(codeEl, "Handle");

					if (handle != null) 
			            stack.addVariable(handle, (Struct) this.getResult());
					
					stack.resume();
				}
			});
			
			return;
		}
		
		
		if ("PutAll".equals(codeEl.getName())) {
			
			// TODO integrate with put method below
			
			Struct src = stack.refFromElement(codeEl, "Source");

			if (src == null) {
				// TODO log missing
				stack.resume();
				return;
			}

			if (!(src instanceof IItemCollection)) {
				// TODO log wrong type
				stack.resume();
				return;
			}
			
			boolean relative = stack.boolFromElement(codeEl, "Relative", true);
			
			this.putAll((IItemCollection)src, relative, new OperationCallback() {				
				@Override
				public void callback() {
					// TODO check errors
					System.out.println("done");
					
					stack.resume();
				}
			});
			
			return;
		}
		
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
		
		//System.out.println("fs operation: " + code);
		
		super.operation(stack, codeEl);
	}

	@Override
	public IFileStoreFile wrapFileRecord(RecordStruct file) {
		return new FileSystemFile(this, file);
	}

	@Override
	public void getFileDetail(CommonPath path, FuncCallback<IFileStoreFile> callback) {
		FileSystemFile f = new FileSystemFile(this, path);
		
		callback.setResult(f);
		callback.completed();
	}

	public String getRootFolder() {
		return this.getFieldAsString("RootFolder");
	}

	public void setRootFolder(String path) {
		this.setField("RootFolder", path);
	}

	@Override
	public void addFolder(CommonPath path, OperationCallback callback) {
		Path localpath = this.resolveToLocalPath(path);
		
		if (Files.exists(localpath)) {
			if (!Files.isDirectory(localpath)) {
				callback.error("Path is not a folder: " + localpath);
			}
		}
		else {
			callback.copyMessages(FileUtil.confirmOrCreateDir(localpath));
		}
		
		callback.completed();
	}

	@Override
	public void removeFolder(CommonPath path, OperationCallback callback) {
		Path localpath = this.resolveToLocalPath(path);
		
		FileUtil.deleteDirectory(callback, localpath);
		
		callback.completed();
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
		return (IFileStoreScanner) this.getField("Scanner");
	}

	@Override
	public void getFolderListing(CommonPath path, FuncCallback<List<IFileStoreFile>> callback) {
		Path folder = this.resolveToLocalPath(path);
		
		if (folder == null) {
			callback.error("Requested path is invalid");
			callback.completed();
			return;
		}
		
		if (!Files.exists(folder)) {
			callback.error("Requested path does not exist");
			callback.completed();
			return;
		}
		
		if (!Files.isDirectory(folder)) {
			callback.error("Requested path is not a folder");
			callback.completed();
			return;
		}
		
		List<IFileStoreFile> files = new ArrayList<>();
		
		try {
			Files.list(folder).forEach(entry -> {
				FileSystemFile f = new FileSystemFile(this, entry);
				files.add(f);
			});
			
			callback.setResult(files);
		}
		catch (IOException x) {
			callback.error("Problem listing files: " + x);
		}
		
		callback.completed();
		
	}

	@Override
	public void put(IFileStoreFile source, boolean relative, final FuncCallback<IFileStoreFile> callback) {
		if (source == null) {
			// TODO log missing
			callback.completed();
			return;
		}

		if (! (source instanceof RecordStruct)) {
			// TODO log wrong type
			callback.completed();
			return;
		}
		
		RecordStruct rsrc = (RecordStruct)source;
		
		String cwd = this.getFieldAsString("RootFolder");
		
		String dfilepath = cwd + "/" + (relative ? rsrc.getFieldAsString("Path") : rsrc.getFieldAsString("Name"));
		
		System.out.println("copied to: " + dfilepath);
		
		final Path dest = Paths.get(dfilepath);
		
		OperationResult mdres = FileUtil.confirmOrCreateDir(dest.getParent());
		
		callback.copyMessages(mdres);
			
		if (mdres.hasErrors()) {
			callback.completed();
			return;
		}
		
		try {
			final OutputStream out = Files.newOutputStream(dest);
			
			source.copyTo(out, new OperationCallback() {				
				@Override
				public void callback() {
					// TODO improve, check abort, etc
					
					try {
						out.close();
					} 
					catch (IOException x) {
					}
					
					callback.setResult(new FileSystemFile(FileSystemDriver.this, dest));					
					callback.completed();
				}
			});
		}
		catch (Exception x) {
			// TODO
			//ssrc.abort();
			
			callback.completed();
		}			
	}
	

	@Override
	public void put(final InputStream in, long size, final IFileStoreFile dest, boolean relative, final OperationCallback callback) {
		if (in == null) {
			// TODO log missing
			callback.completed();
			return;
		}

		if (dest == null) {
			// TODO log missing
			callback.completed();
			return;
		}

		if (! (dest instanceof RecordStruct)) {
			// TODO log wrong type
			callback.completed();
			return;
		}
		
		final RecordStruct rsrc = (RecordStruct)dest;
		
		String cwd = this.getFieldAsString("RootFolder");
		
		String dfilepath = cwd + "/" + (relative ? rsrc.getFieldAsString("Path") : rsrc.getFieldAsString("Name"));
		
		System.out.println("copied to: " + dfilepath);
		
		try {
			Path destp = Paths.get(dfilepath);
			
			Files.createDirectories(destp);
			
			try {
				Files.copy(in, destp);
			} 
			catch (IOException x) {
				callback.error(1, "Unable to write file");		// TODO codes
			}
			finally {
	            IOUtil.closeQuietly(in);
			}
			
			callback.completed();
		}
		catch (Exception x) {
			// TODO
			//ssrc.abort();
			
			callback.completed();
		}			
	}	

	@Override
	public void putAll(IItemCollection files, final boolean relative, final OperationCallback callback) {
		/* TODO restore someday
		// launch three threads
		int threadcount = 3;
		
		final IAsyncIterator<Struct> it = files.getItemsAsync().iterator();
		final ReentrantLock lock = new ReentrantLock();
		final CountDownCallback cdcallback = new CountDownCallback(threadcount, callback);
		
		final AtomicReference<IWork> putcmd = new AtomicReference<IWork>();
		
		IWork copy = new IWork() {			
			@Override
			public void run(final Task task) {
				lock.lock();	
				
				it.hasNext(new FuncCallback<Boolean>() {							
					@Override
					public void callback() {
						// TODO logging

						// if not next then complete
						if (!this.getResult()) {
							
							System.out.println(Thread.currentThread().getId() + " done");
							
							lock.unlock();
							cdcallback.countDown();
							
							task.complete();
							return;
						}
						
						it.next(new FuncCallback<Struct>() {								
							@Override
							public void callback() {
								// TODO logging
								
								lock.unlock();

								final IFileStoreFile source = (IFileStoreFile) this.getResult();
								
								System.out.println(Thread.currentThread().getId() + " start copying " + source.getPath());
								
								FileSystemDriver.this.put(source, relative, new FuncCallback<IFileStoreFile>() {									
									@Override
									public void callback() {
										// TODO logging
										
										System.out.println(Thread.currentThread().getId() + " done copying " + source.getPath());
										
										// regardless of success, set thread to copy next file in list 
										Hub.instance.getScheduler().runNow(putcmd.get());										
									}
								});
							}
						});							
					}
				});
			}
		};
		
		putcmd.set(copy);
		
		for (int i = 0; i < threadcount; i++) 
			Hub.instance.getScheduler().runNow(copy);
			*/
	}
	
	/*
	// return true if got lock
	@Override
	public boolean tryLocalLock(CommonPath path) {
		this.locallockslock.lock();

		try {
			if (!this.locallocks.contains(path)) {
				this.locallocks.add(path);
				System.out.println("File locked: " + path);
				return true;
			}
		}
		finally {
			this.locallockslock.unlock();
		}
		
		System.out.println("Failed file locked: " + path);
		
		return false;
	}

	@Override
	public void releaseLocalLock(CommonPath path) {
		this.locallockslock.lock();

		try {
			if (this.locallocks.remove(path))
				System.out.println("File unlocked: " + path);
			else
				System.out.println("Bad file unlock: " + path);
		}
		finally {
			this.locallockslock.unlock();
		}
	}
	*/
}