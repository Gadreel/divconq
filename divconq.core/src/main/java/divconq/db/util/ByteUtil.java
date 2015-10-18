package divconq.db.util;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import divconq.db.Constants;
import divconq.lang.BigDateTime;
import divconq.lang.Memory;
import divconq.lang.chars.Utf8Decoder;
import divconq.lang.chars.Utf8Encoder;
import divconq.struct.CompositeStruct;
import divconq.struct.Struct;
import divconq.struct.builder.BuilderStateException;
import divconq.struct.builder.ObjectBuilder;
import divconq.struct.serial.BufferToCompositeParser;
import divconq.util.ArrayUtil;
import divconq.util.TimeUtil;
import divconq.xml.XElement;

public class ByteUtil {
	static final public DateTimeFormatter localDate = DateTimeFormat.forPattern("yyyyMMdd"); 
	static final public DateTimeFormatter localTime = DateTimeFormat.forPattern("HHmmssSSS"); 
	
	static public int compareKeys(byte[] a, byte[] b) {
		if ((a == null) && (b == null))
			return 0;
		
		if (a == null)
			return -1;
		
		if (b == null)
			return 1;
		
		for (int i = 0; i < Math.min(a.length, b.length); i++) {
			if (a[i] > b[i])
				return 1;
			
			if (a[i] < b[i])
				return -1;
		}
		
		if (a.length > b.length)
			return 1;
		
		if (a.length < b.length)
			return -1;
		
		return 0;
	}
	
	static public boolean keyStartsWith(byte[] key, byte[] part) {
		if (part.length > key.length)
			return false;
		
		for (int i = 0; i < part.length; i++) {
			if (key[i] != part[i])
				return false;
		}
		
		if (part.length == key.length)
			return true;
		
		return (key[part.length] == Constants.DB_TYPE_MARKER_ALPHA);
	}
	
	// key contains a part at offset
	static public boolean keyContains(byte[] key, int offset, byte[] part, int len) {
		if (len + offset > key.length)
			return false;
		
		for (int i = 0; i < len; i++) {
			if (key[i + offset] != part[i])
				return false;
		}
		
		return true;
	}
	
	static public byte[] buildKey(Object in) {
		if (in == null) 
			return null;
		
		Memory key = new Memory();
		
		ByteUtil.encodeValue(key, in, true);
		
		return key.toArray();
	}
	
	static public byte[] buildKey(Object... list) {
		return ByteUtil.buildKey(list, 0, list.length);
	}
	
	static public byte[] buildKey(Object[] list, int offset, int len) {
		if ((list == null) || (list.length <= offset))
			return null;
		
		Memory key = new Memory();
		
		for (int i = offset; i < (offset + len); i++) {
			ByteUtil.encodeValue(key, list[i], true);
			
			// add marker between keys but not after last key
			if (i < (offset + len - 1))
				key.writeByte(Constants.DB_TYPE_MARKER_ALPHA);
		}
		
		return key.toArray();
	}

	static public byte[] combineKeys(byte[] basekey, byte[] subid) {
		if (basekey == null)
			return subid;
		
		byte[] res = new byte[basekey.length + subid.length + 1];
		
		ArrayUtil.blockCopy(basekey, 0, res, 0, basekey.length);
		res[basekey.length] = Constants.DB_TYPE_MARKER_ALPHA;
		ArrayUtil.blockCopy(subid, 0, res, basekey.length + 1, subid.length);
		
		return res;
	}
	
	static public byte[] buildValue(Object in) {
		if (in == null) 
			return Constants.DB_EMPTY_ARRAY;
		
		Memory val = new Memory();
	
		ByteUtil.encodeValue(val, in, false);
		
		return val.toArray();
	}
	
	static public Object extractValue(byte[] value) {
		if ((value == null) || (value.length < 1))
			return null;
		
		Memory val = new Memory(value);		
		val.setPosition(0);
		
		return ByteUtil.extractNext(val);
	}
	
	static public List<Object> extractKeyParts(byte[] key) {
		if ((key == null) || (key.length < 1))
			return null;
		
		//int cnt = ByteUtil.countKeyParts(key);
		//Object[] ret = new Object[cnt];
		
		List<Object> ret = new ArrayList<>();
		
		Memory val = new Memory(key);
		val.setPosition(0);
		
		while (val.readableBytes() > 0) {
			ret.add(ByteUtil.extractNext(val));
			
			if (val.readableBytes() > 0) {
				int m = val.readByte();
				
				// TODO throw error?
				if (m != Constants.DB_TYPE_MARKER_ALPHA)
					return null;
			}
		}
		
		return ret;
	}
	
	/*
	static public int countKeyParts(byte[] key) {
		int cnt = 1;
		
		for (int i = 0; i < key.length; i++)
			if (key[i] == Constants.DB_TYPE_MARKER_ALPHA)
				cnt++;
		
		return cnt;
	}
	*/
	
	static public byte[] extractNextDirect(Memory val) {
		if ((val == null) || (val.readableBytes() < 1))
			return null;
		
		int pos = val.getPosition();
		int type = val.readByte();
		
		if (type == Constants.DB_TYPE_MARKER_OMEGA)
			return null;
		
		int mlen = 1;
		
		// the only type allowed to contain zeros is Number, it is fixed len
		if (type == Constants.DB_TYPE_NUMBER) {
			mlen = 18;
		}
		else {
			int b = val.readByte();
			
			while ((b != -1) && (b != Constants.DB_TYPE_MARKER_ALPHA)) {
				mlen++;
				b = val.readByte();
			}
			
			// something not right TODO
			//if (b == -1)
			//	return null;
		}
		
		byte[] ret = new byte[mlen];
		
		val.setPosition(pos);
		val.read(ret, 0, mlen);
		
		return ret;
	}
	
	static public Object extractNext(Memory val) {
		if ((val == null) || (val.readableBytes() < 1))
			return null;
		
		int type = val.readByte();
		
		if (type == -1)
			return null;
		
		if (type == Constants.DB_TYPE_NUMBER)
			return ByteUtil.dbNumberToNumber(val);
		
		if (type == Constants.DB_TYPE_STRING)
			return ByteUtil.dbStringToString(val);
		
		if (type == Constants.DB_TYPE_DATE_TIME)
			return ByteUtil.dbStringToDateTime(val);		
		
		if (type == Constants.DB_TYPE_BIG_DATE_TIME)
			return ByteUtil.dbStringToBigDateTime(val);		
		
		if (type == Constants.DB_TYPE_DATE)
			return ByteUtil.dbStringToDate(val);		
		
		if (type == Constants.DB_TYPE_TIME)
			return ByteUtil.dbStringToTime(val);		
		
		if (type == Constants.DB_TYPE_NULL)
			return null;		
		
		if (type == Constants.DB_TYPE_BOOLEAN)
			return ByteUtil.dbBooleanToBoolean(val);		
		
		// return as string as this is most commonly how the XML will be consumed
		if (type == Constants.DB_TYPE_XML)
			return ByteUtil.dbStringToString(val);

		if (type == Constants.DB_TYPE_COMPOSITE) 
			return ByteUtil.dbCompositeToComposite(val);
		
		// TODO support more		
		
		return null;
	}
	
	static public void encodeValue(Memory mem, Object v, boolean forKey) {
		v = Struct.objectToCore(v);
		
		if (v == null) {
			mem.writeByte(Constants.DB_TYPE_NULL);
			return;
		}
		
		if (v instanceof Boolean) {
			mem.writeByte(Constants.DB_TYPE_BOOLEAN);
			
			if ((Boolean)v) 
				mem.writeByte((byte)0x01);
			else
				mem.writeByte((byte)0x00);
				
			return;
		}
		
		if (v instanceof DateTime) {
			String val = TimeUtil.stampFmt.print((DateTime)v);
		
			mem.writeByte(Constants.DB_TYPE_DATE_TIME);
			mem.write(val);	
			
			return;
		}
		
		if (v instanceof BigDateTime) {
			String val = ((BigDateTime)v).toString();
		
			mem.writeByte(Constants.DB_TYPE_BIG_DATE_TIME);
			mem.write(val);	
			
			return;
		}
		
		if (v instanceof LocalDate) {
			String val = ByteUtil.localDate.print((LocalDate)v);
		
			mem.writeByte(Constants.DB_TYPE_DATE);
			mem.write(val);	
			
			return;
		}
		
		if (v instanceof LocalTime) {
			String val = ByteUtil.localTime.print((LocalTime)v);
		
			mem.writeByte(Constants.DB_TYPE_TIME);
			mem.write(val);	
			
			return;
		}
		
		if (v instanceof String) {
			String val = (String)v;
			
			if (forKey) {
				mem.writeByte(Constants.DB_TYPE_STRING);
				mem.write(val.substring(0, Math.min(val.length(), 1000)));		// up to 1000 characters
			}
			else {
				mem.writeByte(Constants.DB_TYPE_STRING);
				mem.write(val);	
			}
			
			return;
		}
		
		if (v instanceof Memory) {
			Memory val = (Memory)v;
			
			if (forKey) {
				// index is on size only
				ByteUtil.encodeValue(mem, val.getLength(), forKey);
				//mem.writeByte(Constants.DB_TYPE_BINARY);
				//mem.write(val.toArray(), 0, Math.min(val.getLength(), 1000));		// up to 1000 bytes
			}
			else {
				mem.writeByte(Constants.DB_TYPE_BINARY);
				mem.write(val.toArray());	
			}
			
			return;
		}
		
		if (v instanceof BigDecimal) {
			BigDecimal val = (BigDecimal)v;
			
			// TODO add support for DB_TYPE_NUMBER_SPECIAL
			
			mem.writeByte(Constants.DB_TYPE_NUMBER);
			mem.write(ByteUtil.decimalToDBNumber(val));	
			
			return;
		}
		
		if (v instanceof BigInteger) {
			BigInteger val = (BigInteger)v;
			
			// TODO add support for DB_TYPE_NUMBER_SPECIAL
			
			mem.writeByte(Constants.DB_TYPE_NUMBER);
			mem.write(ByteUtil.bigToDBNumber(val));	
			
			return;
		}
		
		if (v instanceof Long) {
			Long val = (Long)v;
			
			mem.writeByte(Constants.DB_TYPE_NUMBER);
			mem.write(ByteUtil.longToDBNumber(val));
			
			return;
		}
		
		if (v instanceof CompositeStruct) {
			try {
				if (forKey) {
					// index on size only
					
					// TODO create a size calculator method 
					// instead of doing the full encoding
					Memory m = ((CompositeStruct)v).toSerialMemory();					
					ByteUtil.encodeValue(mem, m.getLength(), forKey);
				}
				else {
					mem.writeByte(Constants.DB_TYPE_COMPOSITE);
					((CompositeStruct)v).toSerialMemory(mem);
				}
			} 
			catch (BuilderStateException x) {
				System.out.println("Error encoding db value: " + v);
				x.printStackTrace();
			}
			
			return;
		}
		
		if (v instanceof XElement) {
			if (forKey) {
				// index on size only
				
				// TODO create a size calculator method 
				// instead of doing the full encoding
				int m = Utf8Encoder.size(((XElement)v).toString());
				ByteUtil.encodeValue(mem, m, forKey);
			}
			else {
				mem.writeByte(Constants.DB_TYPE_XML);
				mem.write(((XElement)v).toString());		// TODO stream xml to buffer?
			}
			
			return;
		}
	}
    
	/* IOUtil
    public static long byteArrayToLong(byte[] b) {
        return  (b[0] & 0xff) << 56
        		| (b[1] & 0xff) << 48 
        		| (b[2] & 0xff) << 40 
        		| (b[3] & 0xff) << 32 
        		| (b[4] & 0xff) << 24 
        		| (b[5] & 0xff) << 16 
        		| (b[6] & 0xff) << 8 
        		| (b[7] & 0xff);
    }
    */

    public static byte[] longToDBNumber(long a) {
        byte[] ret = new byte[17];
        
        boolean isNeg = (a < 0); 
        
        a = Math.abs(a);
        
        ret[5] = (byte) ((a >> 56) & 0xff);
        ret[6] = (byte) ((a >> 48) & 0xff);
        ret[7] = (byte) ((a >> 40) & 0xff);
        ret[8] = (byte) ((a >> 32) & 0xff);
        ret[9] = (byte) ((a >> 24) & 0xff);
        ret[10] = (byte) ((a >> 16) & 0xff);   
        ret[11] = (byte) ((a >> 8) & 0xff);   
        ret[12] = (byte) (a & 0xff);
        
        if (isNeg) {
        	for (int i = 1; i < ret.length; i++)
        		ret[i] ^= (byte)0xff;
        }
        else {
	        // set positive flag
	        ret[0] = (byte)0x01;
        }
        
        return ret;
    }    

    public static byte[] bigToDBNumber(BigInteger left) {
    	if (left.compareTo(Constants.DB_NUMBER_MAX_I) > 0)
    		left = Constants.DB_NUMBER_MAX_I;
    	
    	if (left.compareTo(Constants.DB_NUMBER_MIN_I) < 0)
    		left = Constants.DB_NUMBER_MIN_I;
        
        boolean isNeg = (left.signum() < 0); 
        
        left = left.abs();
        
        byte[] ret = new byte[17];
        
		byte[] larray = left.toByteArray();  
        
		for (int i = 1; i < 13; i++) {
			int loff = larray.length - i;
			
			if (loff >= 0)
				ret[13 - i] = larray[loff];
		}
        
        if (isNeg) {
        	for (int i = 1; i < ret.length; i++)
        		ret[i] ^= (byte)0xff;
        }
        else {
	        // set positive flag
	        ret[0] = (byte)0x01;
        }
        
        return ret;
    }    

    public static byte[] decimalToDBNumber(BigDecimal a) {
    	if (a.compareTo(Constants.DB_NUMBER_MAX) > 0)
    		a = Constants.DB_NUMBER_MAX;
    	
    	if (a.compareTo(Constants.DB_NUMBER_MIN) < 0)
    		a = Constants.DB_NUMBER_MIN;
        
        boolean isNeg = (a.signum() < 0); 
        
        a = a.abs();
        
        BigInteger left = a.toBigInteger();  //  a.setScale(0, RoundingMode.HALF_UP);
    	BigDecimal right = a.remainder(BigDecimal.ONE).setScale(9, RoundingMode.HALF_UP); // a.subtract(left);  //.setScale(9, RoundingMode.HALF_UP);
        
        byte[] ret = new byte[17];
        
		byte[] larray = left.toByteArray();  // .unscaledValue().toByteArray();
		byte[] rarray = right.unscaledValue().toByteArray();
        
		for (int i = 1; i < 13; i++) {
			int loff = larray.length - i;
			
			if (loff >= 0)
				ret[13 - i] = larray[loff];
		}
        
		for (int i = 1; i < 5; i++) {
			int roff = rarray.length - i;
			
			if (roff >= 0)
				ret[17 - i] = rarray[roff];
		}
        
        if (isNeg) {
        	for (int i = 1; i < ret.length; i++)
        		ret[i] ^= (byte)0xff;
        }
        else {
	        // set positive flag
	        ret[0] = (byte)0x01;
        }
        
        return ret;
    }    
    
    public static String dbStringToString(Memory mem) {
    	int pos = mem.getPosition();
    	int len = 0;
    	
    	while (mem.readableBytes() > 0) {
    		if (mem.readByte() == Constants.DB_TYPE_MARKER_ALPHA)
    			break;
    		
    		len++;
    	}
    	
    	if (len == 0)
    		return null;
    	
    	mem.setPosition(pos);
    	byte[] str = new byte[len];
    	
    	mem.read(str, 0, len);
    	
    	return Utf8Decoder.decode(str).toString();
    }    
    
    public static BigDateTime dbStringToBigDateTime(Memory mem) {
    	int pos = mem.getPosition();
    	int len = 0;
    	
    	while (mem.readableBytes() > 0) {
    		if (mem.readByte() == Constants.DB_TYPE_MARKER_ALPHA)
    			break;
    		
    		len++;
    	}
    	
    	if (len == 0)
    		return null;
    	
    	mem.setPosition(pos);
    	byte[] str = new byte[len];
    	
    	mem.read(str, 0, len);
    	
    	return BigDateTime.parse(Utf8Decoder.decode(str).toString()).getResult();
    }    
    
    public static DateTime dbStringToDateTime(Memory mem) {
    	int pos = mem.getPosition();
    	int len = 0;
    	
    	while (mem.readableBytes() > 0) {
    		if (mem.readByte() == Constants.DB_TYPE_MARKER_ALPHA)
    			break;
    		
    		len++;
    	}
    	
    	if (len == 0)
    		return null;
    	
    	mem.setPosition(pos);
    	byte[] str = new byte[len];
    	
    	mem.read(str, 0, len);
    	
    	return TimeUtil.stampFmt.parseDateTime(Utf8Decoder.decode(str).toString());
    }    
    
    public static LocalDate dbStringToDate(Memory mem) {
    	int pos = mem.getPosition();
    	int len = 0;
    	
    	while (mem.readableBytes() > 0) {
    		if (mem.readByte() == Constants.DB_TYPE_MARKER_ALPHA)
    			break;
    		
    		len++;
    	}
    	
    	if (len == 0)
    		return null;
    	
    	mem.setPosition(pos);
    	byte[] str = new byte[len];
    	
    	mem.read(str, 0, len);
    	
    	return ByteUtil.localDate.parseLocalDate(Utf8Decoder.decode(str).toString());
    }    
    
    public static LocalTime dbStringToTime(Memory mem) {
    	int pos = mem.getPosition();
    	int len = 0;
    	
    	while (mem.readableBytes() > 0) {
    		if (mem.readByte() == Constants.DB_TYPE_MARKER_ALPHA)
    			break;
    		
    		len++;
    	}
    	
    	if (len == 0)
    		return null;
    	
    	mem.setPosition(pos);
    	byte[] str = new byte[len];
    	
    	mem.read(str, 0, len);
    	
    	return ByteUtil.localTime.parseLocalTime(Utf8Decoder.decode(str).toString());
    }    
    
    public static Number dbNumberToNumber(Memory mem) {
    	// TODO return Long if possible - no probably need to return Big Decimal always so user can count on a casting
    	
    	// if there is not enough bytes left then error, skip to end
    	if (mem.readableBytes() < 17) {
    		// TODO write error
    		
    		mem.setPosition(mem.getLength());
    		return null;
    	}
    	
    	boolean isNeg = (mem.readByte() == 0x00);
    	
    	byte[] left = new byte[12];
    	byte[] right = new byte[4];
    	
    	mem.read(left, 0, 12);
    	mem.read(right, 0, 4);
    	
    	BigInteger ival = new BigInteger(left);
    	
    	if (isNeg)
    		ival = ival.add(BigInteger.ONE);		
    	
    	BigInteger fval = new BigInteger(right);
    	
    	if (isNeg)
    		fval = fval.add(BigInteger.ONE);
    	
    	if (fval.compareTo(BigInteger.ZERO) == 0)
    		return ival;
    	
    	BigDecimal n = new BigDecimal(fval, 9);
    	
    	n = n.add(new BigDecimal(ival));
    	
    	return n.stripTrailingZeros();
    }
    
    public static Boolean dbBooleanToBoolean(Memory mem) {
    	// if there is not enough bytes left then error, skip to end
    	if (mem.readableBytes() < 1) {
    		// TODO write error
    		return null;
    	}
    	
    	return (mem.readByte() == 0x01);
    }
    
    public static CompositeStruct dbCompositeToComposite(Memory mem) {
    	ObjectBuilder builder = new ObjectBuilder();
    	BufferToCompositeParser headerparser = new BufferToCompositeParser(builder);
		
		try {
			headerparser.parseStruct(mem);
		} 
		catch (Exception x) {
			System.out.println("Error parsing dbComposite: " + x);
			return null;		// TODO error!!
		}
		
		// value from db must be complete
		if (!headerparser.isDone())
			return null;		// TODO error!!
		
		return builder.getRoot();
    }
}
