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
package divconq.filestore;

import java.util.List;

import divconq.filestore.select.FileSelection;
import divconq.lang.op.FuncCallback;
import divconq.lang.op.OperationCallback;
import divconq.script.StackEntry;
import divconq.struct.RecordStruct;
import divconq.xml.XElement;

public interface IFileStoreDriver {
	IFileSelector select(FileSelection selection);
	
	
	// TODO review and probably remove many of these
	void setMimeProvider(IMimeProvider v);
	
	// connect with given settings
	void connect(RecordStruct params, OperationCallback callback);
	
	// close (callback can be null to ignore/skip results of close)
	void close(OperationCallback callback);
	
	// works with files or folders - get file detail on one file
	void getFileDetail(CommonPath path, FuncCallback<IFileStoreFile> callback);
	
	// the driver has a concept of "current working folder" against which 
	// other operations are relative to
	//String getRootFolder();
	//void setRootFolder(String path);
	
	// add a folder - if path starts with / then relative to root, else relative to working folder
	void addFolder(CommonPath path, FuncCallback<IFileStoreFile> callback);
	
	// remove a folder - if path starts with / then relative to root, else relative to working folder
	void removeFolder(CommonPath path, OperationCallback callback);

	void queryFeatures(FuncCallback<RecordStruct> callback);
	
	void customCommand(RecordStruct params, FuncCallback<RecordStruct> callback);

	IFileStoreScanner scanner();
	
	void getFolderListing(CommonPath path, FuncCallback<List<IFileStoreFile>> callback);
	//void getFolderListing2(String path, FuncCallback<ListStruct> callback);
	
	//void put(InputStream in, long size, IFileStoreFile dest, boolean relative, OperationCallback callback);
	
	//void put(IFileStoreFile source, boolean relative, FuncCallback<IFileStoreFile> callback);
	
	//void putAll(IItemCollection files, boolean relative, OperationCallback callback);
	
	// scripts
	void operation(final StackEntry stack, XElement code);
	
	IFileStoreFile wrapFileRecord(RecordStruct file);

	// the only thing we can mount are folders, not files - so all root folders are safe 
	// for use without needing to use async   
	IFileStoreFile rootFolder();
	
	CommonPath resolvePath(CommonPath path);
	
	//String resolveToWorkingPath(CommonPath path);
	
	//boolean tryLocalLock(CommonPath path);
	//void releaseLocalLock(CommonPath path);
}
