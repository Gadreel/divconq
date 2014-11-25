package divconq.filestore.select;

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import divconq.filestore.IFileStoreFile;

public class FileMatcherPathFilter extends FileMatcher {
	protected Pattern pattern = null;
	protected String startsWith = null;
	protected String endsWith = null;
	
	public FileMatcherPathFilter withPattern(Pattern v) {
		this.pattern = v;
		return this;
	}
	
	public FileMatcherPathFilter withStartsWith(String v) {
		this.startsWith = v;
		return this;
	}
	
	public FileMatcherPathFilter withEndsWith(String v) {
		this.endsWith = v;
		return this;
	}

	@Override
	public boolean approve(IFileStoreFile file, AtomicReference<String> value, FileSelection selection) {
		boolean pass = true;
		String name = file.getPath();
		
		if (this.pattern != null) {
			Matcher m = this.pattern.matcher(name);
			
			pass = m.matches();
			
			if (pass && (m.groupCount() > 0) && (value.get() == null)) 
				value.set(m.group(1));
		}
		
		if (pass && (this.startsWith != null)) {
			pass = name.startsWith(this.startsWith);
		}
		
		if (pass && (this.endsWith != null)) {
			pass = name.endsWith(this.endsWith);
		}
		
		if (this.exclude)
			pass = !pass;
		
		return pass;
	}
}
