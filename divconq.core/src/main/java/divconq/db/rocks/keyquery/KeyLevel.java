package divconq.db.rocks.keyquery;

import divconq.lang.Memory;

abstract public class KeyLevel {
	protected KeyLevel next = null;
	
	public boolean contains(byte[] key, boolean browseMode, Memory browseKey) {
		return this.compare(key, 0, browseMode, browseKey) == 0;
	}
	
	public byte[] next() {
		//if (!this.nextSeek())
		//	return null;
		
		Memory key = new Memory(2048);
		
		this.buildSeek(key);
		
		return key.toArray();
	}
	
	// return 0 if does not contain, N past offset when does contain
	abstract public int compare(byte[] key, int offset, boolean browseMode, Memory browseKey);
	abstract public void buildSeek(Memory mem);

	abstract public void resetLast();
}