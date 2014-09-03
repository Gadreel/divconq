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
package divconq.io;

import java.io.Reader;

public class CharSequenceReader extends Reader implements IReader {
	protected CharSequence seq = null;
	protected int pos = 0;
    protected int mark = 0;
	
	public CharSequenceReader(CharSequence seq) {
		this.seq = seq != null ? seq : "";
	}

	@Override
	public int readChar() {
		return this.read();
	}
	
	@Override
	public void close() {
		this.pos = 0;
		this.mark = 0;
	}

    @Override
    public void mark(int readAheadLimit) {
        this.mark = this.pos;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public int read() {
		if (this.pos < this.seq.length()) {
			int i = this.pos;
			this.pos++;
			return this.seq.charAt(i);
		}
		
		return -1;
    }

    @Override
    public int read(final char[] array, final int offset, final int length) {
        if (this.pos >= this.seq.length()) 
            return -1;

        if (array == null) 
            throw new NullPointerException("Character array is missing");

        if (length < 0 || offset < 0 || offset + length > array.length) 
            throw new IndexOutOfBoundsException("Array Size=" + array.length +
                    ", offset=" + offset + ", length=" + length);

        int count = 0;
        
        for (int i = 0; i < length; i++) {
            final int c = read();
            
            if (c == -1) {
                return count;
            }
            
            array[offset + i] = (char)c;
            count++;
        }
        return count;
    }

    @Override
    public void reset() {
        this.pos = mark;
    }

    @Override
    public long skip(final long n) {
        if (n < 0) 
            throw new IllegalArgumentException(
                    "Number of characters to skip is less than zero: " + n);

        if (this.pos >= this.seq.length()) 
            return -1;

        int dest = (int)Math.min(this.seq.length(), this.pos + n);
        int count = dest - this.pos;
        this.pos = dest;
        
        return count;
    }

    @Override
    public String toString() {
        return this.seq.toString();
    }	
}
