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

import org.joda.time.DateTime;

import divconq.io.stream.IStreamDest;
import divconq.io.stream.IStreamSource;
import divconq.lang.FuncCallback;
import divconq.lang.OperationCallback;
import divconq.script.StackEntry;
import divconq.session.DataStreamChannel;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.xml.XElement;

public interface IFileStoreFile {
	String getName();
	void setName(String v);
	String getPath();
	void setPath(String v);
	String getExtension();
	String getFullPath();
	DateTime getModificationTime();
	long getSize();
	boolean isFolder();
	void isFolder(boolean b);
	boolean exists();
	
	CommonPath path();
	CommonPath resolvePath(CommonPath path);
	
	IFileStoreDriver driver();
	IFileStoreScanner scanner();
	
	// TODO use DataStreamChannel instead
	//void copyTo(OutputStream out, OperationCallback callback);
	
	void hash(String method, FuncCallback<String> callback);
	
	// TODO use DataStreamChannel instead
	//void getInputStream(FuncCallback<InputStream> callback);
	
	void rename(String name, OperationCallback callback);
	
	void remove(OperationCallback callback);
	
	void setModificationTime(DateTime time, OperationCallback callback);
	
	void getAttribute(String name, FuncCallback<Struct> callback);
	void setAttribute(String name, Struct value, OperationCallback callback);
	
	// scripts
	public void operation(final StackEntry stack, XElement code);
	
	// remote
	
	void openRead(DataStreamChannel channel, FuncCallback<RecordStruct> callback);
	void openWrite(DataStreamChannel channel, FuncCallback<RecordStruct> callback);
	
	IStreamDest allocDest();
	IStreamSource allocSrc();
}
