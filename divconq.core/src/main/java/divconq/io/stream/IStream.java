package divconq.io.stream;

public interface IStream extends AutoCloseable {
	void setUpstream(IStream upstream);
	void setDownstream(IStream downstream);
	
	HandleReturn handle(StreamMessage msg);
	IStreamSource getOrigin();
	void cleanup();
	void request();
}
