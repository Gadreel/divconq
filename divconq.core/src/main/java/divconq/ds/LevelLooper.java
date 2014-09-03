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
package divconq.ds;

/*
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.ArrayUtils;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;

import divconq.lang.chars.Utf8Decoder;
import divconq.lang.chars.Utf8Encoder;
import divconq.struct.CompositeParser;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
*/

public class LevelLooper {
	/*
	protected DBIterator it = null;
	protected byte[] anchor = null;
	protected boolean reverse = false;
	
	public LevelLooper(DB db, String key) {
		this(db, Utf8Encoder.encode(key), false);
	}
	
	public LevelLooper(DB db, String key, boolean reverse) {
		this(db, Utf8Encoder.encode(key), true);
	}
	
	public LevelLooper(DB db, byte[] key, boolean reverse) {
		this.anchor = key;
		this.reverse = reverse;
		
		this.it = db.iterator();
		this.it.seek( reverse 
				? ArrayUtils.addAll(this.anchor, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff) 
				: this.anchor);
	}
	
	private boolean testEntry(Entry<byte[], byte[]> n) {
		if (n == null)
			return false;
		
		byte[] key = n.getKey();
		
		if (key.length < this.anchor.length)
			return false;
		
		for (int i = 0; i < this.anchor.length; i++)
			if (key[i] != this.anchor[i])
				return false;
		
		return true;
	}
		
	public boolean hasNext() {
		try {
			return this.testEntry(this.reverse ? this.it.peekPrev() : this.it.peekNext());
		}
		catch (NoSuchElementException x) {
			return false;
		}
	}
	
	public Entry<byte[], byte[]> next() {
		Entry<byte[], byte[]> n = this.reverse ? this.it.prev() : this.it.next();
		
		if (!this.testEntry(n))
			return null;
		
		return n;
	}
	
	public byte[] nextValue() {
		Entry<byte[], byte[]> n = this.next();
		
		if (n == null)
			return null;
		
		return n.getValue();
	}
	
	public String nextAsString() {
		Entry<byte[], byte[]> n = this.next();
		
		if (n == null)
			return null;
		
		CharSequence c = Utf8Decoder.decode(n.getValue());
		
		if (c == null)
			return null;
		
		return c.toString();		
	}
	
	public CompositeStruct nextAsComposite() {
		String s = this.nextAsString();
		
		if (s == null)
			return null;
		
		return CompositeParser.parseJson(s).getResult();		
	}
	
	public RecordStruct nextAsRecord() {
		return (RecordStruct) this.nextAsComposite();
	}
	
	public ListStruct nextAsList() {
		return (ListStruct) this.nextAsComposite();
	}
	*/
}
