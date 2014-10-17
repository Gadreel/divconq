package divconq.io;

import divconq.hub.Hub;
import divconq.lang.chars.Utf8Encoder;
import io.netty.buffer.ByteBuf;

public class ByteBufWriter {
	static public ByteBufWriter createLargeHeap() {
		ByteBufWriter bw = new ByteBufWriter();
		bw.buffer = Hub.instance.getBufferAllocator().heapBuffer(32 * 1024, 4 * 1024 * 1024);
		return bw;
	}
	
	static public ByteBufWriter createSmallHeap() {
		ByteBufWriter bw = new ByteBufWriter();
		bw.buffer = Hub.instance.getBufferAllocator().heapBuffer(1024, 32 * 1024);
		return bw;
	}
	
	static public ByteBufWriter createLargeDirect() {
		ByteBufWriter bw = new ByteBufWriter();
		bw.buffer = Hub.instance.getBufferAllocator().directBuffer(32 * 1024, 4 * 1024 * 1024);
		return bw;
	}
	
	static public ByteBufWriter createSmallDirect() {
		ByteBufWriter bw = new ByteBufWriter();
		bw.buffer = Hub.instance.getBufferAllocator().directBuffer(1024, 32 * 1024);
		return bw;
	}
	
	protected ByteBuf buffer = null;
	
	protected ByteBufWriter() {		
	}
	
	public void write(String v) {
		Utf8Encoder.encode(v, this.buffer);
	}
	
	public void writeLine(String v) {
		Utf8Encoder.encode(v, this.buffer);
		this.buffer.writeBytes(Utf8Encoder.encode('\n')); 
	}
	
	public void writeLine() {
		this.buffer.writeBytes(Utf8Encoder.encode('\n')); 
	}
	
	public void writeChar(int v) {
		this.buffer.writeBytes(Utf8Encoder.encode(v)); 
	}

	public int readableBytes() {
		return this.buffer.readableBytes();
	}

	public ByteBuf getByteBuf() {
		return this.buffer;
	}
}
