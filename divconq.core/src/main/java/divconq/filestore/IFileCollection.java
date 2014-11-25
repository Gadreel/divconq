package divconq.filestore;

import divconq.lang.op.FuncCallback;
import divconq.script.StackEntry;
import divconq.xml.XElement;

public interface IFileCollection {
	CommonPath path();		
	void next(FuncCallback<IFileStoreFile> callback);
	void forEach(FuncCallback<IFileStoreFile> callback);
	
	// scripts
	public void operation(final StackEntry stack, XElement code);
}
