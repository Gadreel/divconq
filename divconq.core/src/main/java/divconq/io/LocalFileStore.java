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
package divconq.io;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import divconq.filestore.CommonPath;
import divconq.lang.op.FuncCallback;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.log.Logger;
import divconq.xml.XElement;
import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyListener;

public class LocalFileStore {
	protected Integer watchID = null;
	
	// absolute, normalized paths in Path and String forms
	protected Path path = null;
	protected String spath = null;
	
	protected Map<String, CacheFile> cache = new HashMap<>();
	
	protected CopyOnWriteArrayList<FuncCallback<FileStoreEvent>> listeners = new CopyOnWriteArrayList<>();

	public String getPath() {
		return this.spath;
	}

	public Path getFilePath() {
		return this.path;
	}

	public Path resolvePath(String path) {
		return this.path.resolve(path.startsWith("/") ? path.substring(1) : path).normalize().toAbsolutePath();
	}

	public Path resolvePath(Path path) {
		if (path.isAbsolute()) {
			if (path.startsWith(this.path))
				return path;
			
			return null;
		}
		
		return this.path.resolve(path).normalize().toAbsolutePath();
	}

	public Path resolvePath(CommonPath path) {
		return this.path.resolve(path.toString().substring(1)).normalize().toAbsolutePath();
	}
	
	public String relativize(Path path) {
		path = path.normalize().toAbsolutePath();
		
		if (path == null)
			return null;
		
		String rpath = path.toString().replace('\\', '/');
		
		if (!rpath.startsWith(this.spath))
			return null;
		
		return rpath.substring(this.spath.length());
	}
	
	// use CacheFile once and then let go, call this every time you need it or you may be holding on to stale content
	public CacheFile cacheResolvePath(String file) {
		Path lf = this.resolvePath(file);
		
		if (lf != null) {
			String ln = this.relativize(lf);
			
			CacheFile ra = this.cache.get(ln);
			
			if (ra != null) 
				return ra;
			
			ra = CacheFile.fromFile(ln, lf);
			
			if (ra != null) {
				this.cache.put(ln, ra);
				return ra;
			}
		}
		
		return null;
	}
	
	// use CacheFile once and then let go, call this every time you need it or you may be holding on to stale content
	public CacheFile cacheResolvePath(Path file) {
		if (Logger.isDebug())
			Logger.debug("cache resolve path: " + file);
		
		Path lf = this.resolvePath(file);
		
		if (Logger.isDebug())
			Logger.debug("cache resolve path, resolve: " + lf);
		
		if (lf != null) {
			String ln = this.relativize(lf);
			
			if (Logger.isDebug())
				Logger.debug("cache resolve path, relativize: " + ln);
			
			CacheFile ra = this.cache.get(ln);
			
			if (Logger.isDebug())
				Logger.debug("cache resolve path, cache: " + ra);
			
			if (ra != null) 
				return ra;
			
			ra = CacheFile.fromFile(ln, lf);
			
			if (Logger.isDebug())
				Logger.debug("cache resolve path, file: " + ra);
			
			//System.out.println("rcache " + this.cache);
			
			if (ra != null) {
				this.cache.put(ln, ra);
				return ra;
			}
		}
		
		return null;
	}
	
	// use CacheFile once and then let go, call this every time you need it or you may be holding on to stale content
	public CacheFile cacheResolvePath(CommonPath file) {
		Path lf = this.resolvePath(file);
		
		if (lf != null) {
			String ln = this.relativize(lf);
			
			CacheFile ra = this.cache.get(ln);
			
			if (ra != null) 
				return ra;
			
			ra = CacheFile.fromFile(ln, lf);
			
			if (ra != null) {
				this.cache.put(ln, ra);
				return ra;
			}
		}
		
		return null;
	}
	
	public boolean cacheHas(String file) {
		Path lf = this.resolvePath(file);
		
		if (lf != null) {
			String ln = this.relativize(lf);
			
			CacheFile ra = this.cache.get(ln);
			
			if (ra != null) 
				return true;
			
			ra = CacheFile.fromFile(ln, lf);
			
			if (ra != null) {
				this.cache.put(ln, ra);
				return true;
			}
		}
		
		return false;
	}	
	
	public void register(FuncCallback<FileStoreEvent> callback) {
		this.listeners.add(callback);
	}

	public void unregister(FuncCallback<FileStoreEvent> callback) {
		this.listeners.remove(callback);
	}
	
	public void fireEvent(String fname, boolean deleted) {
		OperationContext.useHubContext();
		
		fname = "/" + fname.replace('\\', '/');
		
		//System.out.println("lfs event: " + fname);
		
		this.cache.remove(fname);
		//CacheFile h = this.cache.remove(fname);
		
		//if (h != null)
			//System.out.println("removed from cache: " + fname);
		
		CommonPath p = new CommonPath(fname);
		
		if (p.getNameCount() == 0)
			return;
		
		// TODO create task and put it on the internal (service) work bucket
		
		//System.out.println("lfs fire event on: " + fname);
		
		FileStoreEvent evnt = new FileStoreEvent();
		
		evnt.path = p;  
		evnt.delete = deleted;		
		
		for (FuncCallback<FileStoreEvent> cb : this.listeners) {
			cb.setResult(evnt);
			cb.complete();
		}
	}

	public void start(OperationResult or, XElement fstore) {
		String fpath = fstore.hasAttribute("FolderPath") 
				? fstore.getAttribute("FolderPath")
				: fstore.getName().equals("PrivateFileStore") 
					? "./private" 
					: fstore.getName().equals("PackageFileStore")
						? "./packages"
						: "./public";
		
		this.path = Paths.get(fpath).normalize().toAbsolutePath();
			
		if (Files.exists(this.path) && !Files.isDirectory(this.path)) {
			or.error("File Store cannot be mounted: " + fpath);
			return;
		}
		
		try {
			Files.createDirectories(this.path);
			
			this.spath = this.path.toString().replace('\\', '/');
			
			this.watchID = JNotify.addWatch(
					this.spath, 
					JNotify.FILE_CREATED | JNotify.FILE_DELETED | JNotify.FILE_MODIFIED | JNotify.FILE_RENAMED, 
					true, 
					new JNotifyListener() {
						public void fileRenamed(int wd, String rootPath, String oldName, String newName) {
							LocalFileStore.this.fireEvent(oldName, true);
							LocalFileStore.this.fireEvent(newName, false);
						}
		
						public void fileModified(int wd, String rootPath, String name) {
							LocalFileStore.this.fireEvent(name, false);
						}
		
						public void fileDeleted(int wd, String rootPath, String name) {
							LocalFileStore.this.fireEvent(name, true);
						}
		
						public void fileCreated(int wd, String rootPath, String name) {
							LocalFileStore.this.fireEvent(name, false);
						}
					}
			);
		} 
		catch (Exception x) {
			or.errorTr(132, x);
		}
	}
	
	public void stop(OperationResult or) {
		try {
			if (this.watchID == null)
				return;
			
			JNotify.removeWatch(this.watchID);
		} 
		catch (Exception x) {
			// unimportant
		}
	}
}
