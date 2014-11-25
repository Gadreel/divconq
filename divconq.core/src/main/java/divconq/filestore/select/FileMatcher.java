package divconq.filestore.select;

import java.util.concurrent.atomic.AtomicReference;

import divconq.filestore.IFileStoreFile;

abstract public class FileMatcher {
	protected boolean exclude = false;

	public FileMatcher withExclude() {
		this.exclude = true;
		return null;
	}

	public FileMatcher withInclude() {
		this.exclude = false;
		return null;
	}

	abstract public boolean approve(IFileStoreFile file, AtomicReference<String> value, FileSelection selection);
}
