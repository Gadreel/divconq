package divconq.io.stream;

import divconq.script.StackEntry;
import divconq.work.TaskRun;
import divconq.xml.XElement;

public interface IStreamDest extends IStream {
	void init(StackEntry stack, XElement el, boolean autorelative);
	void execute(TaskRun cb);
	void cleanup(TaskRun run);
}
