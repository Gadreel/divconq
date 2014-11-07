package divconq.interchange;

import divconq.lang.op.FuncCallback;
import divconq.script.StackEntry;
import divconq.xml.XElement;

public interface IFileCollection {
	void next(FuncCallback<IFileStoreFile> callback);
	CommonPath path();		// TODO file collection should not be bound to a common path, get path elsewhere
	
	// scripts
	public void operation(final StackEntry stack, XElement code);
}
