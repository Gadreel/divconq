package divconq.filestore;

import divconq.lang.op.FuncResult;

public interface ISyncFileCollection extends IFileCollection {
	FuncResult<IFileStoreFile> next();
}
