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

import java.util.ArrayList;
import java.util.List;

import divconq.lang.op.FuncCallback;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;

/**
 * 
 * @author andy
 *
 */
public class FileCollection extends RecordStruct implements IFileCollection {
	protected List<IFileStoreFile> collection = null;
	protected int pos = 0;
	protected CommonPath basePath = CommonPath.ROOT;
	
	public FileCollection() {
		// TODO this.setType(Hub.instance.getSchema().getType("dciFileSystemScanner"));
	}
	
	public void add(IFileStoreFile... files) {
		if (this.collection == null)
			this.collection = new ArrayList<>();
		
		for (IFileStoreFile f : files)
			this.collection.add(f);
	}
	
	@Override
	public CommonPath path() {
		return this.basePath;
	}

	public void setPath(CommonPath v) {
		this.basePath = v;
	}
	
	@Override
	public void next(FuncCallback<IFileStoreFile> callback) {
		if ((this.collection != null) && (this.pos < this.collection.size())) {
			callback.setResult(collection.get(this.pos));
			this.pos++;
		}
		
		callback.complete();
	}
	
    @Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	FileCollection nn = (FileCollection)n;
		nn.collection = this.collection;
    }
    
	@Override
	public Struct deepCopy() {
		FileCollection cp = new FileCollection();
		this.doCopy(cp);
		return cp;
	}
}
