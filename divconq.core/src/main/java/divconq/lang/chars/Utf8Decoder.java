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
package divconq.lang.chars;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import divconq.lang.Memory;
import divconq.lang.StringBuilder32;

public class Utf8Decoder implements ICharDecoder {
    private int leftBits = 0;
    private int leftSoFar = 0;
    private int leftSize = 0;
    private int charatcer = 0;
    private boolean needMore = false;

    // TODO consider maybe array of char instead of string builder? then return new String(array)?
    // prescan for size
    protected List<StringBuilder32> result = new ArrayList<StringBuilder32>();
    private int lastSpecial = -1;
    
	@Override
	public int getCharacter() {
		return this.charatcer;
	}
	
	public int getLastSpecialCharacter() {
		return this.lastSpecial;
	}

	@Override
	public boolean needsMore() {
		return (this.needMore && (this.leftSize != 0));
	}

	@Override
	public int getCharacterAndReset() {
        int tchar = charatcer;
        this.reset();
        return tchar;
	}

	@Override
	public void reset() {
		this.leftBits = 0;
		this.leftSoFar = 0;
		this.leftSize = 0;
		this.charatcer = 0;
		this.needMore = false;
		this.lastSpecial = -1;
	}

	@Override
	public CharSequence processBytes(byte[] values) {
		if (values == null)
			return null;
		
        return this.processBytes(values, values.length);
	}

	public CharSequence processBytes(byte[] values, int len) {
		if (values == null)
			return null;
		
        StringBuilder32 sb = new StringBuilder32();

        try {
	        for (int pos = 0; pos < len; pos++) 
	            if (!this.readByteNeedMore(values[pos], true))
	                sb.append(this.getCharacterAndReset());
        }
        catch (Exception x) {
        	// TODO
        }
        
        return sb;
	}

	public List<StringBuilder32> processBytesSplit(ByteBuffer buffer, Special partSep) {
        StringBuilder32 sb = null;
        
        // continue to build up strings from last call
        if (this.result.size() > 0)
        	sb = this.result.get(this.result.size() - 1);
        else {
        	sb = new StringBuilder32();
        	this.result.add(sb);
        }
        
        this.lastSpecial = -1;
        
        try {
        	while (buffer.hasRemaining()) {
	            if (this.readByteNeedMore(buffer.get(), true))
	            	continue;
	        
	            if (this.lastSpecial != -1) {
	            	if (partSep.getCode() != this.lastSpecial) {
	                    List<StringBuilder32> res = this.result;
	            		this.result = new ArrayList<StringBuilder32>();		
	            		return res;
	            	}

                	sb = new StringBuilder32();
                	this.result.add(sb);
	                this.reset();
	                continue;
	            }
	            
	            sb.append(this.getCharacterAndReset());
	        }	        
        }
        catch (Exception x) {
        	// TODO
        }
    	
    	return null;
	}

	// look for string from buffer, up to the char.  no special chars allowed 
	public StringBuilder32 processBytesUntil(ByteBuffer buffer, int partChar) {
        StringBuilder32 sb = null;
        
        // continue to build up strings from last call
        if (this.result.size() > 0)
        	sb = this.result.get(this.result.size() - 1);
        else {
        	sb = new StringBuilder32();
        	this.result.add(sb);
        }
        
        this.lastSpecial = -1;
        
        try {
        	while (buffer.hasRemaining()) {
	            if (this.readByteNeedMore(buffer.get(), true))
	            	continue;
	        
	            // ignore special chars and CR
	            if ((this.lastSpecial != -1) || (this.charatcer == '\r')) {
	                this.lastSpecial = -1;
	                continue;
	            }
            
            	if (partChar == this.charatcer) {
            		this.result = new ArrayList<StringBuilder32>();		
            		return sb;
            	}
	            
	            sb.append(this.getCharacterAndReset());
	        }	        
        }
        catch (Exception x) {
        	// TODO
        }
    	
    	return null;
	}
	
	public StringBuilder32 processBytesUntil(ByteBuffer buffer, Special partSep) {
        StringBuilder32 sb = null;
        
        // continue to build up strings from last call
        if (this.result.size() > 0)
        	sb = this.result.get(this.result.size() - 1);
        else {
        	sb = new StringBuilder32();
        	this.result.add(sb);
        }
        
        this.lastSpecial = -1;
        
        try {
        	while (buffer.hasRemaining()) {
	            if (this.readByteNeedMore(buffer.get(), true))
	            	continue;
	        
	            if (this.lastSpecial != -1) {
	            	if (partSep.getCode() == this.lastSpecial) {
	            		this.result = new ArrayList<StringBuilder32>();		
	            		return sb;
	            	}
	            }
	            
	            sb.append(this.getCharacterAndReset());
	        }	        
        }
        catch (Exception x) {
        	// TODO
        }
    	
    	return null;
	}

	public StringBuilder32 processBytesUntilSpecial(ByteBuffer buffer) {
		if (buffer == null)
			return null;
		
        StringBuilder32 sb = null;
        
        // continue to build up strings from last call
        if (this.result.size() > 0)
        	sb = this.result.get(this.result.size() - 1);
        else {
        	sb = new StringBuilder32();
        	this.result.add(sb);
        }
        
        this.lastSpecial = -1;
        
        try {
        	while (buffer.hasRemaining()) {
	            if (this.readByteNeedMore(buffer.get(), true))
	            	continue;
	        
	            if (this.lastSpecial != -1) {
            		this.result = new ArrayList<StringBuilder32>();		
            		return sb;
	            }
	            
	            sb.append(this.getCharacterAndReset());
	        }	        
        }
        catch (Exception x) {
        	// TODO
        }
    	
    	return null;
	}

	public StringBuilder32 processBytesUntilSpecial(ByteBuf buffer) {
		if (buffer == null)
			return null;
		
        StringBuilder32 sb = null;
        
        // continue to build up strings from last call
        if (this.result.size() > 0)
        	sb = this.result.get(this.result.size() - 1);
        else {
        	sb = new StringBuilder32();
        	this.result.add(sb);
        }
        
        this.lastSpecial = -1;
        
        try {
        	while (buffer.readableBytes() > 0) {
	            if (this.readByteNeedMore(buffer.readByte(), true))
	            	continue;
	        
	            if (this.lastSpecial != -1) {
            		this.result = new ArrayList<StringBuilder32>();		
            		return sb;
	            }
	            
	            sb.append(this.getCharacterAndReset());
	        }	        
        }
        catch (Exception x) {
        	// TODO
        }
    	
    	return null;
	}

	public StringBuilder32 processBytesUntilSpecial(Memory buffer) {
		if (buffer == null)
			return null;
		
        StringBuilder32 sb = null;
        
        // continue to build up strings from last call
        if (this.result.size() > 0)
        	sb = this.result.get(this.result.size() - 1);
        else {
        	sb = new StringBuilder32();
        	this.result.add(sb);
        }
        
        this.lastSpecial = -1;
        
        try {
        	while (buffer.readableBytes() > 0) {
	            if (this.readByteNeedMore((byte)buffer.readByte(), true))
	            	continue;
	        
	            if (this.lastSpecial != -1) {
            		this.result = new ArrayList<StringBuilder32>();		
            		return sb;
	            }
	            
	            sb.append(this.getCharacterAndReset());
	        }	        
        }
        catch (Exception x) {
        	// TODO
        }
    	
    	return null;
	}

	/*
	public boolean processBytesUntil(byte[] values, int offset, int len, Special sectionSep) {
        this.lastSpecial = -1;
        
        try {
	        for (int pos = offset; pos < len; pos++) { 
	            if (this.readByteNeedMore(values[pos], true))
	            	return false;
	        
	            if (this.lastSpecial != -1) {
	            	if (sectionSep.getCode() == this.lastSpecial)
	            		return true;
	            	
	                this.lastSpecial = -1;
	            }
	        }	               
        }
        catch (Exception x) {
        	// TODO
        }
        
    	return false;
	}
	*/

    private void processFirstByte(int ch, boolean safe) throws Exception {
        if (ch < (int)0x80) {
        	// unsafe characters get turned into spaces
            if (safe && ((ch < (int)0x9) || ((ch < (int)0x20) && (ch > (int)0xD)) || (ch == (int)0x7F))) {
            	this.lastSpecial = ch;
            	ch = (int)0x20;
            }
            
            this.charatcer = (int)ch;
            return;
        }

        this.needMore = true;

        if ((ch & (int)0xE0) == (int)0xC0) {
            // Double-byte UTF-8 character.
        	this.leftBits = (ch & (int)0x1F);
        	this.leftSoFar = 1;
        	this.leftSize = 2;
            return;
        }

        if ((ch & (int)0xF0) == (int)0xE0) {
            // Three-byte UTF-8 character.
        	this.leftBits = (ch & (int)0x0F);
        	this.leftSoFar = 1;
        	this.leftSize = 3;
            return;
        }

        if ((ch & (int)0xF8) == (int)0xF0) {
            // Four-byte UTF-8 character.
        	this.leftBits = (ch & (int)0x07);
        	this.leftSoFar = 1;
        	this.leftSize = 4;
            return;
        }

        if ((ch & (int)0xFC) == (int)0xF8) {
            // Five-byte UTF-8 character.
        	this.leftBits = (ch & (int)0x03);
        	this.leftSoFar = 1;
        	this.leftSize = 5;
            return;
        }

        if ((ch & (int)0xFE) == (int)0xFC) {
            // Six-byte UTF-8 character.
        	this.leftBits = (ch & (int)0x03);
        	this.leftSoFar = 1;
        	this.leftSize = 6;
            return;
        }

        throw new Exception("UTF decoder error: Invalid UTF-8 start character.");
    }

	@Override
	public boolean readByteNeedMore(byte bch, boolean safe) throws Exception {
		int ch = 0xFF & bch;
		
        // read first byte
        if (this.leftSoFar == 0) {
        	this.processFirstByte(ch, safe);
            return this.needMore;
        }

        // Process an extra byte in a multi-byte sequence.
        if ((ch & (int)0xC0) == (int)0x80) {
        	this.leftBits = ((this.leftBits << 6) | (ch & (int)0x3F));

            if (++this.leftSoFar >= this.leftSize) {
                // We have a complete character now.
                if (this.leftBits < (int)0x10000) {
                    // is it an overlong ?
                    boolean overlong = false;

                    switch (this.leftSize) {
                        case 2:
                            overlong = (this.leftBits <= 0x7F);
                            break;
                        case 3:
                            overlong = (this.leftBits <= 0x07FF);
                            break;
                        case 4:
                            overlong = (this.leftBits <= 0xFFFF);
                            break;
                        case 5:
                            overlong = (this.leftBits <= 0x1FFFFF);
                            break;
                        case 6:
                            overlong = (this.leftBits <= 0x03FFFFFF);
                            break;
                    }

                    if (overlong)
                        throw new Exception("UTF decoder error: Invalid UTF-8 sequence, overlong value.");
                    else if ((this.leftBits & 0xF800) == 0xD800)
                        throw new Exception("UTF decoder error: Invalid UTF-8 sequence, surrogate characters not allowed.");
                    else
                    	this.charatcer = this.leftBits;
                }
                else if (this.leftBits < (int)0x110000)
                	this.charatcer = this.leftBits;
                else
                    throw new Exception("UTF decoder error: Invalid UTF-8 sequence.");

                this.leftSize = 0;  // signal end
                return false;
            }
        }
        else
            throw new Exception("UTF decoder error: Invalid UTF-8 sequence.");

        return true;
	}

	// must pass in a complete buffer
	public static CharSequence decode(byte[] buffer) {
		if (buffer == null)
			return null;
		
		Utf8Decoder decoder = new Utf8Decoder();
		return decoder.processBytes(buffer);
	}
	
	// TODO by directly reading the byte buf, no copy
	public static CharSequence decode(ByteBuf buffer) {
		byte[] dest = new byte[buffer.readableBytes()];
		buffer.readBytes(dest);
		
		return Utf8Decoder.decode(dest);		
	}
	
	// TODO by directly reading the byte buf, no copy
	public static CharSequence decode(ByteBuf buffer, int max) {
		byte[] dest = new byte[Math.min(buffer.readableBytes(), max)];
		buffer.readBytes(dest);
		
		return Utf8Decoder.decode(dest);		
	}
}
