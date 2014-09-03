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

import divconq.lang.StringBuilder32;

public class Latin1Decoder implements ICharDecoder {
    protected int m_charatcer = 0;

	@Override
	public int getCharacter() {
		return this.m_charatcer;
	}

	@Override
	public boolean needsMore() {
		return false;
	}

	@Override
	public int getCharacterAndReset() {
        int tchar = this.m_charatcer;
        this.reset();
        return tchar;
	}

	@Override
	public void reset() {
		this.m_charatcer = 0;
	}

	@Override
	public CharSequence processBytes(byte[] values) {
        StringBuilder32 sb = new StringBuilder32();

        try {
	        for (int pos = 0; pos < values.length; pos++) 
	            if (!this.readByteNeedMore(values[pos], true))
	                sb.append(this.getCharacterAndReset());
        }
        catch (Exception x) {
        	// TODO
        }
        
        return sb;
	}

	@Override
	public boolean readByteNeedMore(byte bch, boolean safe) throws Exception {
		int ch = 0xFF & bch;

        if (safe && ((ch < (int)0x9) || ((ch < (int)0x20) && (ch > (int)0xD)) || (ch == (int)0x7F)))
            throw new Exception("Latin1 decoder encountered an error: Control characters not allowed.");

        this.m_charatcer = ch;
        
		return false;
	}
}
