package divconq.db.rocks.keyquery;

import divconq.db.util.ByteUtil;
import divconq.lang.Memory;

// a range of possible matches 
public class RangeKeyLevel extends KeyLevel {
	protected byte[] from = null;
	protected byte[] to = null;
	
	public RangeKeyLevel(Object from, Object to) {
		this.from = ByteUtil.buildKey(from);
		this.to = ByteUtil.buildKey(to);
	}
	
	public RangeKeyLevel(byte[] from, byte[] to) {
		this.from = from;
		this.to = to;
	}

	@Override
	public int compare(byte[] key, int offset, boolean browseMode,
			Memory browseKey) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void buildSeek(Memory mem) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resetLast() {
		// TODO Auto-generated method stub
		
	}
	
}