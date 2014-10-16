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

import java.io.InputStream;
import java.io.OutputStream;

import org.joda.time.DateTime;

import divconq.lang.FuncCallback;
import divconq.lang.OperationCallback;
import divconq.script.StackEntry;
import divconq.session.DataStreamChannel;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.xml.XElement;

public interface IFileStoreFile {
	String getName();
	String getPath();
	String getExtension();
	String getFullPath();
	DateTime getMofificationTime();
	long getSize();
	boolean isFolder();
	boolean exists();
	
	// TODO use DataStreamChannel instead
	void copyTo(OutputStream out, OperationCallback callback);
	
	void hash(String method, FuncCallback<String> callback);
	
	// TODO use DataStreamChannel instead
	void getInputStream(FuncCallback<InputStream> callback);
	
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
}
