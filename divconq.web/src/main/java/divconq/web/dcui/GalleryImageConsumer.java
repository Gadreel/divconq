package divconq.web.dcui;

import divconq.struct.RecordStruct;
import divconq.struct.Struct;

public interface GalleryImageConsumer {
	void accept(RecordStruct meta, RecordStruct show, Struct img);
}
