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
import java.io.File;
import java.util.Map.Entry;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.Range;
import org.iq80.leveldb.ReadOptions;
import org.iq80.leveldb.Snapshot;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;
import org.iq80.leveldb.impl.Iq80DBFactory;

import divconq.hub.Hub;
import divconq.lang.OperationResult;
import divconq.lang.chars.Utf8Decoder;
import divconq.lang.chars.Utf8Encoder;
import divconq.struct.CompositeParser;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
*/

public class LevelDb /* implements DB */ {
	/*
	protected DB db = null;

	public void open(String file) {
		Options options = new Options();
		options.createIfMissing(true);
		
		try {
			this.db =  Iq80DBFactory.factory.open(new File(file), options);
		} 
		catch (Exception x) {
			System.out.println("Error testing database: " + x);
		}
	}
	
	@Override
	public void close() {
		  try {
			db.close();
		  }
		  catch (Exception e) {
		  }
	}

	@Override
	public WriteBatch createWriteBatch() {
		return this.db.createWriteBatch();
	}

	@Override
	public void delete(byte[] arg0) throws DBException {
		this.db.delete(arg0);
	}

	@Override
	public Snapshot delete(byte[] arg0, WriteOptions arg1) throws DBException {
		return this.db.delete(arg0, arg1);
	}

	// use null type to skip validation
	public void delete(String key) throws DBException {
		this.db.delete(Utf8Encoder.encode(key));
	}

	@Override
	public byte[] get(byte[] arg0) throws DBException {
		return this.db.get(arg0);
	}

	public String get(String arg0) throws DBException {
		CharSequence seq = Utf8Decoder.decode(this.db.get(Utf8Encoder.encode(arg0)));
		
		if (seq == null)
			return null;

		return seq.toString();
	}

	public RecordStruct getRecord(String key) throws DBException {
		CharSequence seq = Utf8Decoder.decode(this.db.get(Utf8Encoder.encode(key)));
		return (RecordStruct) CompositeParser.parseJson(seq).getResult();
	}

	public ListStruct getList(String key) throws DBException {
		CharSequence seq = Utf8Decoder.decode(this.db.get(Utf8Encoder.encode(key)));
		return (ListStruct) CompositeParser.parseJson(seq).getResult();
	}

	public CompositeStruct getComposite(String key) throws DBException {
		CharSequence seq = Utf8Decoder.decode(this.db.get(Utf8Encoder.encode(key)));
		return CompositeParser.parseJson(seq).getResult();
	}

	@Override
	public byte[] get(byte[] arg0, ReadOptions arg1) throws DBException {
		return this.db.get(arg0, arg1);
	}

	@Override
	public long[] getApproximateSizes(Range... arg0) {
		return this.db.getApproximateSizes(arg0);
	}

	@Override
	public String getProperty(String arg0) {
		return this.db.getProperty(arg0);
	}

	@Override
	public Snapshot getSnapshot() {
		return this.db.getSnapshot();
	}

	@Override
	public DBIterator iterator() {
		return this.db.iterator();
	}

	@Override
	public DBIterator iterator(ReadOptions arg0) {
		return this.db.iterator(arg0);
	}

	@Override
	public void put(byte[] arg0, byte[] arg1) throws DBException {
		this.db.put(arg0, arg1);
	}

	public void put(String arg0, String arg1) throws DBException {
		this.db.put(Utf8Encoder.encode(arg0), Utf8Encoder.encode(arg1));
	}

	// use null type to skip validation
	public void put(String key, String type, CompositeStruct v) throws DBException {
		if (type != null) {
			OperationResult or = Hub.instance.getSchema().validateType(v, type);
			
			if (or.hasErrors())
				throw new DBException(or.getMessage());
		}
		
		this.db.put(Utf8Encoder.encode(key), (v == null) ? null : Utf8Encoder.encode(v.toPrettyString()));
	}

	@Override
	public Snapshot put(byte[] arg0, byte[] arg1, WriteOptions arg2) throws DBException {
		return this.db.put(arg0, arg1, arg2);
	}

	@Override
	public void resumeCompactions() {
		this.db.resumeCompactions();
	}

	@Override
	public void suspendCompactions() throws InterruptedException {
		this.db.suspendCompactions();
	}

	@Override
	public void write(WriteBatch arg0) throws DBException {
		this.db.write(arg0);
	}

	@Override
	public Snapshot write(WriteBatch arg0, WriteOptions arg1) throws DBException {
		return this.db.write(arg0, arg1);
	}

	public LevelLooper loop(String key) {
		return new LevelLooper(this, key);
	}

	public LevelLooper loopBackwards(String key) {
		return new LevelLooper(this, key, true);
	}

	public void dump(String key) {
		LevelLooper cloop = this.loop(key);
		
		while (cloop.hasNext()) {
			Entry<byte[], byte[]> n = cloop.next();
			
			System.out.println("entry: " + Utf8Decoder.decode(n.getKey()) + "\t\t" + Utf8Decoder.decode(n.getValue()) );
		}
	}

	public void dump(String key, int top) {
		LevelLooper cloop = this.loop(key);
		int cnt = 0;
		
		while (cloop.hasNext()) {
			Entry<byte[], byte[]> n = cloop.next();
			
			System.out.println("entry: " + Utf8Decoder.decode(n.getKey()) + "\t\t" + Utf8Decoder.decode(n.getValue()) );
			
			cnt++;
			
			if (cnt == top)
				break;
		}
	}
	*/
}
