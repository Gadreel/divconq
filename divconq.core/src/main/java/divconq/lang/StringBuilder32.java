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

// TODO convert to use Memory
public class StringBuilder32 implements CharSequence {
	protected StringBuilder sb = new StringBuilder();
	
	@Override
    public int length() {
        return this.sb.length(); 
    }

	@Override
    public String toString() {
    	return this.sb.toString();
    }
	
	public void reset() {
		this.sb = new StringBuilder();
	}
	
	public void append(CharSequence str) {
		if (str != null)
			this.sb.append(str);
	}
	
	public void appendLine(CharSequence str) {
		if (str != null)
			this.sb.append(str);
		
		this.sb.append('\n');
	}
	
	public void appendLine() {
		this.sb.append('\n');
	}
	
	public void append(char c) {
		this.sb.append(c);
	}
    
    // appends a utf32 character or surrogate pair
    public void append(int value) throws Exception {
        if (value < 0 || value > 0x10FFFF)
            throw new Exception("UTF builder error: The argument must be from 0 to 0x10FFFF.");

        if (0xD800 <= value && value <= 0xDFFF)
            throw new Exception("UTF builder error: The argument must not be in surrogate pair range.");

        if (value < 0x10000) {
            this.sb.append((char)value);
        }
        else {
            value -= 0x10000;  // we have a surrogate pair
            this.sb.append((char)((value >> 10) + 0xD800));
            this.sb.append((char)(value % 0x0400 + 0xDC00));
        }
    }

	@Override
	public char charAt(int index) {
		return this.sb.charAt(index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return this.sb.subSequence(start, end);
	}
	
	
}
