package divconq.db;

import java.math.BigDecimal;
import java.math.BigInteger;

public class Constants {
	final static public byte DB_TYPE_MARKER_ALPHA = (byte) 0x00;
	final static public byte DB_TYPE_MARKER_OMEGA = (byte) 0xFF;
	
	final static public byte DB_TYPE_NULL = (byte) 0x01;
	final static public byte DB_TYPE_BOOLEAN = (byte) 0x02;
	final static public byte DB_TYPE_NUMBER = (byte) 0x06;
	final static public byte DB_TYPE_DATE_TIME = (byte) 0x09;
	final static public byte DB_TYPE_BIG_DATE_TIME = (byte) 0x0C;
	final static public byte DB_TYPE_TIME = (byte) 0x0F;
	final static public byte DB_TYPE_DATE = (byte) 0x12;
	final static public byte DB_TYPE_BINARY = (byte) 0x15;
	final static public byte DB_TYPE_STRING = (byte) 0x18;
	final static public byte DB_TYPE_NUMBER_SPECIAL = (byte) 0x1B;
	final static public byte DB_TYPE_COMPOSITE = (byte) 0x1E;
	final static public byte DB_TYPE_XML = (byte) 0x1F;
	
	final static public BigDecimal DB_NUMBER_MAX = new BigDecimal("10000000000000000000000000000");
	final static public BigDecimal DB_NUMBER_MIN = new BigDecimal("-10000000000000000000000000000");
	final static public BigInteger DB_NUMBER_MAX_I = new BigInteger("10000000000000000000000000000");
	final static public BigInteger DB_NUMBER_MIN_I = new BigInteger("-10000000000000000000000000000");
	
	final static public byte[] DB_ALPHA_MARKER_ARRAY = new byte[] { 0x00 };
	final static public byte[] DB_OMEGA_MARKER_ARRAY = new byte[] { (byte)0xFF };
	final static public byte[] DB_EMPTY_ARRAY = new byte[] { 0x01 };
	
	final static public String DB_GLOBAL_RECORD_META = "dcRecordMeta";
	final static public String DB_GLOBAL_RECORD = "dcRecord";
	final static public String DB_GLOBAL_INDEX = "dcIndex1";
	final static public String DB_GLOBAL_INDEX_SUB = "dcIndex2";
	
	final static public String DB_GLOBAL_ROOT_DOMAIN = "00000_000000000000001";
	final static public String DB_GLOBAL_ROOT_USER = "00000_000000000000001";
	final static public String DB_GLOBAL_GUEST_USER = "00000_000000000000002";
}
