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
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import divconq.util.ArrayUtil;

/**
 * Internal class, @see Memory.
 * 
 * @author Andy
 *
 */
public class MemoryChunk {
    private byte[] buffer = null;		// the capacity is buffer.Length
    private int length = 0;    			// effective length of content
    private int position = 0;

    public MemoryChunk(byte[] buffer) {
        if (buffer == null) 
        	buffer = new byte[0];
        
        this.buffer = buffer;
        this.length = buffer.length;
    }

    public MemoryChunk(byte[] buffer, int length) {
        if (buffer == null) {
        	buffer = new byte[0];
        	length = 0;
        }
        
        if (length > buffer.length) 
        	length = buffer.length;
        
        this.buffer = buffer;
        this.length = buffer.length;
    }

    public MemoryChunk(MemoryChunk chunk) {
        if (chunk == null) 
        	buffer = new byte[0];
        else {
        	this.buffer = chunk.buffer;
        	this.length = chunk.length;
        }
    }

    public MemoryChunk(int capacity) {
        if (capacity < 0) 
        	capacity = 0;
        
        this.buffer = new byte[capacity];
    }

    public int getCapacity() {
        return this.buffer.length;
    }

    public int getLength() {
    	return this.length;
    }

    public int getPosition() {
    	return this.position;
    }
    
    public void setPosition(int value) {
        if (value < 0) 
        	value = 0;
        
        if (value >= this.buffer.length) 
        	value = this.buffer.length - 1;
        
        this.position = value;
    }

    public byte[] getBuffer() {
        return this.buffer;
    }

    public int read(byte[] buffer) {
    	return this.read(buffer, 0, buffer.length);
    }
    
    public int read(byte[] buffer, int offset, int count) {
    	if (count <= 0)
    		return 0;
    	
        if ((this.position + count) > this.length) 
        	count = this.length - this.position;
        
        if (ArrayUtil.blockCopy(this.buffer, this.position, buffer, offset, count)) {
        	this.position += count;
            return count;
        }

        return 0;
    }

    public int readByte() {
        if (this.position >= this.length) 
        	return -1;

        return (int)0xFF & this.buffer[this.position++];
    }

    public void setLength(int value) {
        if (value > this.buffer.length)
        	value = this.buffer.length;
        
        if (value < 0) 
        	value = 0;

        length = value;
    }

    public byte[] toArray() {
        return Arrays.copyOf(this.buffer, this.length);
    }

    public int write(byte[] buffer) {
        return this.write(buffer, 0, buffer.length);
    }

    public int write(byte[] buffer, int offset, int count) {
    	if (count <= 0)
    		return 0;
    	
        if ((this.position + count) > this.buffer.length) 
        	count = this.buffer.length - this.position;
        
        if (ArrayUtil.blockCopy(buffer, offset, this.buffer, this.position, count)) {
            this.position += count;

            if (this.position >= this.length) 
            	this.length = this.position;
            
            return count;
        }
    	
        return 0;
    }

    public int write(ByteBuffer buffer) {
    	if (buffer == null)
    		return 0;
    	
        int count = buffer.limit() - buffer.position();
        int offset = buffer.position();
        
        if ((this.position + count) > this.buffer.length) 
        	count = this.buffer.length - this.position;
        
        if (ArrayUtil.blockCopy(buffer, offset, this.buffer, this.position, count)) {
            this.position += count;
            buffer.position(offset + count);

            if (this.position >= this.length) 
            	this.length = this.position;
            
            return count;
        }
    	
        return 0;
    }

    public boolean writeByte(byte value) {
        if (this.position >= this.buffer.length) 
        	return false;

        this.buffer[this.position] = value;
        
        this.position++;
        
        if (this.position >= this.length) 
        	this.length = this.position;
        
        return true;
    }

    public long copyToStream(OutputStream stream, long count) {
    	if (count <= 0)
    		return 0;
    	
        if ((this.position + count) > this.buffer.length) 
        	count = this.buffer.length - this.position;
        
        try {
			stream.write(this.buffer, this.position, (int)count);
	        
	        this.position += count;

	        if (this.position >= this.length) 
	        	this.length = this.position;
	        
	        return count;
		} 
        catch (IOException e) {
        	// TODO logging
		}
        
        return 0;
    }
    

    /*
    public virtual void WriteFrom(Stream stream)
    {
        CheckIfClosedThrowDisposed();

        if (stream == null) throw new ArgumentNullException("stream");

        stream.Read(m_buffer, m_position, m_buffer.Length - m_position);
    }

    public virtual void ReadInto(Stream stream)
    {
        CheckIfClosedThrowDisposed();

        if (stream == null) throw new ArgumentNullException("stream");

        stream.Write(m_buffer, 0, m_length);
    }
    */
}

