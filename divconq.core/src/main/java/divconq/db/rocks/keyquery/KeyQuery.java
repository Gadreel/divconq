package divconq.db.rocks.keyquery;

import org.rocksdb.RocksIterator;

import divconq.db.Constants;
import divconq.db.DatabaseException;
import divconq.db.rocks.RocksInterface;
import divconq.db.util.ByteUtil;
import divconq.lang.Memory;
import divconq.util.HexUtil;

public class KeyQuery {
	protected KeyLevel first = null;
	protected RocksInterface adapter = null;
	protected RocksIterator it = null;
	
	protected boolean browseMode = false;
	protected byte[] key = null;
	protected byte[] value = null;
	
	public void setBrowseMode(boolean v) {
		this.browseMode = v;
	}
	
	// levels pasted in may not be reused elsewhere, now owned/mutable by this object
	public KeyQuery(RocksInterface adapter, KeyLevel... levels) {
		if ((levels == null) || (levels.length == 0))
			throw new IllegalArgumentException("KeyQuery missing levels");
		
		this.adapter = adapter;
		this.first = levels[0];
		
		KeyLevel lastWild = null;
		
		// setup the chain
		for (int i = 1; i < levels.length; i++) {
			levels[i-1].next = levels[i];
			
			if (levels[i] instanceof WildcardKeyLevel) 
				lastWild = levels[i]; 
		}
		
		if (lastWild != null)
			((WildcardKeyLevel)lastWild).setFinalWild(true);
		
		this.it = this.adapter.iterator();
	}
	
	// return the key - null when done
	public byte[] nextKey() {
		if (!this.it.isValid())
			this.it.seekToFirst();
		else
			this.it.next();
		
		while (true) {
			// because Query should never include omega, and db should always contain omega, this condition
			// should never occur. KeyQuery should not be used to find Omega.
			if (!this.it.isValid()) {
				System.out.println(">>>>>> Key Query got to invalid position <<<<<<<<<<<<<<<<<");
				return null;
			}
			
			byte[] key = this.it.key();
			
			if (key[0] == Constants.DB_TYPE_MARKER_OMEGA) {
				System.out.println(">>>>>> Key Query got to Omega <<<<<<<<<<<<<<<<<");
				return null;
			}
			
			Memory keyMod = new Memory(key);
			
			if (this.first.contains(key, this.browseMode, keyMod)) {
				if (key.length > keyMod.getLength()) {
					key = keyMod.toArray();
					this.key = key;
					
					try {
						this.value = this.adapter.get(key);
					} 
					catch (DatabaseException x) {
						// TODO error msg
						this.value = null;
					}
					
					if (this.value == null)
						this.value = Constants.DB_EMPTY_ARRAY;
				}
				else {
					this.key = key;
					this.value = it.value();
				}
				
				return key;
			}
			
			byte[] nextkey = this.first.next();
			
			if (nextkey == null) {
				System.out.println(">>>>>> Key Query got no more suggestions <<<<<<<<<<<<<<<<<");
				return null;
			}
			
			System.out.println("last key: " + HexUtil.bufferToHex(key));
			System.out.println("sugg key: " + HexUtil.bufferToHex(nextkey));
			
			// if nextkey <= current key return null because the key levels have run out of suggestions
			if (ByteUtil.compareKeys(key, nextkey) >= 0) {
				System.out.println(">>>>>> Key Query got stale suggestion <<<<<<<<<<<<<<<<<");
				return null;
			}
			
			this.it.seek(nextkey);
		}
	}
	
	public byte[] key() {
		return this.key;
	}
	
	public byte[] value() {
		return this.value;
	}
}
