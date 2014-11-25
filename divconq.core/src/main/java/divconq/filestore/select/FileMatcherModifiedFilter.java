package divconq.filestore.select;

import java.util.concurrent.atomic.AtomicReference;

import divconq.filestore.IFileStoreFile;

public class FileMatcherModifiedFilter extends FileMatcher {
	protected String equal = null;
	protected String lessThan = null;
	protected String greaterThan = null;
	protected String lessThanEqual = null;
	protected String greaterThanEqual = null;

	public FileMatcherModifiedFilter withEqual(String v) {
		this.equal = v;
		return this;
	}

	public FileMatcherModifiedFilter withLessThan(String v) {
		this.lessThan = v;
		return this;
	}

	public FileMatcherModifiedFilter withGreaterThan(String v) {
		this.greaterThan = v;
		return this;
	}

	public FileMatcherModifiedFilter withLessThanEqual(String v) {
		this.lessThanEqual = v;
		return this;
	}

	public FileMatcherModifiedFilter withGreaterThanEqual(String v) {
		this.greaterThanEqual = v;
		return this;
	}

	@Override
	public boolean approve(IFileStoreFile file, AtomicReference<String> value, FileSelection selection) {
		boolean pass = true;
		String datetime = file.getModification();
		
		if (this.equal != null) {
			pass = this.equal.startsWith(datetime);
		}
		else {
			if (this.greaterThanEqual != null) 
				pass = (datetime.compareTo(this.greaterThanEqual) >= 0);
			else if (this.greaterThan != null) 
				pass = (datetime.compareTo(this.greaterThan) > 0);

			// only check if the greater than passed
			if (pass) {
				if (this.lessThanEqual != null) 
					pass = (datetime.compareTo(this.lessThanEqual) <= 0);
				else if (this.lessThan != null) 
					pass = (datetime.compareTo(this.lessThan) < 0);
			}
		}
		
		if (this.exclude)
			pass = !pass;
		
		return pass;
	}
}
