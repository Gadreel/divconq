package divconq.db.rocks.keyquery;

import divconq.db.Constants;
import divconq.db.util.ByteUtil;
import divconq.lang.Memory;

public class MatchKeyLevel extends KeyLevel {
	protected byte[] match = null;
	protected boolean hasMatch = false;
	
	public MatchKeyLevel(Object match) {
		this.match = ByteUtil.buildKey(match);
	}
	
	public MatchKeyLevel(byte[] match) {
		this.match = match;
	}
	
	// return -1 if key is less than match, 0 for equal and 1 for key is greater than match
	
	@Override
	public int compare(byte[] key, int offset, boolean browseMode, Memory browseKey) {
		int mlen = this.match.length;
		
		// compare the arrays, as far as they both have bytes
		for (int i = 0; i < Math.min(mlen, key.length - offset); i++) {
			if (this.match[i] < key[offset + i]) {
				this.resetLast();
				return 1;
			}
			
			if (this.match[i] > key[offset + i]) {
				this.resetLast();
				return -1;
			}
		}
		
		// Presumably they are an exact match up to the lowest shared slot
		// thus is key is shorter it comes before us
		if (offset + mlen > key.length) {
			this.resetLast();
			return -1;
		}
		
		/*
		if (this.next == null) {
			// can be nothing past me, if there is it is bigger than me
			if (key.length > offset + mlen)
				return 1;
			
			return 0;
		}
		*/		
		
		// can be nothing past me if there is no `next`
		if ((this.next == null) && (key.length > offset + mlen)) {
			if (!browseMode)
				return 1;
			
			if (this.hasMatch) 
				return 1;
			
			// treat as match - need special handling
			browseKey.setLength(offset + mlen); 
			
			this.hasMatch = true;
			return 0;
		}

		if (this.next == null) {
			if (browseMode)
				this.hasMatch = true;
			
			return 0;
		}
		
		// look for field separator - check length
		if (offset + mlen + 1 > key.length) {
			this.next.resetLast();
			return -1;
		}
		
		// look for field separator - check byte
		if (Constants.DB_TYPE_MARKER_ALPHA != key[offset + this.match.length]) {
			this.next.resetLast();
			return 1;
		}
		
		return this.next.compare(key, offset + mlen + 1, browseMode, browseKey);
	}

	@Override
	public void resetLast() {
		if (this.hasMatch)
			this.hasMatch = false;
		
		if (this.next != null)
			this.next.resetLast();
	}
	
	@Override
	public void buildSeek(Memory mem) {
		mem.write(this.match);
		
		if (this.next != null) {
			mem.writeByte(Constants.DB_TYPE_MARKER_ALPHA);
			this.next.buildSeek(mem);
		}
		else if (this.hasMatch) {
			mem.writeByte((byte)0x01);
		}
	}
}