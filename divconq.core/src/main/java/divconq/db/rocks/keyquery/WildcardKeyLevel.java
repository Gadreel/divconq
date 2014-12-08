package divconq.db.rocks.keyquery;

import divconq.db.Constants;
import divconq.db.util.ByteUtil;
import divconq.lang.Memory;
import divconq.util.ArrayUtil;

// any key this level
public class WildcardKeyLevel extends KeyLevel {
	protected byte[] last = Constants.DB_ALPHA_MARKER_ARRAY;
	protected boolean skipchild = false;
	protected boolean isFinalWild = false;

	public void setFinalWild(boolean v) {
		this.isFinalWild = v;
	}
	
	@Override
	public int compare(byte[] key, int offset, boolean browseMode, Memory browseKey) {
		byte[] old = this.last;
		
		this.last = Constants.DB_ALPHA_MARKER_ARRAY;
		this.skipchild = false;
		
		int mlen = 0;
		
		// the only type allowed to contain zeros is Number, it is fixed
		if (key[offset] == Constants.DB_TYPE_NUMBER) {
			mlen = 18;
		}
		else {
			for (int i = offset; i < key.length; i++) {
				if (key[i] == Constants.DB_TYPE_MARKER_ALPHA)
					break;
				
				mlen++;
			}
		}
		
		if (offset + mlen > key.length) {
			this.resetLast();
			return -1;
		}
		
		// can be nothing past me if there is no `next`
		if ((this.next == null) && (key.length > offset + mlen)) {
			if (!browseMode)
				return 1;
			
			if (ByteUtil.keyContains(key, offset, old, mlen)) {
				this.last = new byte[mlen + 1];		// one longer and set last is 0x00			
				ArrayUtil.blockCopy(key, offset, this.last, 0, mlen);
			
				this.last[this.last.length - 1] = 0x01;
				
				return 1;
			}
			
			// treat as match - need special handling
			this.last = new byte[mlen];		// exact match			
			ArrayUtil.blockCopy(key, offset, this.last, 0, mlen);
			
			browseKey.setLength(offset + mlen); 
			
			return 0;
		}
		
		if (this.next == null) {
			this.last = new byte[mlen + 1];		// one longer and set last is 0x00			
			ArrayUtil.blockCopy(key, offset, this.last, 0, mlen);
		
			this.last[this.last.length - 1] = 0x01;
			return 0;
		}
				
		// look for field separator - check length
		if (offset + mlen + 1 > key.length) {
			this.last = new byte[mlen];		// exact match			
			ArrayUtil.blockCopy(key, offset, this.last, 0, mlen);
			
			this.next.resetLast();			
			return -1;
		}
		
		// look for field separator - check byte
		if (Constants.DB_TYPE_MARKER_ALPHA != key[offset + mlen]) {
			this.next.resetLast();			
			return 1;
		}
		
		int ret = this.next.compare(key, offset + mlen + 1, browseMode, browseKey);
		
		// when there is a child in the chain, only the final wild can peek forward
		if ((ret <= 0) || !this.isFinalWild) {
			this.last = new byte[mlen];		// exact match			
			ArrayUtil.blockCopy(key, offset, this.last, 0, mlen);
		}
		else {
			this.last = new byte[mlen + 1];		// one longer and set last is 0x00			
			ArrayUtil.blockCopy(key, offset, this.last, 0, mlen);
			this.last[this.last.length - 1] = 0x01;
			this.skipchild = true;
		}
		
		return ret;
	}

	@Override
	public void resetLast() {
		this.last = Constants.DB_ALPHA_MARKER_ARRAY;
		this.skipchild = false;
				
		if (this.next != null)
			this.next.resetLast();
	}
	
	@Override
	public void buildSeek(Memory mem) {
		mem.write(this.last);		// last byte has a trailing 1 (in addition to ALPHA) forcing the next key to be sought 
		
		if (!this.skipchild && (this.next != null)) {
			mem.writeByte(Constants.DB_TYPE_MARKER_ALPHA);
			this.next.buildSeek(mem);
		}
	}
	
}