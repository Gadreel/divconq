package divconq.interchange;

import divconq.lang.FuncCallback;
import divconq.script.StackEntry;
import divconq.xml.XElement;

public interface IFileCollection {
	void next(FuncCallback<IFileStoreFile> callback);
	CommonPath path();
	
	// scripts
	public void operation(final StackEntry stack, XElement code);
}
