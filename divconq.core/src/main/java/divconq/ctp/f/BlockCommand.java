/* ************************************************************************
#
#  DivConq
#
#  http://divconq.com/
#
#  Copyright:
#    Copyright 2014 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package divconq.ctp.f;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import io.netty.buffer.ByteBuf;
import divconq.ctp.CtpCommand;
import divconq.ctp.CtpConstants;
import divconq.ctp.IStreamCommand;
import divconq.hub.Hub;
import divconq.lang.chars.Utf8Decoder;
import divconq.lang.chars.Utf8Encoder;
import divconq.util.IOUtil;

public class BlockCommand extends CtpCommand implements IStreamCommand {
	public Map<Integer, byte[]> headers = new HashMap<>();
	public ByteBuf data = null;
	public boolean eof = false;
	
	public ByteBuf getData() {
		return this.data;
	}
	
	public void setData(ByteBuf v) {
		this.data = v;
	}

	public void setIsFolder(boolean v) {
		this.headers.put(CtpConstants.CTP_F_ATTR_IS_FOLDER, v ? new byte[] { 0x01 } : new byte[] { 0x00 } );
	}
	
	public boolean isFolder() {
		byte[] attr = headers.get(CtpConstants.CTP_F_ATTR_IS_FOLDER);
		
		if (attr == null)
			return false;
		
		return attr[0] == 0x01;
	}

	public void setSize(long size) {
		this.headers.put(CtpConstants.CTP_F_ATTR_SIZE, IOUtil.longToByteArray(size));
	}
	
	public long getSize() {
		byte[] attr = headers.get(CtpConstants.CTP_F_ATTR_SIZE);
		
		if (attr == null)
			return 0;
		
		return IOUtil.byteArrayToLong(attr);
	}

	public void setModTime(long millis) {
		this.headers.put(CtpConstants.CTP_F_ATTR_MODTIME, IOUtil.longToByteArray(millis));
	}

	public void setPermissions(int v) {
		this.headers.put(CtpConstants.CTP_F_ATTR_PERMISSIONS, IOUtil.intToByteArray(v));
	}

	public void setOffset(long v) {
		this.headers.put(CtpConstants.CTP_F_ATTR_FILE_OFFSET, IOUtil.longToByteArray(v));
	}
	
	public void setPath(CharSequence v) {
		this.headers.put(CtpConstants.CTP_F_ATTR_PATH, Utf8Encoder.encode(v));
	}
	
	public String getPath() {
		byte[] attr = headers.get(CtpConstants.CTP_F_ATTR_PATH);
		
		if (attr == null)
			return null;
		
		return Utf8Decoder.decode(attr).toString();
	}
	
	public void setEof(boolean v) {
		this.eof = v;
	}
	
	public BlockCommand() {
		super(CtpConstants.CTP_F_CMD_STREAM_BLOCK);
	}
	
	@Override
	public ByteBuf encode() {
		int size = 1 + 1;  // code + CTP_F_BLOCK_TYPE_
		
		int type = 0;
		
		int hdrcnt = this.headers.size();
		
		if (hdrcnt > 0) {
			type |= CtpConstants.CTP_F_BLOCK_TYPE_HEADER;
			size += hdrcnt * 4 + 2;		// each header is at least 4 bytes (2 attr, 2 len) + ATTR_END is 2 bytes
			
			for (byte[] val : this.headers.values())
				size += val.length;
		}
		
		if (this.data != null) {
			type |= CtpConstants.CTP_F_BLOCK_TYPE_CONTENT;
			size += 3 + this.data.readableBytes();
		}
		
		if (this.eof)
			type |= CtpConstants.CTP_F_BLOCK_TYPE_EOF;
		
		// build buffer
		ByteBuf bb = Hub.instance.getBufferAllocator().buffer(size);
		
		bb.writeByte(this.cmdCode);
		bb.writeByte(type);
		
		// add header if any
		if (hdrcnt > 0) {
			for (Entry<Integer, byte[]> val : this.headers.entrySet()) {
				bb.writeShort(val.getKey());
				bb.writeShort(val.getValue().length);
				bb.writeBytes(val.getValue());
			}
			
			// end of list of headers
			bb.writeShort(CtpConstants.CTP_F_ATTR_END);
		}
		
		// add content if any
		if (this.data != null) {
			bb.writeLong(this.streamOffset);
			bb.writeMedium(this.data.readableBytes());
			bb.writeBytes(this.data);
		}
		
		return bb;
	}

	@Override
	public void release() {
		if (this.data != null)
			this.data.release();
	}

	public void copyAttributes(BlockCommand cmd) {
		for (Entry<Integer, byte[]> attr : cmd.headers.entrySet())
			this.headers.put(attr.getKey(), attr.getValue());
		
		this.eof = cmd.eof;
	}

	public void copyAttributes(FileDescriptor file) {
		for (Entry<Integer, byte[]> attr : file.headers.entrySet())
			this.headers.put(attr.getKey(), attr.getValue());
		
		this.eof = file.eof;
	}

	////////////////////////////////////////////////////////////////////
	// decode support
	////////////////////////////////////////////////////////////////////
	
    enum State {
        BLOCK_TYPE,
        HEADER_ATTR,
        HEADER_SIZE,
        HEADER_VALUE,
        STREAM_OFFSET,
        PAYLOAD_SIZE,
        PAYLOAD,
        DONE
    }

    protected State state = State.BLOCK_TYPE;
	protected int blocktype = 0;
	protected boolean skipHeaders = false;
	protected boolean skipPayload= false;
	protected int currattr = 0;
	protected int currasize = 0;
	protected long streamOffset = 0;
	protected int paysize = 0;

	@Override
	public boolean decode(ByteBuf in) {
		while (this.state != State.DONE) {
			switch (this.state) {
			case BLOCK_TYPE: {
	            if (in.readableBytes() < 1) 
	                return false;
	            
	            this.blocktype = in.readUnsignedByte();
				
	            this.eof = ((this.blocktype & CtpConstants.CTP_F_BLOCK_TYPE_EOF) != 0);
	            this.skipHeaders = ((this.blocktype & CtpConstants.CTP_F_BLOCK_TYPE_HEADER) == 0);
	            this.skipPayload = ((this.blocktype & CtpConstants.CTP_F_BLOCK_TYPE_CONTENT) == 0);
	            
	            // completely done, exit the loop and decode
	            if (this.skipHeaders && this.skipPayload) {
					this.state = State.DONE;
					break;
	            }
	            
	            // to skip headers, go back to loop
	            if (this.skipHeaders) {
					this.state = State.STREAM_OFFSET;
					break;
	            }
	            
				this.state = State.HEADER_ATTR;
				
				// deliberate fall through 
			}
			case HEADER_ATTR: {
	            if (in.readableBytes() < 2) 
	                return false;
	            
	            this.currattr = in.readShort();
	            
	            // done with headers, go back to loop to skip down to payload
	            if (this.currattr == CtpConstants.CTP_F_ATTR_END) {
		            if (this.skipPayload) 
						this.state = State.DONE;
					else
						this.state = State.STREAM_OFFSET;
					
					break;
	            }
				
				this.state = State.HEADER_SIZE;
				
				// deliberate fall through 
			}
			case HEADER_SIZE: {
	            if (in.readableBytes() < 2) 
	                return false;
	            
	            this.currasize = in.readShort();
	            
	            // an empty attribute is like a flag - present but no data
	            // go on to next header
	            if (this.currasize == 0) {
	            	this.headers.put(this.currattr, new byte[0]);
	            	this.currattr = 0;
					this.state = State.HEADER_ATTR;
					break;
	            }
				
				this.state = State.HEADER_VALUE;
				
				// deliberate fall through 
			}
			case HEADER_VALUE: {
	            if (in.readableBytes() < this.currasize) 
	                return false;
	            
	            byte[] val = new byte[this.currasize];
	            
	            in.readBytes(val);
				
            	this.headers.put(this.currattr, val);
            	
	            this.currattr = 0;
            	this.currasize = 0;
            	
				this.state = State.HEADER_ATTR;
				
				break;
			}
			case STREAM_OFFSET: {
	            if (in.readableBytes() < 8) 
	                return false;
	            
	            this.streamOffset = in.readLong();
				
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
	            	this.data = bb;
	            }
				
				this.state = State.DONE;
				
				// deliberate fall through 
			}
			case DONE: {
				break;
			}
			}
		}
		
		return true;
	}

}
