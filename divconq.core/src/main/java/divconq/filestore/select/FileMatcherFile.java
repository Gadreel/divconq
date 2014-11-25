package divconq.filestore.select;

import java.util.concurrent.atomic.AtomicReference;

import divconq.filestore.CommonPath;
import divconq.filestore.IFileStoreFile;

public class FileMatcherFile extends FileMatcher {
	protected CommonPath path = null;
	protected Integer recursion = null;		// default to 1 in read, 999 in other operations
	protected String newname = null;
	protected long offset = 0;
	
	// set by Selector after init
	protected CommonPath expandedPath = null;
	
	public FileMatcherFile withPath(CommonPath v) {
		this.path = v;
		return this;
	}

	public FileMatcherFile withRecursion(int v) {
		this.recursion = v;
		return this;
	}

	public FileMatcherFile withRename(String v) {
		this.newname = v;
		return this;
	}

	public FileMatcherFile withOffset(long v) {
		this.offset = v;
		return this;
	}

	public CommonPath expandedPath() {
		return this.expandedPath;
	}

	public int recursion(FileSelectionMode mode) {
		if (this.recursion == null) {
			if (mode == FileSelectionMode.Detail)
				return 0;
			
			if (mode == FileSelectionMode.Listing)
				return 1;
			
			return 999;
		}
		
		return this.recursion;
	}
	
	@Override
	public boolean approve(IFileStoreFile file, AtomicReference<String> value, FileSelection selection) {
		boolean pass = false;
		int recur = this.recursion(selection.getMode());
		
		if (this.expandedPath.equals(file.path()) || this.expandedPath.isParent(file.path())) {
			int fparts = file.path().getNameCount();
			int pparts = this.expandedPath.getNameCount();
			
			if (fparts - pparts <= recur)
				pass = true;
		}
		
		if (this.exclude)
			pass = !pass;
		
		return pass;
	}
}
