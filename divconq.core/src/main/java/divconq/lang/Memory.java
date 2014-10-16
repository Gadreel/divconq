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
package divconq.lang;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import divconq.io.IReader;
import divconq.lang.chars.Utf8Decoder;
import divconq.lang.chars.Utf8Encoder;
import divconq.util.ArrayUtil;

/**
 * Memory is a flexible way to manipulate bytes or characters in RAM.  Memory
 * is auto-expanding, read and write, and takes both byte and character input 
 * (UTF-9) natively
 * 
 * @author Andy
 *
 */
public class Memory implements IReader {
    protected int chunkSize = 4096;    
    protected List<MemoryChunk> buffers = new ArrayList<MemoryChunk>();
    protected int length = 0;    // effective length of content, the capacity is total of all buffers
    protected int position = 0;
    protected Utf8Decoder decoder = null;
    
    public Memory() {
    }

    /**
     * @param chunkSize initial size, how much to grow by as memory auto-expands
     */
    public Memory(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    /**
     * Use for view on a now static memory chunk.  Position is independent of original, but buffers are shared.
     * 
     * @param memstream memory to create a view of, shares memory but will not expand with original 
     */
    public Memory(Memory memstream) {
        if (memstream == null)
            return;

        
        for (MemoryChunk mc : memstream.buffers) 
        	this.buffers.add(new MemoryChunk(mc));
        
        this.length = memstream.length;
        //this.position = memstream.position;
        this.chunkSize = memstream.chunkSize;
    }
    
    /**
     * @param firstbuffer initial bytes  
     */
    public Memory(byte[] firstbuffer) {
    	this.chunkSize = firstbuffer.length;
    	this.length = chunkSize;
    	this.position = firstbuffer.length;
        this.buffers.add(new MemoryChunk(firstbuffer));
    }
    
    /**
     * @param firstbuffer initial bytes
     * @param length cropped to
     */
    public Memory(byte[] firstbuffer, int length) {
    	this.chunkSize = firstbuffer.length;
    	
    	if (length < 0)
    		length = 0;
    	
    	this.length = length;
    	this.position = length;
        this.buffers.add(new MemoryChunk(firstbuffer, length));
    }

    public Memory(CharSequence chars) {
    	// TODO this could be more efficient, see write below
		this(Utf8Encoder.encode(chars));
	}

	/**
     * @return how much does memory grow by when current buffer is used up
     */
    public int getChunkSize() { 
    	return this.chunkSize;
    }

    protected void allocateNext() {
    	this.buffers.add(new MemoryChunk(this.chunkSize));
    }

    /**
     * @return what is the total allocated memory so far
     */
    public int getCapacity() {
        return this.buffers.size() * this.chunkSize;
    }

    /**
     * @param value force a greater allocation of memory (does not reduce)
     */
    public void setCapacity(int value) {
        while ((this.buffers.size() * this.chunkSize) < value)
        	this.allocateNext();
    }

    /**
     * @return how many bytes have been written
     */
    public int getLength() {
        return this.length;
    }

    /**
     * @return byte currently pointing to for a read or a write
     */
    public int getPosition() {
        return this.position;
    }

    /**
     * @param value which byte to use for the next read or write
     */
    public void setPosition(int value) {
        if (value < 0) 
        	value = 0;

        this.setCapacity(value);   // if needed, grow capacity to accommodate position
        
        this.position = value;
    }

    protected MemoryChunk linkPositionToBuffer() {
        int idx = (int)(this.position / this.chunkSize);

        if (idx >= this.buffers.size()) 
        	return null;

        MemoryChunk chunk = this.buffers.get(idx);
        chunk.setPosition((int)(this.position % this.chunkSize));
    	return chunk;
    }

    /**
     * Read from current position in memory into a byte array.  Increments the
     * the position accordingly.
     * 
     * @param buffer destination for copy
     * @param offset within destination
     * @param count of bytes to copy
     * @return number of bytes that were copied, less than count if end of memory was hit
     */
    public int read(byte[] buffer, int offset, int count) {
        if (buffer == null) 
        	return 0;
        
        if ((offset < 0) || (count <= 0))
        	return 0;
        
        if ((buffer.length - offset) < count) 
        	return 0;

        if ((this.position + count) > this.length) 
        	count = this.length - this.position;

        int left = count;

        do {
        	MemoryChunk chunk = this.linkPositionToBuffer();
            
            if (chunk == null) 
            	break;

            int read = chunk.read(buffer, offset, left);

            if (read == 0)
            	break;
            
            left -= read;
            this.position += read;
            offset += read;
        } while (left > 0);

        return count;
    }

    /**
     * Read the byte at current position and increment position.
     * 
     * @return byte read or -1 if end of memory
     */
    public int readByte() {
        if (this.position >= this.length) 
        	return -1;

        MemoryChunk chunk = this.linkPositionToBuffer();
        
        if (chunk == null) 
        	return -1;

        int b = chunk.readByte();
        
        this.position++;

        return b;
    }

    /**
     * Read the byte(s) at current position until a full character (utf-8) has been 
     * read.  Increment position accordingly.
     * 
     * @return character read or -1 if end of memory
     */
    public int readChar() {
    	int b = this.readByte();
    	
    	// decoder is created on demand only
    	if (this.decoder == null)
    		this.decoder = new Utf8Decoder();
    	else 
    		this.decoder.reset();		// in case last call ended mid character (see readCharRestricted)
    	
    	try {
	        while (b != -1) {
	            if (!this.decoder.readByteNeedMore((byte)b, true))
	                return this.decoder.getCharacterAndReset();
	            
	            b = this.readByte();
	        }
    	}
    	catch (Exception x) {
    		// TODO
    	}
        
        return -1;
    }
    
    @Override
    public void close() {
    }

    /**
     * Read the bytes at current position until a full character (utf-8) has been 
     * read.  Increment position accordingly.  Keep reading until end or number
     * of bytes has been read.  This method will not leave memory position 
     * part way through a string, but return to start of character that exceeds
     * "bytes".  Special characters ", \, \n, \t all count as 2 chars in case
     * they are escaped later.
     * 
     * @param bytes max bytes (not chars) to read
     * @return character read or -1 if end of memory
     */
    public CharSequence readCharsCapped(int bytes) {
    	StringBuilder sb = new StringBuilder();
    	
    	if (bytes < 1)
    		return sb;
    	
    	int finalpos = this.position + bytes;
    	int charstartpos = this.position;
    	int b = this.readByte();
    	
    	// decoder is created on demand only
    	if (this.decoder == null)
    		this.decoder = new Utf8Decoder();
    	else 
    		this.decoder.reset();		// in case last call ended mid character

    	try {
	        while ((b != -1) && (this.position < finalpos)) {
	            if (!this.decoder.readByteNeedMore((byte)b, true)) {
	            	char ch = (char)this.decoder.getCharacterAndReset();
	                sb.append(ch);
	                charstartpos = this.position;
	                
	                if ((ch == '\\') || (ch == '\"') || (ch == '\n') || (ch == '\t'))
	                	finalpos--;		// reign in for escape chars
	            }
	            
	            b = this.readByte();
	        }
	        
	        if (this.position >= finalpos)
	        	this.position = charstartpos;
    	}
    	catch (Exception x) {
    		// TODO
    	}
        
        return sb;
    }
    
    /**
     * Copy the entire contents of a byte array into Memory.  Increment position accordingly.
     * 
     * @param buffer source bytes
     * @return number of bytes written
     */
    public int write(byte[] buffer) {
    	if (buffer != null)
    		return this.write(buffer, 0, buffer.length);
    	
    	return 0;
    }

    /**
     * Copy the part of the contents of a byte array into Memory.  Increment position accordingly.
     * 
     * @param buffer source bytes
     * @param offset in source
     * @param count number of bytes to copy
     * @return number of bytes written
     */
    public int write(byte[] buffer, int offset, int count) {
        if (buffer == null) 
        	return 0;
        
        if ((offset < 0) || (count <= 0))
        	return 0;
        
        if (buffer.length - offset < count) 
        	return 0;

        this.setCapacity(this.position + count);  // ensure we can write this

        int left = count;

        do {
        	MemoryChunk chunk = this.linkPositionToBuffer();

            if (chunk == null) 
            	break;

            int write = (int)(this.chunkSize - chunk.getPosition());
            
            if (write > left) 
            	write = left;

            write = chunk.write(buffer, offset, write);
            
            if (write < 1)
            	break;

            left -= write;
            this.position += write;
            offset += write;

        } while (left > 0);

        if (this.position > this.length) 
        	this.length = this.position;
        
        return count - left;
    }

    /**
     * Copy the entire contents of a ByteBuffer into Memory.  Increment position accordingly.
     * 
     * @param buffer source buffer
     * @return number of bytes written
     */
    public int write(ByteBuffer buffer) {
        if (buffer == null) 
        	return 0;

        int count = buffer.limit() - buffer.position();
        this.setCapacity(this.position + count);  // ensure we can write this
        int left = count;

        do {
        	MemoryChunk chunk = this.linkPositionToBuffer();

            if (chunk == null) 
            	break;

            int write = chunk.write(buffer);
            
            if (write < 1)
            	break;

            left -= write;
            this.position += write;

        } while (left > 0);

        if (this.position > this.length) 
        	this.length = this.position;
        
        return count - left;
    }

    /**
     * Copy the entire contents of an InputStream into Memory.  Increment position accordingly.
     * 
     * @param s stream to read from
     * @return number of bytes written
     */
    public int copyFromStream(InputStream s) throws IOException {
    	return this.copyFromStream(s, Integer.MAX_VALUE);  // read any amount
    }

    /**
     * Copy the some of the contents of an InputStream into Memory.  Increment position accordingly.
     * 
     * @param s stream to read from
     * @param count number of bytes to read from stream
     * @return number of bytes written
     */
    public int copyFromStream(InputStream s, int count) throws IOException {
        if ((s == null) || (count <= 0)) 
        	return 0;

        int left = count;
        int amt = 0;
        byte[] buff = new byte[4096];

        while (left > 0) {
			amt = s.read(buff, 0, (left > buff.length) ? buff.length : left);
			
			if (amt <= 0)
				break;
			
			this.write(buff, 0, amt);
			left -= amt;
        } 
        
        return count - left;
    }
    
    /**
     * Write a single byte into Memory.  Increment position accordingly.
     * 
     * @param value byte to write
     * @return true if able to write
     */
    public boolean writeByte(byte value) {
        this.setCapacity(this.position + 1);

        MemoryChunk chunk = this.linkPositionToBuffer();

        if (chunk == null) 
        	return false;

        if (!chunk.writeByte(value))
        	return false;

        this.position++;
        
        if (this.position > this.length) 
        	this.length = this.position;
        
        return true;
    }
	
	/**
     * Write a single character into Memory as UTF-8.  Increment position accordingly.
     * 
	 * @param ch character to write
	 */
	public void writeChar(int ch) {
		this.write(Utf8Encoder.encode(ch));
	}
	
	/**
     * Write a string into Memory as UTF-8.  Increment position accordingly.
     * 
	 * @param str string to write
	 */
	public void write(CharSequence str) {
		// TODO this could be more efficient - encode <= 64 bytes at a time and add
		this.write(Utf8Encoder.encode(str));
	}
	
	public void writeLine(CharSequence str) {
		// TODO this could be more efficient - encode <= 64 bytes at a time and add
		this.write(Utf8Encoder.encode(str));
		this.writeChar('\n');
	}
	
	public void writeLine() {
		this.writeChar('\n');
	}

    /**
     * Change the apparent number of bytes written.  If less than current length then
     * bytes are truncated.  If greater than current then new bytes are added with 
     * what ever value may have previously been in them (be careful).
     *   
     * @param value number of bytes we claim to contain
     */
    public void setLength(int value) {
        if (value < 0) 
        	value = 0;

        if (value > this.getCapacity())
            this.setCapacity(value);

        if (value < this.position)
        	this.position = value;

        this.length = value;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
    	return this.toChars().toString();
    }
    
    /**
     * Assumes the entire content is UTF-8 characters and formats them into a string.
     * Ignores position completely.
     * 
     * @return memory as string
     */
    public CharSequence toChars() {
    	int oldpos = this.getPosition();
    	
    	StringBuilder32 sb = new StringBuilder32();
    	
    	// decoder is created on demand only
    	if (this.decoder == null)
    		this.decoder = new Utf8Decoder();
    	else 
    		this.decoder.reset();		// in case last call ended mid character

    	this.setPosition(0);
    	
    	try {
	    	int b = this.readByte();
	    	
	    	while (b != -1) {
	            if (!this.decoder.readByteNeedMore((byte)b, true))
	                sb.append(this.decoder.getCharacterAndReset());
	            
	            b = this.readByte();
	    	}
    	}
    	catch (Exception x) {
    		// TODO
    	}
    	
    	this.setPosition(oldpos);
    	
    	return sb;
    }

    /**
     * Copies all of Memory into a single byte array.  Ignores position.
     * 
     * @return all bytes from Memory 
     */
    public byte[] toArray() {
        int count = this.length;

        if (count < 0) 
        	count = 0;

        byte[] outBuffer = new byte[count];
        
        if (count == 0) 
        	return outBuffer;
        
        int dstpos = 0;

        for (MemoryChunk chunk : this.buffers) {
            int write = chunk.getLength();
            
            if (!ArrayUtil.blockCopy(chunk.getBuffer(), 0, outBuffer, dstpos, write)) 
            	break;
            
            dstpos += write;
        }

        return outBuffer;
    }

    /**
     * Internal
     * 
     * @param pos entry to get
     * @return bytes from entry
     */
    public byte[] getBufferEntry(int pos) {
        return this.buffers.get(pos).getBuffer();
    }

	/**
	 * Access (read) Memory as an Input Stream
	 * 
	 * @return the stream
	 */
	public InputStream getInputStream() {
		return new InputStream() {
			@Override
			public int read() throws IOException {
				return Memory.this.readByte();
			}
		};
	}

	/**
	 * Copy Memory from current position forward into an output stream.
	 * 
	 * @param stream destination stream
	 * @return bytes copied
	 */
	public long copyToStream(OutputStream stream) {
        return this.copyToStream(stream, Long.MAX_VALUE);
		
	}

    /**
	 * Copy Memory from current position forward into an output stream.
	 * 
	 * @param stream destination stream
     * @param count number of bytes to copy into stream
     * @return bytes copied
     */
    public long copyToStream(OutputStream stream, long count) {
        if ((stream == null) || (count <= 0) || (this.position >= this.length)) 
        	return 0;

        if (this.position > (this.length - count)) 
        	count = this.length - this.position;

        long left = count;

        do {
        	MemoryChunk chunk = this.linkPositionToBuffer();
            
            if (chunk == null) 
            	break;

            long write = chunk.copyToStream(stream, left);

            if (write == 0)
            	break;
            
            left -= write;
            this.position += write;
        } while (left > 0);
        
		try {
			stream.flush();
		} 
		catch (IOException e) {
			// TODO logging
		}

        return count;
    }
}
