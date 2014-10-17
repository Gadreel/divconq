package divconq.io.stream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import divconq.interchange.CommonPath;
import divconq.interchange.IFileStoreFile;
import divconq.io.ctp.CtpMessage;
import divconq.lang.chars.Utf8Decoder;
import divconq.lang.chars.Utf8Encoder;

public class StreamMessage extends CtpMessage {
	static public StreamMessage FINAL = new StreamMessage();
	
	static public StreamMessage fromFileStore(IFileStoreFile file) {
		StreamMessage ref = new StreamMessage();
		ref.path = file.path();
		ref.isfolder = file.isFolder();
		ref.filesize = file.getSize();
		ref.modified = file.getModificationTime().getMillis();
		
		// TODO permission
		
		return ref;
	}
	
	static public StreamMessage fromMessage(StreamMessage msg) {
		StreamMessage ref = new StreamMessage();
		ref.path = msg.getPath();
		ref.isfolder = msg.isFolder();
		ref.filesize = msg.getFileSize();
		ref.modified = msg.getModified();
		ref.permission = msg.getPermission();
		
		return ref;
	}
	
	protected CommonPath path = null;
	protected boolean eof = false;
	protected boolean isfolder = false;
	protected long filesize = 0;
	protected long modified = 0;
	protected byte permission = 0x40;  
	protected long payloadoffset = 0;
	protected ByteBuf payload = null;
	
	public StreamMessage() {
		this.msgType = 4;
	}
	
	public boolean isFolder() {
		return this.isfolder;
	}
	
	public void setIsFolder(boolean v) {
		this.isfolder = v;
	}
	
	public boolean isEof() {
		return this.eof;
	}
	
	public void setEof(boolean v) {
		this.eof = v;
	}
	
	public CommonPath getPath() {
		return this.path;
	}
	
	public void setPath(CommonPath v) {
		this.path = v;
	}
	
	public void setPath(String v) {
		this.path = new CommonPath(v);
	}
	
	public ByteBuf getPayload() {
		return this.payload;
	}
	
	public void setPayload(ByteBuf v) {
		this.payload = v;
	}
	
	public long getFileSize() {
		return this.filesize;
	}
	
	public void setFileSize(long v) {
		this.filesize = v;
	}
	
	public long getModified() {
		return this.modified;
	}
	
	public void setModified(long v) {
		this.modified = v;
	}
	
	public long getPayloadoffset() {
		return this.payloadoffset;
	}
	
	public void setPayloadoffset(long v) {
		this.payloadoffset = v;
	}
	
	public byte getPermission() {
		return this.permission;
	}
	
	public void setPermission(byte v) {
		this.permission = v;
	}

	@Override
	public ByteBuf encode(ByteBufAllocator ba) {
		int size = 1  // type
				+ 2  // name size
				+ 1   // flag
				+ 4   // size
				+ 8   // modified
				+ 3   // permissions
				+ 8   // offset
				+ 3;   // payload size
				
		int nsize = 0;
		int psize = 0;
		
		if (this.path != null)
			nsize = Utf8Encoder.size(this.path.getFull());
		
		if (this.payload != null)
			psize += this.payload.readableBytes();
		
		ByteBuf bb = ba.buffer(size + nsize + psize);
		
		bb.writeByte(this.msgType);
		bb.writeShort(nsize);
		
		Utf8Encoder.encode(this.path.getFull(), bb);
		
		byte flg = 0;
		
		flg += (this.isfolder ? 1 : 0);
		flg += (this.eof ? 1 : 0) << 1;
		
		bb.writeByte(flg);
		
		bb.writeLong(this.filesize);
		bb.writeLong(this.modified);
		
		bb.writeByte(this.permission);
		bb.writeByte(0);		// unused byte
		bb.writeByte(0);		// unused byte
		
		bb.writeLong(this.payloadoffset);
		
		if (this.payload != null) {
			bb.writeMedium(this.payload.readableBytes());
			bb.writeBytes(this.payload);
		}
		else
			bb.writeMedium(0);
		
		return bb;
	}

	////////////////////////////////////////////////////////////////////
	// decode support
	////////////////////////////////////////////////////////////////////
	
    enum State {
        PATH_SIZE,
        PATH,
        FLAGS,
        SIZE,
        MODIFIED,
        PERMISSION,
        PAYLOAD_OFFSET,
        PAYLOAD_SIZE,
        PAYLOAD
    }

    protected State state = State.PATH_SIZE;
	protected int pathsize = 0;
	protected int paysize = 0;
	
	@Override
	public boolean decode(ChannelHandlerContext ctx, ByteBuf in) {
		switch (this.state) {
		case PATH_SIZE: {
            if (in.readableBytes() < 2) 
                return false;
			
            this.pathsize = in.readShort();
			this.state = State.PATH;
			
			// deliberate fall through 
		}
		case PATH: {
			// return here, without any state reset, means we need more before we can decide what to do
            if (in.readableBytes() < this.pathsize) 
                return false;
            
            if (this.pathsize > 0)
            	this.setPath(Utf8Decoder.decode(in, this.pathsize).toString());
            
			this.state = State.FLAGS;
			
			// deliberate fall through 
		}
		case FLAGS: {
            if (in.readableBytes() < 1) 
                return false;
			
            int flags = in.readByte();
            
            // TODO decode flags properly
            
            if (flags == 1)
            	this.isfolder = true;
            else if (flags == 2)
            	this.eof = true;
            
			this.state = State.SIZE;
			
			// deliberate fall through 
		}
		case SIZE: {
            if (in.readableBytes() < 8) 
                return false;
			
            this.filesize = in.readLong();
            
            // decode flags
            
			this.state = State.MODIFIED;
			
			// deliberate fall through 
		}
		case MODIFIED: {
            if (in.readableBytes() < 8) 
                return false;
			
            this.modified = in.readLong();
            
			this.state = State.PERMISSION;
			
			// deliberate fall through 
		}
		case PERMISSION: {
            if (in.readableBytes() < 3) 
                return false;
			
            this.permission = in.readByte();
            
            in.skipBytes(2);
            
			this.state = State.PAYLOAD_OFFSET;
			
			// deliberate fall through 
		}
		case PAYLOAD_OFFSET: {
            if (in.readableBytes() < 8) 
                return false;
			
            this.payloadoffset = in.readLong();
            
			this.state = State.PAYLOAD_SIZE;
			
			// deliberate fall through 
		}
		case PAYLOAD_SIZE: {
            if (in.readableBytes() < 3) 
                return false;
            
            this.paysize = in.readMedium();
			
			this.state = State.PAYLOAD;
			
			// deliberate fall through 
		}
		case PAYLOAD: {
			// return here, without any state reset, means we need more before we can decide what to do
            if (in.readableBytes() < this.paysize) 
                return false;
            
            // add Data only if there are some bytes, otherwise skip buffer allocation
            if (this.paysize > 0) {
            	ByteBuf bb = in.readSlice(this.paysize);
            	bb.retain();
            	this.payload = bb;
            }
		}
		}
		
		return true;
	}

	@Override
	public void release() {
		if (this.payload != null) {
			this.payload.release();
			this.payload = null;
		}
	}

	public void retain() {
		if (this.payload != null) 
			this.payload.retain();
		
	}
}
