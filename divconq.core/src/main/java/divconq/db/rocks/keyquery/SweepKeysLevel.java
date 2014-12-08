package divconq.db.rocks.keyquery;

import divconq.db.util.ByteUtil;
import divconq.lang.Memory;

// like a range + expando - with multiple keys
// SweepKeysLevel is meant to be used alone with no other levels above or below
// it returns everything for a range, everything under, and does not support extra 
// browseMode features - it is here as a debugging tool to succinctly show what is 
// truly in the database

public class SweepKeysLevel extends KeyLevel {
	protected byte[] from = null;
	protected byte[] to = null;
	
	public SweepKeysLevel() {
	}
	
	public SweepKeysLevel(Object[] from, Object[] to) {
		this.from = ByteUtil.buildKey(from);
		this.to = ByteUtil.buildKey(to);
	}
	
	public SweepKeysLevel(byte[] from, byte[] to) {
		this.from = from;
		this.to = to;
	}
	
	public void setFrom(Object... v) {
		this.from = ByteUtil.buildKey(v);
	}
	
	public void setTo(Object... v) {
		this.to = ByteUtil.buildKey(v);
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
