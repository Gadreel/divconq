package divconq.io.stream;

import java.nio.file.Path;

import divconq.interchange.CommonPath;
import divconq.interchange.FileSystemDriver;
import divconq.interchange.FileSystemFile;
import divconq.util.FileUtil;
import divconq.util.StringUtil;
import divconq.work.IWork;
import divconq.work.Task;
import divconq.work.TaskRun;

public class StreamUtil {
	static public TaskRun composeStream(Task task, IStream... steps) {
		if (steps.length < 2)
			throw new IllegalArgumentException("Stream steps must contain a source and destination step");
		
		if (!(steps[0] instanceof IStreamSource))
			throw new IllegalArgumentException("Stream steps must contain a source as the first step");
		
		if (!(steps[steps.length - 1] instanceof IStreamDest))
			throw new IllegalArgumentException("Stream steps must contain a destination as the last step");
		
		for (int i = 1; i < steps.length; i++)
			steps[i].setUpstream(steps[i - 1]);
		
		IWork sw = new StreamWork((IStreamDest) steps[steps.length - 1]);
		
		task.withWork(sw);
		
		return new TaskRun(task);
	}
	
	static public FileSystemFile localFile(Path lpath) {
	    FileSystemDriver drv = new FileSystemDriver(lpath.getParent());
	    return new FileSystemFile(drv, new CommonPath("/" + lpath.getFileName().toString()), false);
	}
	
	static public FileSystemDriver localDriver(Path lpath) {
	    return new FileSystemDriver(lpath.getParent());
	}
	
	static public FileSystemFile tempFile(String ext) {
        CommonPath path = new CommonPath("/" + (StringUtil.isNotEmpty(ext) ? FileUtil.randomFilename(ext) : FileUtil.randomFilename()));
        
        Path tfpath = FileUtil.allocateTempFolder2();

        FileSystemDriver drv = new FileSystemDriver(tfpath);
        drv.isTemp(true);
        
        return new FileSystemFile(drv, path, false);
	}
	
	static public FileSystemFile tempFolder() {
        Path path = FileUtil.allocateTempFolder2();

        FileSystemDriver drv = new FileSystemDriver(path);
        drv.isTemp(true);
        
        return new FileSystemFile(drv, CommonPath.ROOT, true);
	}

}
