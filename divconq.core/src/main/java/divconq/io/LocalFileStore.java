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
import java.util.concurrent.CopyOnWriteArrayList;

import divconq.filestore.CommonPath;
import divconq.lang.op.FuncCallback;
import divconq.lang.op.OperationResult;
import divconq.xml.XElement;
import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyListener;

public class LocalFileStore {
	protected Integer watchID = null;
	protected Path path = null;
	protected String spath = null;
	
	protected CopyOnWriteArrayList<FuncCallback<FileStoreEvent>> listeners = new CopyOnWriteArrayList<>();

	public String getPath() {
		return this.spath;
	}

	public Path getFilePath() {
		return this.path;
	}

	public Path resolvePath(String path) {
		return this.path.resolve(path);
	}

	public Path resolvePath(Path path) {
		return this.path.resolve(path);
	}

	public Path resolvePath(CommonPath path) {
		return this.path.resolve(path.toString().substring(1));
	}
	
	public void register(FuncCallback<FileStoreEvent> callback) {
		this.listeners.add(callback);
	}

	public void unregister(FuncCallback<FileStoreEvent> callback) {
		this.listeners.remove(callback);
	}
	
	public void fireEvent(String fname, boolean deleted) {
		fname = "/" + fname.replace('\\', '/');
		
		CommonPath p = new CommonPath(fname);
		
		if (p.getNameCount() < 2)
			return;
		
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
		
		this.path = Paths.get(fpath);
			
		if (Files.exists(this.path) && !Files.isDirectory(this.path)) {
			or.error("File Store cannot be mounted: " + fpath);
			return;
		}
		
		try {
			Files.createDirectories(this.path);
			
			this.spath = this.path.toString();
			
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
