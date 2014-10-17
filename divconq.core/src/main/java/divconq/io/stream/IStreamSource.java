package divconq.io.stream;

import divconq.script.StackEntry;
import divconq.xml.XElement;

public interface IStreamSource extends IStream {
	void init(StackEntry stack, XElement el);
}
