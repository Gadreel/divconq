package divconq.db.rocks;

import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import divconq.db.Constants;
import divconq.db.DatabaseException;
import divconq.db.DatabaseInterface;
import divconq.db.util.ByteUtil;
import divconq.lang.Memory;
import divconq.lang.op.OperationContext;

public class RocksInterface extends DatabaseInterface {
	protected DatabaseManager dbman = null;
	protected RocksDB db = null;
	
	public RocksInterface(DatabaseManager dbman) {
		this.dbman = dbman;
		this.db = dbman.db;
	}
	
	public RocksIterator iterator() {
		return this.db.newIterator();
	}
	
	public byte[] get(byte[] key) throws DatabaseException {
		try {
			return this.db.get(key);
		} 
		catch (RocksDBException x) {
			throw new DatabaseException(x);
		}
	}

	@Override
	public void put(byte[] key, byte[] value) throws DatabaseException {
		try {
			this.db.put(key, value);
		} 
		catch (RocksDBException x) {
			throw new DatabaseException(x);
		}
	}
	
	@Override
	public boolean isAuditDisabled() {
		return this.dbman.isAuditDisabled();
	}
	
	@Override
	public Long inc(byte[] key, int amt) throws DatabaseException {
		try {
			return this.dbman.inc(key, amt);
		} 
		catch (RocksDBException x) {
			throw new DatabaseException(x);
		}
	}

	@Override
	public boolean isSet(byte[] key) throws DatabaseException {
		RocksIterator it = this.db.newIterator();
		
		it.seek(key);
		
		// found an exact match
		boolean ret = (ByteUtil.compareKeys(it.key(), key) == 0);
		
		it.dispose();
		
		return ret;
	}
	
	@Override
	public String allocateSubkey() {
		return this.dbman.allocateSubkey();
	}

	@Override
	public boolean hasAny(byte[] key) throws DatabaseException {
		RocksIterator it = this.db.newIterator();
		
		it.seek(key);
		
		// found an sub match or exact match
		boolean ret = ByteUtil.keyStartsWith(it.key(), key);
		
		it.dispose();
		
		return ret;
	}

	@Override
	public byte[] getOrNextPeerKey(byte[] key, byte[] peer) throws DatabaseException {
		RocksIterator it = this.db.newIterator();
		
		if (peer == null)
			peer = Constants.DB_EMPTY_ARRAY;
		
		Memory mem = new Memory(key.length + 1 + peer.length);
		mem.write(key);
		mem.writeByte(Constants.DB_TYPE_MARKER_ALPHA);
		mem.write(peer);
		
		it.seek(mem.getBufferEntry(0));
		
		if (!it.isValid()) {
			it.dispose();
			return null;
		}
		
		byte[] fnd = it.key();
		
		it.dispose();
		
		// match not found, peer key doesn't exist at all and was skipped
		if (!ByteUtil.keyStartsWith(fnd, key))
			return null;
		
		mem = new Memory(fnd);
		mem.setPosition(key.length + 1);
		
		// return just 1 part - it might the same as peer or it might be the next peer
		return ByteUtil.extractNextDirect(mem);
	}

	@Override
	public byte[] getOrPrevPeerKey(byte[] key, byte[] peer) throws DatabaseException {
		RocksIterator it = this.db.newIterator();
		
		if (peer == null)
			peer = Constants.DB_OMEGA_MARKER_ARRAY;
		
		Memory mem = new Memory(key.length + 1 + peer.length);
		mem.write(key);
		mem.writeByte(Constants.DB_TYPE_MARKER_ALPHA);
		mem.write(peer);
		
		it.seek(mem.getBufferEntry(0));
		
		if (it.isValid()) {
			byte[] fnd = it.key();

			/*
			// ------------- TODO --------------
			System.out.println("looking for match: " + HexUtil.bufferToHex(key));
			System.out.println("           before: " + HexUtil.bufferToHex(mem.getBufferEntry(0)));
			System.out.println("              got: " + HexUtil.bufferToHex(fnd));
			// ---------------------------------
			*/
			
			// match found, peer key exists 
			if (ByteUtil.keyStartsWith(fnd, key)) {
				mem = new Memory(fnd);
				mem.setPosition(key.length + 1);
				
				it.dispose();
				
				// ------------- TODO --------------
				/*
				List<Object> keyParts = ByteUtil.extractKeyParts(fnd);
				
				for (Object p : keyParts)
					System.out.print((p == null) ? " / " : p.toString() + " / ");
				
				System.out.println();
				*/
				// ---------------------------------
				
				// return just 1 part - it might the same as peer or it might be the next peer
				return ByteUtil.extractNextDirect(mem);
			}
		}
		
		// otherwise peer does not exist at all, go back 1
		it.prev();
		
		if (!it.isValid()) {
			it.dispose();
			return null;
		}
		
		byte[] fnd = it.key();
		
		it.dispose();

		// ------------- TODO --------------
		/*
		System.out.println("looking for match: " + HexUtil.bufferToHex(key));
		System.out.println("           before: " + HexUtil.bufferToHex(mem.getBufferEntry(0)));
		System.out.println("              got: " + HexUtil.bufferToHex(fnd));
		*/
		// ---------------------------------

		// match found, prev peer key exists 
		if (ByteUtil.keyStartsWith(fnd, key)) {
			mem = new Memory(fnd);
			mem.setPosition(key.length + 1);
			
			// ------------- TODO --------------
			/*
			List<Object> keyParts = ByteUtil.extractKeyParts(fnd);
			
			for (Object p : keyParts)
				System.out.print((p == null) ? " / " : p.toString() + " / ");
			
			System.out.println();
			*/
			// ---------------------------------
			
			// return just 1 part - it might the same as peer or it might be the next peer
			return ByteUtil.extractNextDirect(mem);
		}
		
		return null;
	}

	@Override
	public byte[] nextPeerKey(byte[] key, byte[] peer) throws DatabaseException {
		RocksIterator it = this.db.newIterator();
		
		//if (peer == null)
		//	peer = Constants.DB_EMPTY_ARRAY;
		
		int len = 1;
		
		if (key != null)
			len += key.length + 1;
		
		if (peer != null)
			len += peer.length;
		
		Memory mem = new Memory(len);
		
		if (key != null) {
			mem.write(key);
			mem.writeByte(Constants.DB_TYPE_MARKER_ALPHA);
		}
		
		if (peer != null) 
			mem.write(peer);
		
		mem.writeByte((byte)0x01);		// forces us to look for next key - all subkeys of peer are skipped
		
		//System.out.println("Next on:  " + HexUtil.bufferToHex(mem.getBufferEntry(0)));
		
		it.seek(mem.getBufferEntry(0));
		
		if (!it.isValid()) {
			it.dispose();
			return null;
		}
		
		byte[] fnd = it.key();
		
		//System.out.println("Next fnd: " + HexUtil.bufferToHex(fnd));
		
		it.dispose();
		
		//System.out.println("looking for match: " + HexUtil.bufferToHex(key));
		//System.out.println("           after: " + HexUtil.bufferToHex(peer));
		//System.out.println("              got: " + HexUtil.bufferToHex(fnd));
		
		// match not found, next peer key doesn't exist at all and was skipped
		if ((key != null) && !ByteUtil.keyStartsWith(fnd, key))
			return null;
		
		//System.out.println("match!");
		
		mem = new Memory(fnd);
		mem.setPosition((key == null) ? 0 : key.length + 1);
		
		// return just 1 part - the next peer
		return ByteUtil.extractNextDirect(mem);
	}

	@Override
	public byte[] prevPeerKey(byte[] key, byte[] peer) throws DatabaseException {
		RocksIterator it = this.db.newIterator();
		
		if (peer == null)
			peer = Constants.DB_OMEGA_MARKER_ARRAY;
		
		Memory mem = new Memory(key.length + 1 + peer.length);
		mem.write(key);
		mem.writeByte(Constants.DB_TYPE_MARKER_ALPHA);
		mem.write(peer);
		
		it.seek(mem.getBufferEntry(0));
		
		/*
		if (!it.isValid()) {
			it.dispose();
			return null;
		}
		
		byte[] fnd2 = it.key();
		
		// ------------- TODO --------------
		System.out.println("looking for match: " + HexUtil.bufferToHex(key));
		System.out.println("           before: " + HexUtil.bufferToHex(mem.getBufferEntry(0)));
		System.out.println("              got: " + HexUtil.bufferToHex(fnd2));
				
		List<Object> keyParts2 = ByteUtil.extractKeyParts(fnd2);
		
		for (Object p : keyParts2)
			System.out.print((p == null) ? " / " : p.toString() + " / ");
		
		System.out.println();
		*/
		// ---------------------------------
		
		// regardless if peer exists or does not exist, go back 1 key
		it.prev();
		
		if (!it.isValid()) {
			it.dispose();
			return null;
		}
		
		byte[] fnd = it.key();
		
		// ------------- TODO --------------
		/*
		System.out.println("            got 2: " + HexUtil.bufferToHex(fnd));
		
		List<Object> keyParts = ByteUtil.extractKeyParts(fnd);
		
		for (Object p : keyParts)
			System.out.print((p == null) ? " / " : p.toString() + " / ");
		
		System.out.println();
		*/
		// ---------------------------------
		
		it.dispose();
		
		// match found, prev peer key exists 
		if (ByteUtil.keyStartsWith(fnd, key)) {
			mem = new Memory(fnd);
			mem.setPosition(key.length + 1);
			
			//System.out.println("match!");
			
			// return just 1 part - it might the same as peer or it might be the next peer
			return ByteUtil.extractNextDirect(mem);
		}
		
		//System.out.println("no match!");
		
		return null;
	}

	@Override
	public void kill(byte[] key) {
		RocksIterator it = this.db.newIterator();
		
		it.seek(key);
		
		while (ByteUtil.keyStartsWith(it.key(), key)) {
			try {
				db.remove(it.key());
			} 
			catch (RocksDBException x) {
				OperationContext.get().error("Error removing key: " + x);
			}
			
			it.next();
		}
		
		it.dispose();
	}
}
