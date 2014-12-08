package divconq.db.rocks.keyquery;

import divconq.db.Constants;
import divconq.lang.Memory;
import divconq.util.ArrayUtil;

// any key this level or under
public class ExpandoKeyLevel extends WildcardKeyLevel {

	// ignore child if any, everything under me is returned
	@Override
	public int compare(byte[] key, int offset, boolean browseMode, Memory browseKey) {
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
		
		if (offset + mlen > key.length)
			return -1;
		
		this.last = new byte[mlen + 1];		// one longer and set last is 0x00			
		ArrayUtil.blockCopy(key, offset, this.last, 0, mlen);
	
		this.last[this.last.length - 1] = 0x01;
		return 0;
	}	
}