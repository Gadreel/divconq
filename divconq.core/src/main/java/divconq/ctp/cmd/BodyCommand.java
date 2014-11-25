package divconq.ctp.cmd;

import io.netty.buffer.ByteBuf;
import divconq.ctp.CtpCommand;
import divconq.hub.Hub;
import divconq.lang.chars.Special;
import divconq.lang.chars.Utf8Encoder;
import divconq.struct.RecordStruct;
import divconq.struct.builder.ObjectBuilder;
import divconq.struct.serial.BufferToCompositeParser;

abstract public class BodyCommand extends CtpCommand {
	protected RecordStruct body = null;
	
	public void setBody(RecordStruct v) {
		this.body = v;
	}
	
	public RecordStruct getBody() {
		return this.body;
	}
	
	public boolean isOp(String name) {
		if (this.body == null)
			return false;
		
		return name.equals(this.body.getFieldAsString("Op"));
	}
	
	@Override
	public ByteBuf encode() throws Exception {
		int size = 1 + 4096;  // code + JSON   --- TODO, current max is 4KB
		
		ByteBuf bb = Hub.instance.getBufferAllocator().buffer(size);
		
		bb.writeByte(this.cmdCode);
		
		if (this.body != null)
			this.body.toSerial(bb);
		else
			bb.writeBytes(Utf8Encoder.encode(Special.End.getCode()));
		
		return bb;
	}

	@Override
	public void release() {
		// na
	}
	
    protected BufferToCompositeParser headerparser = null;		
    protected ObjectBuilder builder = null;

	@Override
	public boolean decode(ByteBuf in) throws Exception {
		if (this.headerparser == null) {
			this.builder = new ObjectBuilder();
			this.headerparser = new BufferToCompositeParser(this.builder);
		}
		
		this.headerparser.parseStruct(in);
		
		// if not done wait for more bytes
		if (!this.headerparser.isDone())
			return false;
		
		this.body = (RecordStruct)this.builder.getRoot();
		this.builder = null;
		this.headerparser = null;

		return true;
	}
}
