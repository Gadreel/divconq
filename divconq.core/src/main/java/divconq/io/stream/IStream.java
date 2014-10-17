package divconq.io.stream;

import divconq.work.TaskRun;

public interface IStream extends AutoCloseable {
	void setUpstream(IStream upstream);
	void setDownstream(IStream downstream);
	
	HandleReturn handle(TaskRun cb, StreamMessage msg);
	IStreamSource getOrigin();
	void cleanup(TaskRun cb);
	void request(TaskRun cb);
}
