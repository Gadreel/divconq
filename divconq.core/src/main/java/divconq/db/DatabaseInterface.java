package divconq.db;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import divconq.db.util.ByteUtil;
import divconq.lang.BigDateTime;
import divconq.struct.Struct;

abstract public class DatabaseInterface {
	public void set(Object... list) throws DatabaseException {
		if ((list == null) || (list.length < 2))
			throw new IllegalArgumentException("SET list missing or too small");
		
		byte[] key = ByteUtil.buildKey(list, 0, list.length - 1);
		byte[] val = ByteUtil.buildValue(list[list.length - 1]);
		
		this.put(key, val);
	}
	
	public void set(byte[] key, Object value) throws DatabaseException {
		if (key == null) 
			throw new IllegalArgumentException("SET key missing");
		
		byte[] val = ByteUtil.buildValue(value);
		
		this.put(key, val);
	}
	
	public String getAsString(Object... list) throws DatabaseException {
		return Struct.objectToString(this.get(list));
	}
	
	public BigDecimal getAsDecimal(Object... list) throws DatabaseException {
		return Struct.objectToDecimal(this.get(list));
	}
	
	public Boolean getAsBoolean(Object... list) throws DatabaseException {
		return Struct.objectToBoolean(this.get(list));
	}
	
	public boolean getAsBooleanOrFalse(Object... list) throws DatabaseException {
		return Struct.objectToBooleanOrFalse(this.get(list));
	}
	
	public Long getAsInteger(Object... list) throws DatabaseException {
		return Struct.objectToInteger(this.get(list));
	}
	
	public BigInteger getAsBigInteger(Object... list) throws DatabaseException {
		return Struct.objectToBigInteger(this.get(list));
	}
	
	public LocalDate getAsDate(Object... list) throws DatabaseException {
		return Struct.objectToDate(this.get(list));
	}
	
	public LocalTime getAsTime(Object... list) throws DatabaseException {
		return Struct.objectToTime(this.get(list));
	}
	
	public DateTime getAsDateTime(Object... list) throws DatabaseException {
		return Struct.objectToDateTime(this.get(list));
	}
	
	public BigDateTime getAsBigDateTime(Object... list) throws DatabaseException {
		return Struct.objectToBigDateTime(this.get(list));
	}
	
	public Object get(Object... list) throws DatabaseException {
		if ((list == null) || (list.length < 1))
			throw new IllegalArgumentException("GET list missing or too small");
		
		byte[] key = ByteUtil.buildKey(list, 0, list.length);

		return ByteUtil.extractValue(this.get(key));
	}
	
	public byte[] getRaw(Object... list) throws DatabaseException {
		if ((list == null) || (list.length < 1))
			throw new IllegalArgumentException("GET list missing or too small");
		
		byte[] key = ByteUtil.buildKey(list, 0, list.length);

		return this.get(key);
	}
	
	public boolean isSet(Object... list) throws DatabaseException {
		if ((list == null) || (list.length < 1))
			throw new IllegalArgumentException("GET list missing or too small");
		
		byte[] key = ByteUtil.buildKey(list, 0, list.length);
		
		return this.isSet(key);
	}
	
	public boolean hasAny(Object... list) throws DatabaseException {
		if ((list == null) || (list.length < 1))
			throw new IllegalArgumentException("GET list missing or too small");
		
		byte[] key = ByteUtil.buildKey(list, 0, list.length);
		
		return this.hasAny(key);
	}
	
	public void kill(Object... list) throws DatabaseException {
		if ((list == null) || (list.length < 1))
			throw new IllegalArgumentException("GET list missing or too small");
		
		byte[] key = ByteUtil.buildKey(list, 0, list.length);

		this.kill(key);
	}
	
	public byte[] nextPeerKey(Object... list) throws DatabaseException {
		if ((list == null) || (list.length < 1))
			throw new IllegalArgumentException("GET list missing or too small");
		
		byte[] key = ByteUtil.buildKey(list, 0, list.length - 1);
		
		return this.nextPeerKey(key, ByteUtil.buildKey(list[list.length - 1]));
	}
	
	public byte[] prevPeerKey(Object... list) throws DatabaseException {
		if ((list == null) || (list.length < 1))
			throw new IllegalArgumentException("GET list missing or too small");
		
		byte[] key = ByteUtil.buildKey(list, 0, list.length - 1);
		
		return this.prevPeerKey(key, ByteUtil.buildKey(list[list.length - 1]));
	}
	
	public byte[] getOrNextPeerKey(Object... list) throws DatabaseException {
		if ((list == null) || (list.length < 1))
			throw new IllegalArgumentException("GET list missing or too small");
		
		byte[] key = ByteUtil.buildKey(list, 0, list.length - 1);
		
		return this.getOrNextPeerKey(key, ByteUtil.buildKey(list[list.length - 1]));
	}
	
	public byte[] getOrPrevPeerKey(Object... list) throws DatabaseException {
		if ((list == null) || (list.length < 1))
			throw new IllegalArgumentException("GET list missing or too small");
		
		byte[] key = ByteUtil.buildKey(list, 0, list.length - 1);
		
		return this.getOrPrevPeerKey(key, ByteUtil.buildKey(list[list.length - 1]));
	}
	
	public Long inc(Object... list) throws DatabaseException {
		if ((list == null) || (list.length < 1))
			throw new IllegalArgumentException("INC list missing or too small");
		
		byte[] key = ByteUtil.buildKey(list, 0, list.length);

		return this.inc(key, 1);
	}
	
	public Long dec(Object... list) throws DatabaseException {
		if ((list == null) || (list.length < 1))
			throw new IllegalArgumentException("DEC list missing or too small");
		
		byte[] key = ByteUtil.buildKey(list, 0, list.length);

		return this.inc(key, -1);
	}
	
	public Long inc(byte[] metakey) throws DatabaseException {
		return this.inc(metakey, 1);
	}
	
	abstract public boolean isSet(byte[] key) throws DatabaseException;
	abstract public boolean hasAny(byte[] key) throws DatabaseException;
	abstract public byte[] nextPeerKey(byte[] key, byte[] peer) throws DatabaseException;	
	abstract public byte[] prevPeerKey(byte[] key, byte[] peer) throws DatabaseException;	
	abstract public byte[] getOrNextPeerKey(byte[] key, byte[] peer) throws DatabaseException;	
	abstract public byte[] getOrPrevPeerKey(byte[] key, byte[] peer) throws DatabaseException;	
	
	abstract public Long inc(byte[] metakey, int amt) throws DatabaseException;
	abstract public byte[] get(byte[] key) throws DatabaseException;	
	abstract public void put(byte[] key, byte[] value) throws DatabaseException;
	abstract public void kill(byte[] key);
	
	abstract public boolean isAuditDisabled();

}
