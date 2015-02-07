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
package divconq.struct;

import java.io.BufferedReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.SQLException;

import divconq.util.Base64;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import divconq.lang.BigDateTime;
import divconq.lang.Memory;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.schema.DataType;
import divconq.schema.IDataExposer;
import divconq.script.StackEntry;
import divconq.struct.builder.ICompositeBuilder;
import divconq.struct.scalar.AnyStruct;
import divconq.struct.scalar.BigDateTimeStruct;
import divconq.struct.scalar.BigIntegerStruct;
import divconq.struct.scalar.BinaryStruct;
import divconq.struct.scalar.BooleanStruct;
import divconq.struct.scalar.DateStruct;
import divconq.struct.scalar.DateTimeStruct;
import divconq.struct.scalar.DecimalStruct;
import divconq.struct.scalar.IntegerStruct;
import divconq.struct.scalar.NullStruct;
import divconq.struct.scalar.StringStruct;
import divconq.util.StringUtil;
import divconq.util.TimeUtil;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

abstract public class Struct {
	protected DataType explicitType = null;
	
	// override this to return implicit type if no explicit exists
	public DataType getType() {
		return this.explicitType;
	}

	public void setType(DataType v) {
		this.explicitType = v;
	}	
	
	public boolean hasExplicitType() {
		return (this.explicitType != null);
	}
	
	public Struct() {
	}
	
	public Struct(DataType type) {
		this.explicitType = type;
	}

	// just a reminder of the things to override in types
	
	@Override
	public Object clone() {
		return this.deepCopy();
	}
	
	@Override
	abstract public String toString();
	
    protected void doCopy(Struct n) {
    	n.explicitType = this.explicitType;
    }
    
	abstract public Struct deepCopy();
	
	/**
	 * @return true if contains no data or insufficient data to constitute a complete value
	 */
	abstract public boolean isEmpty();
	
	/**
	 * 
	 * @return true if it really is null (scalars only, composites are never null)
	 */
	abstract public boolean isNull();
		
	public OperationResult validate() {
		OperationResult mr = new OperationResult();
		this.validate(this.explicitType);
		return mr;
	}
	
	public OperationResult validate(String type) {
		OperationResult mr = new OperationResult();
		this.validate(OperationContext.get().getSchema().getType(type));
		return mr;
	}
	
	public OperationResult validate(DataType type) {
		OperationResult mr = new OperationResult();
		
		if (type == null)
			OperationContext.get().errorTr(522);		
		else
			type.validate(this);
		
		return mr;
	}
	
	// statics
	
	static public Long objectToInteger(Object o) {
		if (o == null)
			return null;
		
		if (o instanceof FieldStruct)
			return Struct.objectToInteger(((FieldStruct)o).getValue());
		
		if (o instanceof NullStruct)
			return null;
		
		if (o instanceof AnyStruct)
			o = ((AnyStruct)o).getValue();
		else if (o instanceof IntegerStruct)
			o = ((IntegerStruct)o).getValue();
		else if (o instanceof BigIntegerStruct) 
			o = ((BigIntegerStruct)o).getValue();
		else if (o instanceof DecimalStruct) 
			o = ((DecimalStruct)o).getValue();
		else if (o instanceof StringStruct)
			o = ((StringStruct)o).getValue();
		else if (o instanceof DateTimeStruct)
			o = ((DateTimeStruct)o).getValue();
		
		if (o == null)
			return null;
		
		if (o instanceof Long)
			return (Long)o;
		
		if (o instanceof Number)
			return ((Number)o).longValue();
		
		if (o instanceof java.sql.Timestamp)
			return ((java.sql.Timestamp)o).getTime();
		
		if (o instanceof DateTime)
			return ((DateTime)o).getMillis();
		
		if (o instanceof CharSequence) {
			try {
				return new Long(o.toString());
			}
			catch (Exception x) {
			}
		}
		
		return null;
	}
	
	static public BigInteger objectToBigInteger(Object o) {
		if (o == null)
			return null;
		
		if (o instanceof FieldStruct)
			return Struct.objectToBigInteger(((FieldStruct)o).getValue());
		
		if (o instanceof NullStruct)
			return null;
		
		if (o instanceof AnyStruct)
			o = ((AnyStruct)o).getValue();
		else if (o instanceof BigIntegerStruct)
			o = ((BigIntegerStruct)o).getValue();
		else if (o instanceof IntegerStruct)
			o = ((IntegerStruct)o).getValue();
		else if (o instanceof StringStruct)
			o = ((StringStruct)o).getValue();
		
		if (o == null)
			return null;
		
		if (o instanceof BigInteger)
			return (BigInteger)o;
		
		if (o instanceof Number)
			return new BigInteger(o.toString());
		
		if (o instanceof java.sql.Timestamp)
			return BigInteger.valueOf(((java.sql.Timestamp)o).getTime());
		
		if (o instanceof CharSequence) {
			try {
				return new BigInteger(o.toString());
			}
			catch (Exception x) {
			}
		}
		
		return null;
	}
	
	static public BigDecimal objectToDecimal(Object o) {
		if (o == null)
			return null;
		
		if (o instanceof FieldStruct)
			return Struct.objectToDecimal(((FieldStruct)o).getValue());
		
		if (o instanceof NullStruct)
			return null;
		
		if (o instanceof AnyStruct)
			o = ((AnyStruct)o).getValue();
		else if (o instanceof IntegerStruct)
			o = ((IntegerStruct)o).getValue();
		else if (o instanceof BigIntegerStruct)
			o = ((BigIntegerStruct)o).getValue();
		else if (o instanceof DecimalStruct)
			o = ((DecimalStruct)o).getValue();
		else if (o instanceof StringStruct)
			o = ((StringStruct)o).getValue();
		
		if (o == null)
			return null;
		
		if (o instanceof Number) {
			if (o instanceof BigDecimal)
				return (BigDecimal)o;
			
			return new BigDecimal(((Number)o).doubleValue());
		}
		
		if (o instanceof CharSequence) {
			try {
				return new BigDecimal(o.toString());
			}
			catch (Exception x) {
			}
		}
		
		return null;
	}

	// returns true only if can be a valid Number types - Long, BigInteger or BigDecimal
	public static boolean objectIsNumber(Object o) {
		if (o == null)
			return false;
		
		if (o instanceof AnyStruct)
			o = ((AnyStruct)o).getValue();
		
		if (o instanceof FieldStruct)
			return Struct.objectIsNumber(((FieldStruct)o).getValue());
		
		if (o instanceof DecimalStruct)
			return true;
		
		if (o instanceof IntegerStruct)
			return true;
		
		if (o instanceof BigIntegerStruct)
			return true;
		
		if (o instanceof Number) {
			if (o instanceof BigDecimal)
				return true;
			
			if (o instanceof BigInteger)
				return true;
			
			if (o instanceof Long)
				return true;
			
			if (o instanceof Integer)
				return true;
			
			if (o instanceof Short)
				return true;
			
			if (o instanceof Byte)
				return true;
			
			if (o instanceof Float)
				return true;
			
			if (o instanceof Double)
				return true;
			
			return false;
		}
		
		if (o instanceof CharSequence) {
			try {
				new BigDecimal(o.toString());
				return true;
			}
			catch (Exception x) {
			}
		}
		
		return false;
	}

	// returns only valid Number types - Long, BigInteger or BigDecimal
	public static Number objectToNumber(Object o) {
		if (o == null)
			return null;
		
		if (o instanceof FieldStruct)
			return Struct.objectToNumber(((FieldStruct)o).getValue());
		
		if (o instanceof NullStruct)
			return null;
		
		if (o instanceof AnyStruct)
			o = ((AnyStruct)o).getValue();
		else if (o instanceof IntegerStruct)
			o = ((IntegerStruct)o).getValue();
		else if (o instanceof BigIntegerStruct)
			o = ((BigIntegerStruct)o).getValue();
		else if (o instanceof DecimalStruct)
			o = ((DecimalStruct)o).getValue();
		else if (o instanceof StringStruct)
			o = ((StringStruct)o).getValue();
		
		if (o == null)
			return null;
		
		if (o instanceof Number) {
			if (o instanceof BigDecimal)
				return (Number) o;
			
			if (o instanceof BigInteger)
				return (Number) o;
			
			if (o instanceof Long)
				return (Number) o;
			
			if (o instanceof Integer)
				return ((Number) o).longValue();
			
			if (o instanceof Short)
				return ((Number) o).longValue();
			
			if (o instanceof Byte)
				return ((Number) o).longValue();
			
			if (o instanceof Float)
				return new BigDecimal(((Number) o).floatValue());
			
			if (o instanceof Double)
				return new BigDecimal(((Number) o).doubleValue());
			
			return null;
		}
		
		if (o instanceof CharSequence) {
			String num = o.toString();
			
			if (StringUtil.isNotEmpty(num)) {
				if (!num.contains(".")) {
					// try to fit in 64 bit
					try {
						return new Long(num);
					}
					catch (Exception x) {
					}
					
					// otherwise try to fit in big int
					try {
						return new BigInteger(num);
					}
					catch (Exception x) {
					}
				}
				else {
					try {
						return new BigDecimal(num);
					}
					catch (Exception x) {
					}
				}
			}
		}
		
		return null;
	}
	
	static public boolean objectIsBoolean(Object o) {
		if (o == null)
			return false;
		
		if (o instanceof AnyStruct)
			o = ((AnyStruct)o).getValue();
		
		if (o instanceof FieldStruct)
			return Struct.objectIsBoolean(((FieldStruct)o).getValue());
		
		if (o instanceof Boolean)
			return true;
		
		if (o instanceof BooleanStruct)
			return true;
		
		if (o instanceof StringStruct)
			o = ((StringStruct) o).getValue();
		
		if (o instanceof CharSequence) {
			try {
				new Boolean(o.toString());
				return true;
			}
			catch (Exception x) {
			}
		}
		
		return false;
	}
	
	static public boolean objectToBooleanOrFalse(Object o) {
		Boolean v = Struct.objectToBoolean(o);
		
		if (v == null)
			return false;
		
		return v;
	}
	
	static public Boolean objectToBoolean(Object o) {
		if (o == null)
			return null;
		
		if (o instanceof FieldStruct)
			return Struct.objectToBoolean(((FieldStruct)o).getValue());
		
		if (o instanceof NullStruct)
			return null;
		
		if (o instanceof AnyStruct)
			o = ((AnyStruct)o).getValue();
		else if (o instanceof BooleanStruct)
			o = ((BooleanStruct)o).getValue();
		else if (o instanceof StringStruct)
			o = ((StringStruct)o).getValue();
		else if (o instanceof IntegerStruct)
			o = ((IntegerStruct)o).getValue();
		
		if (o == null)
			return null;
		
		if (o instanceof Boolean)
			return (Boolean)o;
		
		if (o instanceof Number)
			return ((Number)o).intValue() != 0;
		
		if (o instanceof CharSequence) {
			try {
				return new Boolean(o.toString().toLowerCase().trim());
			}
			catch (Exception x) {
			}
		}

		return null;
	}
	
	static public DateTime objectToDateTime(Object o) {
		if (o == null)
			return null;
		
		if (o instanceof FieldStruct)
			return Struct.objectToDateTime(((FieldStruct)o).getValue());
		
		if (o instanceof NullStruct)
			return null;
		
		if (o instanceof AnyStruct)
			o = ((AnyStruct)o).getValue();
		else if (o instanceof DateTimeStruct)
			o = ((DateTimeStruct)o).getValue();
		else if (o instanceof StringStruct)
			o = ((StringStruct)o).getValue();
		
		if (o == null)
			return null;
		
		if (o instanceof DateTime)
			return (DateTime)o;
		
		if (o instanceof java.sql.Timestamp)
			return TimeUtil.convertSqlDate((java.sql.Timestamp)o);
		
		if (o instanceof CharSequence) {
			try {
				return TimeUtil.parseDateTime(o.toString());
			}
			catch (Exception x) {
			}
		}
		
		return null;
	}
	
	static public BigDateTime objectToBigDateTime(Object o) {
		if (o == null)
			return null;
		
		if (o instanceof FieldStruct)
			return Struct.objectToBigDateTime(((FieldStruct)o).getValue());
		
		if (o instanceof NullStruct)
			return null;
		
		if (o instanceof AnyStruct)
			o = ((AnyStruct)o).getValue();
		else if (o instanceof DateTimeStruct)
			o = ((DateTimeStruct)o).getValue();
		else if (o instanceof BigDateTimeStruct)
			o = ((BigDateTimeStruct)o).getValue();
		else if (o instanceof StringStruct)
			o = ((StringStruct)o).getValue();
		else if (o instanceof java.sql.Timestamp)
			o = new DateTime(((java.sql.Timestamp)o).getTime(), DateTimeZone.UTC);
		
		if (o == null)
			return null;
		
		if (o instanceof DateTime)
			return new BigDateTime((DateTime) o);
		
		if (o instanceof BigDateTime)
			return (BigDateTime)o;
		
		if (o instanceof CharSequence) 
			return TimeUtil.parseBigDateTime(o.toString());
		
		return null;
	}
	
	static public LocalDate objectToDate(Object o) {
		if (o == null)
			return null;
		
		if (o instanceof FieldStruct)
			return Struct.objectToDate(((FieldStruct)o).getValue());
		
		if (o instanceof NullStruct)
			return null;
		
		if (o instanceof AnyStruct)
			o = ((AnyStruct)o).getValue();
		else if (o instanceof DateStruct)
			o = ((DateStruct)o).getValue();
		else if (o instanceof StringStruct)
			o = ((StringStruct)o).getValue();
		
		if (o == null)
			return null;
		
		if (o instanceof LocalDate)
			return (LocalDate)o;
		
		if (o instanceof java.sql.Timestamp)
			return new LocalDate(((java.sql.Timestamp)o).getTime(), DateTimeZone.UTC);
		
		if (o instanceof CharSequence) {
			try {
				return LocalDate.parse(o.toString());
			}
			catch (Exception x) {
			}
		}

		return null;
	}
	
	static public LocalTime objectToTime(Object o) {
		if (o == null)
			return null;
		
		if (o instanceof FieldStruct)
			return Struct.objectToTime(((FieldStruct)o).getValue());
		
		if (o instanceof NullStruct)
			return null;
		
		if (o instanceof AnyStruct)
			o = ((AnyStruct)o).getValue();
		else if (o instanceof DateStruct)
			o = ((DateStruct)o).getValue();
		else if (o instanceof StringStruct)
			o = ((StringStruct)o).getValue();
		
		if (o == null)
			return null;
		
		if (o instanceof LocalTime)
			return (LocalTime)o;
		
		if (o instanceof CharSequence) {
			try {
				return LocalTime.parse(o.toString());
			}
			catch (Exception x) {
			}
		}
		
		return null;
	}
	
	static public String objectToString(Object o) {
		if (o == null)
			return null;
		
		CharSequence x = Struct.objectToCharsStrict(o);
		
		if (x != null)
			return x.toString();
		
		return o.toString();		
	}
	
	// if it is any of our common types it can become a string, but otherwise not
	static public boolean objectIsCharsStrict(Object o) {
		if (o == null)
			return false;
		
		if (o instanceof AnyStruct)
			o = ((AnyStruct)o).getValue();
		
		if (o instanceof FieldStruct)
			return Struct.objectIsCharsStrict(((FieldStruct)o).getValue());
		
		if (o instanceof NullStruct)
			o = null;
		else if (o instanceof StringStruct)
			o = ((StringStruct)o).getValue();
		else if (o instanceof DateTimeStruct)
			o = ((DateTimeStruct)o).getValue();
		else if (o instanceof DateStruct)
			o = ((DateStruct)o).getValue();
		else if (o instanceof BigDateTimeStruct)
			o = ((BigDateTimeStruct)o).getValue();
		else if (o instanceof BigIntegerStruct)
			o = ((BigIntegerStruct)o).getValue();
		else if (o instanceof DecimalStruct)
			o = ((DecimalStruct)o).getValue();
		else if (o instanceof IntegerStruct)
			o = ((IntegerStruct)o).getValue();
		else if (o instanceof BinaryStruct)
			o = ((BinaryStruct)o).getValue();
		else if (o instanceof BooleanStruct)
			o = ((BooleanStruct)o).getValue();
		else if (o instanceof IDataExposer)
			o = ((IDataExposer)o).exposeData();
		
		if (o == null)
			return false;
		
		if (o instanceof java.sql.Timestamp)
			return true;
		
		if (o instanceof java.sql.Clob) 
			return true;
		
		if (o instanceof CharSequence)		
			return true;
		
		if (o instanceof DateTime)
			return true;
		
		if (o instanceof BigDateTime)
			return true;
		
		if (o instanceof LocalDate)
			return true;
		
		if (o instanceof BigDecimal)
			return true;
		
		if (o instanceof BigInteger)
			return true;
		
		if (o instanceof Long)
			return true;
		
		if (o instanceof Integer)
			return true;
		
		if (o instanceof Boolean)
			return true;
		
		if (o instanceof Memory)		
			return true;
		
		return false;
	}
	
	static public CharSequence objectToCharsStrict(Object o) {
		if (o == null)
			return null;
		
		if (o instanceof AnyStruct)
			o = ((AnyStruct)o).getValue();
		
		if (o instanceof FieldStruct)
			return Struct.objectToCharsStrict(((FieldStruct)o).getValue());
		
		if (o instanceof NullStruct)
			o = null;
		else if (o instanceof StringStruct)
			o = ((StringStruct)o).getValue();
		else if (o instanceof DateTimeStruct)
			o = ((DateTimeStruct)o).getValue();
		else if (o instanceof DateStruct)
			o = ((DateStruct)o).getValue();
		else if (o instanceof BigDateTimeStruct)
			o = ((BigDateTimeStruct)o).getValue();
		else if (o instanceof BigIntegerStruct)
			o = ((BigIntegerStruct)o).getValue();
		else if (o instanceof DecimalStruct)
			o = ((DecimalStruct)o).getValue();
		else if (o instanceof IntegerStruct)
			o = ((IntegerStruct)o).getValue();
		else if (o instanceof BinaryStruct)
			o = ((BinaryStruct)o).getValue();
		else if (o instanceof BooleanStruct)
			o = ((BooleanStruct)o).getValue();
		else if (o instanceof IDataExposer)
			o = ((IDataExposer)o).exposeData();
		
		if (o == null)
			return null;
		
		if (o instanceof java.sql.Timestamp)
			o = TimeUtil.convertSqlDate((java.sql.Timestamp)o);
		
		if (o instanceof java.sql.Clob) {
			try {
				BufferedReader reader = new BufferedReader(((java.sql.Clob)o).getCharacterStream());
				
				StringBuilder builder = new StringBuilder();
				String aux = "";

				while ((aux = reader.readLine()) != null) {
				    builder.append(aux);
				}

				o = builder.toString();			} 
			catch (Exception x) {
				return null;
			}
		}
		
		if (o instanceof CharSequence)		
			return (CharSequence)o;
		
		if (o instanceof DateTime)
			return TimeUtil.stampFmt.print((DateTime)o);
		
		if (o instanceof BigDateTime)
			return ((BigDateTime)o).toString();
		
		if (o instanceof LocalDate)
			return ((LocalDate)o).toString();
		
		if (o instanceof BigDecimal)
			return ((BigDecimal)o).toString();
		
		if (o instanceof BigInteger)
			return ((BigInteger)o).toString();
		
		if (o instanceof Long)
			return ((Long)o).toString();
		
		if (o instanceof Integer)
			return ((Integer)o).toString();
		
		if (o instanceof Boolean)
			return ((Boolean)o).toString();
		
		if (o instanceof Memory)		
			return o.toString();
		
		return null;
	}
	
	static public RecordStruct objectToRecord(Object o) {
		CompositeStruct cs = Struct.objectToComposite(o);
		
		if (cs instanceof RecordStruct)
			return (RecordStruct)cs;
		
		return null;
	}
	
	static public ListStruct objectToList(Object o) {
		CompositeStruct cs = Struct.objectToComposite(o);
		
		if (cs instanceof ListStruct)
			return (ListStruct)cs;
		
		return null;
	}
	
	static public CompositeStruct objectToComposite(Object o) {
		if (o == null)
			return null;
		
		if (o instanceof FieldStruct)
			return Struct.objectToComposite(((FieldStruct)o).getValue());
		
		if (o instanceof NullStruct)
			return null;
		
		if (o instanceof AnyStruct)
			o = ((AnyStruct)o).getValue();
		
		if (o == null)
			return null;
		
		if (o instanceof CompositeStruct)
			return (CompositeStruct)o;
		
		if (o instanceof ICompositeBuilder)
			return ((ICompositeBuilder)o).toLocal();
		
		if (o instanceof Memory) {
			((Memory)o).setPosition(0);			
			return CompositeParser.parseJson((Memory)o).getResult();
		}
		
		if (o instanceof StringStruct)
			return CompositeParser.parseJson(((StringStruct)o).getValue()).getResult();
		
		if (o instanceof CharSequence)
			return CompositeParser.parseJson(o.toString()).getResult();
		
		// TODO add some other obvious types - List, Array, Map?, etc
		return null;		
	}
	
	static public XElement objectToXml(Object o) {
		if (o == null)
			return null;
		
		if (o instanceof FieldStruct)
			return Struct.objectToXml(((FieldStruct)o).getValue());
		
		if (o instanceof NullStruct)
			return null;
		
		if (o instanceof AnyStruct)
			o = ((AnyStruct)o).getValue();
		else if (o instanceof StringStruct)
			o = ((StringStruct)o).getValue();
		
		if (o == null)
			return null;
		
		if (o instanceof XElement)
			return (XElement)o;
		
		CharSequence xml = null;
		
		if (o instanceof CharSequence)
			xml = (CharSequence)o;
		
		if (xml == null)
			return null;
		
		FuncResult<XElement> xres3 = XmlReader.parse(xml, false);
		
		// TODO add some other obvious types - List, Array, Map?, etc
		return xres3.getResult();		
	}

	public static boolean objectIsBinary(Object o) {
		if (o == null)
			return false;
		
		if (o instanceof AnyStruct)
			o = ((AnyStruct)o).getValue();
		
		if (o == null)
			return false;
		
		if (o instanceof FieldStruct)
			return Struct.objectIsBinary(((FieldStruct)o).getValue());
		
		if (o instanceof Memory)
			return true;
		
		if (o instanceof BinaryStruct)
			return true;
		
		if (o instanceof byte[])
			return true;
		
		if (o instanceof ByteBuffer)
			return true;
		
		return false;
	}

	public static Memory objectToBinary(Object o) {
		if (o == null)
			return null;
		
		if (o instanceof FieldStruct)
			return Struct.objectToBinary(((FieldStruct)o).getValue());
		
		if (o instanceof NullStruct)
			return null;
		
		if (o instanceof AnyStruct)
			o = ((AnyStruct)o).getValue();
		else if (o instanceof BinaryStruct)
			o = ((BinaryStruct)o).getValue();
		else if (o instanceof StringStruct)
			o = ((StringStruct)o).getValue();
		
		if (o == null)
			return null;
		
		if (o instanceof Memory)
			return (Memory)o;
		
		if (o instanceof byte[])
			return new Memory((byte[])o);
		
		if (o instanceof CharSequence) 
			return new Memory(Base64.decodeFast((CharSequence)o));
		
		if (o instanceof ByteBuffer)
			return new Memory(((ByteBuffer)o).array());
		
		return null;
	}
	
	static public Struct objectToStruct(Object o) {
		if (o == null)
			return null;
		
		if (o instanceof Struct)
			return (Struct)o;
		
		if (o instanceof FieldStruct)
			return Struct.objectToStruct(((FieldStruct)o).getValue());

		Struct svalue = null;
		
		if (o instanceof java.sql.Timestamp) {
			String t = ((java.sql.Timestamp)o).toString();
			
			// going to be returned in the local server's timezone, need to make that into UTC			  
			DateTime dt1 = TimeUtil.sqlStampFmt.parseDateTime(t);
			
			o = dt1;
		}
		
		if (o instanceof java.sql.Clob) {
			try {
				o = ((java.sql.Clob)o).getSubString(1L, (int)((java.sql.Clob)o).length());
			} 
			catch (SQLException x) {
				return null;
			}
		}
		
		if (o instanceof DateTime) {
			svalue = new DateTimeStruct();
			((DateTimeStruct)svalue).adaptValue(o);
			return svalue;
		}
		
		if (o instanceof BigDateTime) {
			svalue = new BigDateTimeStruct();
			((BigDateTimeStruct)svalue).adaptValue(o);
			return svalue;
		}
		
		if ((o instanceof BigDecimal) || (o instanceof Double) || (o instanceof Float)) {
			svalue = new DecimalStruct();
			((DecimalStruct)svalue).adaptValue(o);
			return svalue;
		}
		
		if (o instanceof BigInteger) {
			svalue = new BigIntegerStruct();
			((BigIntegerStruct)svalue).adaptValue(o);
			return svalue;
		}
		
		if (o instanceof Number) {
			svalue = new IntegerStruct();
			((IntegerStruct)svalue).adaptValue(o);
			return svalue;
		}
		
		if (o instanceof Boolean) {
			svalue = new BooleanStruct();
			((BooleanStruct)svalue).adaptValue(o);
			return svalue;
		}
		
		if (o instanceof CharSequence) {
			svalue = new StringStruct();
			((StringStruct)svalue).adaptValue(o);
			return svalue;
		}
		
		if (o instanceof Memory) {
			svalue = new BinaryStruct();
			((BinaryStruct)svalue).adaptValue(o);
			return svalue;
		}
		
		if (o instanceof XElement) {
			svalue = new StringStruct();
			((StringStruct)svalue).adaptValue(o);
			return svalue;
		}
		
		// TODO
		//if (o instanceof CharSequence)
		//	return CompositeParser.parseJson(o.toString());
		//
		//if (o instanceof StringStruct)
		//	return CompositeParser.parseJson(((StringStruct)o).getValue());
		
		// TODO add some other obvious types - List, Array, Map?, etc 
		// bytebuffer, bytearray, memory...
		
		svalue = new AnyStruct();
		((AnyStruct)svalue).adaptValue(o);
		return svalue;
	}
	
	// return the most appropriate core type for this object
	// core types are: String, DateTime, BigDateTime, BigDecimal, BigInteger, Long (aka Integer), Boolean, Memory
	// XElement and CompositeStructs too!
	static public Object objectToCore(Object o) {
		if (o == null)
			return null;
		
		if (o instanceof CompositeStruct)
			return o;
		
		if (o instanceof FieldStruct)
			return Struct.objectToCore(((FieldStruct)o).getValue());
		
		if (o instanceof ScalarStruct)
			return Struct.objectToCore(((ScalarStruct)o).getGenericValue());
		
		if (o instanceof java.sql.Timestamp) {
			String t = ((java.sql.Timestamp)o).toString();
			
			// going to be returned in the local server's timezone, need to make that into UTC			  
			return TimeUtil.sqlStampFmt.parseDateTime(t);
		}
		
		if (o instanceof java.sql.Clob) {
			try {
				return ((java.sql.Clob)o).getSubString(1L, (int)((java.sql.Clob)o).length());
			} 
			catch (SQLException x) {
				return null;
			}
		}
		
		if (o instanceof DateTime) 
			return o;
		
		if (o instanceof LocalDate) 
			return o;
		
		if (o instanceof LocalTime) 
			return o;
		
		if (o instanceof BigDateTime) 
			return o;
		
		if (o instanceof BigDecimal) 
			return o;
		
		if ((o instanceof Double) || (o instanceof Float)) 
			return Struct.objectToDecimal(o);
		
		if (o instanceof BigInteger) 
			return o;
		
		if (o instanceof Number) 
			return Struct.objectToInteger(o);
		
		if (o instanceof Boolean) 
			return o;
		
		if (o instanceof CharSequence) 
			return o.toString();
		
		if (o instanceof Memory) 
			return o;
		
		if (o instanceof XElement) 
			return o;
		
		// TODO add some other obvious types - List, Array, Map?, etc 
		// bytebuffer, bytearray, memory...
		
		// could not convert
		return null;
	}
	
	// if it is any of our common types it can become a string, but otherwise not
	static public boolean objectIsEmpty(Object o) {
		if (o == null)
			return true;
		
		if (o instanceof AnyStruct)
			o = ((AnyStruct)o).getValue();
		
		if (o instanceof FieldStruct)
			return Struct.objectIsEmpty(((FieldStruct)o).getValue());
		
		if (o == null)
			return true;
		
		if (o instanceof Struct)
			return ((Struct)o).isEmpty();
		
		if (o instanceof java.sql.Clob)
			try {
				return ((java.sql.Clob)o).length() == 0;
			} 
			catch (SQLException x) {
				return true;
			}
		
		if (o instanceof CharSequence)		
			return StringUtil.isEmpty((CharSequence)o);
		
		if (o instanceof Memory)		
			return ((Memory)o).getLength() == 0;

		// no one else has a special Empty condition
		return false;
	}

	public void operation(StackEntry stack, XElement code) {
		if ("Validate".equals(code.getName())) 
			this.validate();
		else 
			OperationContext.get().error("operation failed, op name not recoginized: " + code.getName());

		stack.resume();
	}
}
