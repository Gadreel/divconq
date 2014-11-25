package divconq.filestore;

import divconq.ctp.CtpAdapter;
import divconq.filestore.select.FileSelection;

public interface IFileSelector extends IFileCollection {
	FileSelection selection();
	void read(CtpAdapter adapter);
}
