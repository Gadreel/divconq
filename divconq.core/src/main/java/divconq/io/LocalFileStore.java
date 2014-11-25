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

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import divconq.filestore.CommonPath;
import divconq.lang.op.FuncCallback;
import divconq.lang.op.OperationResult;
import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyListener;

public class LocalFileStore {
	protected Integer watchID = null;
	protected ConcurrentHashMap<String, CopyOnWriteArrayList<FuncCallback<FileStoreEvent>>> listeners = new ConcurrentHashMap<String, CopyOnWriteArrayList<FuncCallback<FileStoreEvent>>>();
	
	public void register(String pname, FuncCallback<FileStoreEvent> callback) {
		CopyOnWriteArrayList<FuncCallback<FileStoreEvent>> cblist = this.listeners.get(pname);
		
		if (cblist == null) {
			cblist = new CopyOnWriteArrayList<FuncCallback<FileStoreEvent>>();
			CopyOnWriteArrayList<FuncCallback<FileStoreEvent>> x = this.listeners.putIfAbsent(pname, cblist);
			
			if (x != null)
				cblist = x;
		}
		
		cblist.add(callback);
	}

	public void unregister(String pname, FuncCallback<FileStoreEvent> callback) {
		CopyOnWriteArrayList<FuncCallback<FileStoreEvent>> cblist = this.listeners.get(pname);
		
		if (cblist != null) 
			cblist.remove(callback);
	}
	
	public void fireEvent(String fname, boolean deleted) {
		fname = "/" + fname.replace('\\', '/');
		
		CommonPath p = new CommonPath(fname);
		
		if (p.getNameCount() < 2)
			return;
		
		FileStoreEvent evnt = new FileStoreEvent();
		
		evnt.packagename = p.getName(0);
		evnt.path = p.subpath(1);
		evnt.delete = deleted;		
		
		CopyOnWriteArrayList<FuncCallback<FileStoreEvent>> cblist = this.listeners.get(evnt.packagename);
		
		if (cblist != null) {
			for (FuncCallback<FileStoreEvent> cb : cblist) {
				cb.setResult(evnt);
				cb.complete();
			}
		}
	}

	public void start(OperationResult or) {
		new File("/Work/Temp/Watch").mkdirs(); 
		
		try {
			this.watchID = JNotify.addWatch(
					"/Work/Temp/Watch", 
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
