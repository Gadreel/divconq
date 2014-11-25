package divconq.filestore.select;

import java.util.concurrent.atomic.AtomicReference;

import divconq.filestore.IFileStoreFile;

public class FileMatcherSizeFilter extends FileMatcher {
	protected Long equal = null;
	protected Long lessThan = null;
	protected Long greaterThan = null;
	protected Long lessThanEqual = null;
	protected Long greaterThanEqual = null;

	public FileMatcherSizeFilter withEqual(long v) {
		this.equal = v;
		return this;
	}

	public FileMatcherSizeFilter withLessThan(long v) {
		this.lessThan = v;
		return this;
	}

	public FileMatcherSizeFilter withGreaterThan(long v) {
		this.greaterThan = v;
		return this;
	}

	public FileMatcherSizeFilter withLessThanEqual(long v) {
		this.lessThanEqual = v;
		return this;
	}

	public FileMatcherSizeFilter withGreaterThanEqual(long v) {
		this.greaterThanEqual = v;
		return this;
	}

	@Override
	public boolean approve(IFileStoreFile file, AtomicReference<String> value, FileSelection selection) {
		boolean pass = true;
		long size = file.getSize();
		
		// if equal is present only check that
		if (this.equal != null) {
			pass = (this.equal == size);
		}
		else {
			// otherwise check one greater than if available 
			if (this.greaterThanEqual != null)
				pass = (size >= this.greaterThanEqual);
			else if (this.greaterThan != null) 
				pass = (size > this.greaterThan);
			
			// and check one less than if available 
			if (pass) {
				if (this.lessThanEqual != null)
					pass = (size <= this.lessThanEqual);
				else if (this.lessThan != null) 
					pass = (size < this.lessThan);
			}
			
			// only if both pass is it still true
		}
		
		if (this.exclude)
			pass = !pass;
		
		return pass;
	}
}
