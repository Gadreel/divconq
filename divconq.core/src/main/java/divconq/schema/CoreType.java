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
package divconq.schema;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import divconq.lang.Memory;
import divconq.lang.OperationResult;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.ScalarStruct;
import divconq.struct.Struct;
import divconq.struct.scalar.AnyStruct;
import divconq.struct.scalar.BigIntegerStruct;
import divconq.struct.scalar.BinaryStruct;
import divconq.struct.scalar.BooleanStruct;
import divconq.struct.scalar.DateTimeStruct;
import divconq.struct.scalar.DecimalStruct;
import divconq.struct.scalar.IntegerStruct;
import divconq.struct.scalar.NullStruct;
import divconq.struct.scalar.StringStruct;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class CoreType {
	protected Schema schema = null;
	protected RootType root = null;
	protected XElement def = null;
	protected List<IDataRestriction> restrictions = new ArrayList<IDataRestriction>();
	
	public RootType getType() {
		return this.root;
	}
	
	public CoreType(Schema schema) {
		this.schema = schema;
	}

	public CoreType(RootType root) {
		this.root = root;
	}

	public RecordStruct toJsonDef() {
		RecordStruct def = new RecordStruct();
		
		def.setField("RootType", this.root.getCode());
		
		if (this.restrictions.size() > 0) {
			ListStruct rests = new ListStruct();
			
			for (IDataRestriction dr : this.restrictions) 
				rests.addItem(dr.toJsonDef());
			
			def.setField("DataRestrictions", rests);
		}
		
		return def;
	}
	
	public void compile(XElement def, OperationResult mr) {
		this.def = def;
		
		String name = def.getName();
		
		if ("StringType".equals(name)) {
			this.root = RootType.String;
			
			for (XElement dtel : def.selectAll("StringRestriction")) { 
				StringRestriction dt = new StringRestriction();
				dt.compile(dtel, mr);
				this.restrictions.add(dt);
			}
		}
		else if ("BinaryType".equals(name)) {
			this.root = RootType.Binary;
			
			for (XElement dtel : def.selectAll("BinaryRestriction")) { 
				BinaryRestriction dt = new BinaryRestriction();
				dt.compile(dtel, mr);
				this.restrictions.add(dt);
			}
		}
		else if ("NumberType".equals(name)) {
			this.root = RootType.Number;
			
			for (XElement dtel : def.selectAll("NumberRestriction")) { 
				NumberRestriction dt = new NumberRestriction();
				dt.compile(dtel, mr);
				this.restrictions.add(dt);
			}
		}
		else if ("BooleanType".equals(name)) {
			this.root = RootType.Boolean;
		}
		else if ("NullType".equals(name)) {
			this.root = RootType.Null;
		}
		else if ("AnyType".equals(name)) {
			this.root = RootType.Any;
		}
	}

	public boolean match(Object data, OperationResult mr) {
		if (this.root == null) 
			return false;
		
		if (this.root == RootType.String) 
			return Struct.objectIsCharsStrict(data);		
		
		if (this.root == RootType.Number) 
			return Struct.objectIsNumber(data);		
		
		if (this.root == RootType.Binary) 
			return Struct.objectIsBinary(data);		
		
		if (this.root == RootType.Boolean) 
			return Struct.objectIsBoolean(data);		
		
		if (this.root == RootType.Null) 
			return (data == null);
		
		if (this.root == RootType.Any) 
			return true;
		
		return false;
	}
	
	public Struct create(OperationResult mr) {
		if (this.root == null) 
			return null;
		
		if (this.root == RootType.String) 
			return new StringStruct();		
		
		if (this.root == RootType.Number) { 
			for (IDataRestriction dr : this.restrictions) {
				if (dr instanceof NumberRestriction) {
					NumberRestriction ndr = (NumberRestriction)dr;
					
					if (ndr.conform == ConformKind.Integer) 
						return new IntegerStruct();
					
					if (ndr.conform == ConformKind.BigInteger) 
						return new BigIntegerStruct();
				}
			}
			
			return new DecimalStruct();
		}
		
		if (this.root == RootType.Binary) 
			return new BinaryStruct();		
		
		if (this.root == RootType.Boolean) 
			return new BooleanStruct();		
		
		if (this.root == RootType.Null) 
			return new NullStruct(); 	// need to do this because we set type later - could be a custom type
		
		if (this.root == RootType.Any) 
			return new AnyStruct();
		
		return null;
	}
	
	// don't call this with data == null from a field if field required - required means "not null" so put the error in
	public boolean validate(Object data, OperationResult mr) {
		if (data == null)
			return false;
		
		if (this.root == null) {
			mr.errorTr(407, data);			
			return false;
		}
		
		if (this.root == RootType.String) {
			CharSequence x = Struct.objectToCharsStrict(data);		// TODO use this more
			
			if (x == null) {
				// shouldn't count as error, null is fine if not required - mr.errorTr(408, data);
				return false;
			}
			
			if (this.restrictions.size() == 0)
				return true;
			
			for (IDataRestriction dr : this.restrictions) {
				if (dr.pass(x)) 
					return true;
			}
			
			for (IDataRestriction dr : this.restrictions) 
				dr.fail(x, mr);
			
			return false;
		}
		
		if (this.root == RootType.Number) {
			Number x = Struct.objectToNumber(data);		
			
			if (x == null){
				// shouldn't count as error, null is fine if not required - mr.errorTr(409, data);
				return false;
			}
			
			if (this.restrictions.size() == 0)
				return true;
			
			for (IDataRestriction dr : this.restrictions) {
				if (dr.pass(x)) 
					return true;
			}
			
			for (IDataRestriction dr : this.restrictions) 
				dr.fail(x, mr);
			
			return false;
		}
		
		if (this.root == RootType.Binary) {
			Memory x = Struct.objectToBinary(data);		
			
			if (x == null) {
				// shouldn't count as error, null is fine if not required - mr.errorTr(410, data);
				return false;
			}
			
			if (this.restrictions.size() == 0)
				return true;
			
			for (IDataRestriction dr : this.restrictions) {
				if (dr.pass(x)) 
					return true;
			}
			
			for (IDataRestriction dr : this.restrictions) 
				dr.fail(x, mr);
			
			return false;
		}
		
		if (this.root == RootType.Boolean) {
			Boolean x = Struct.objectToBoolean(data);		
			
			if (x == null) {
				// shouldn't count as error, null is fine if not required - mr.errorTr(411, data);
				return false;
			}
			
			return true;
		}
		
		if (this.root == RootType.Null) 
			return (data == null);
		
		if (this.root == RootType.Any) 
			return true;
		
		mr.errorTr(412, data);			
		return false;
	}

	public Struct wrap(Object data, OperationResult mr) {
		if (this.root == null) 
			return null;
		
		ScalarStruct ret = null;
		
		if (this.root == RootType.String) {
			if ("DateTime".equals(this.def.getAttribute("Id")))		// TODO maybe generalize this, look at Class? (before this if/else structure)
				ret = new DateTimeStruct();
			else
				ret = new StringStruct();		
		}		
		else if (this.root == RootType.Number) { 
			for (IDataRestriction dr : this.restrictions) {
				if (dr instanceof NumberRestriction) {
					NumberRestriction ndr = (NumberRestriction)dr;
					
					if (ndr.conform == ConformKind.Integer) {
						ret = new IntegerStruct();
						break;
					}
					
					if (ndr.conform == ConformKind.BigInteger) {
						ret = new BigIntegerStruct();
						break;
					}
					
					if (ndr.conform == ConformKind.Decimal) {
						ret = new DecimalStruct();
						break;
					}
					
					if (ndr.conform == ConformKind.BigDecimal) {
						ret = new DecimalStruct();
						break;
					}
				}
			}
		}
		else if (this.root == RootType.Binary) 
			ret = new BinaryStruct();				
		else if (this.root == RootType.Boolean) 
			ret = new BooleanStruct();				
		else if (this.root == RootType.Null) 
			ret = new NullStruct(); 	// need to do this because we set type later - could be a custom type
		else if (this.root == RootType.Any) 
			ret = new AnyStruct();
		
		if (ret != null)
			ret.adaptValue(data);
		
		return ret;
	}

	interface IDataRestriction {
		void compile(XElement def, OperationResult mr);
		boolean pass(Object x);
		void fail(Object x, OperationResult mr);
		RecordStruct toJsonDef();
	}
	
	class StringRestriction implements IDataRestriction {
		String[] enums = null;
		int max = 0;
		int min = 0;
		Pattern pattern = null;
		
		@Override
		public void compile(XElement def, OperationResult mr) {
			if (def.hasAttribute("Enum")) 
				this.enums = def.getAttribute("Enum", "").split(",");
			
			if (def.hasAttribute("MaxLength"))
				this.max = (int)StringUtil.parseInt(def.getAttribute("MaxLength"), 0);
			
			if (def.hasAttribute("MinLength"))
				this.min = (int)StringUtil.parseInt(def.getAttribute("MinLength"), 0);
			
			if (def.hasAttribute("Length"))
				this.max = this.min = (int)StringUtil.parseInt(def.getAttribute("Length"), 0);
			
			if (def.hasAttribute("Pattern"))
				this.pattern = Pattern.compile("^" + def.getAttribute("Pattern") + "$");
		}

		@Override
		public boolean pass(Object x) {
			if (x instanceof CharSequence) {
				if ((this.max > 0) && (((CharSequence)x).length() > this.max))
					return false;
				
				if ((this.min > 0) && (((CharSequence)x).length() < this.min))
					return false;
				
				if (this.enums != null) {
					boolean fnd = false;
					
					for (String ev : this.enums) {
						if (ev.equals(x)) {
							fnd = true;
							break;
						}
					}
					
					if (!fnd)
						return false;
				}
				
				if ((this.pattern != null) && !this.pattern.matcher((CharSequence)x).matches())
					return false;
				
				return true;
			}
			
			return false;
		}

		@Override
		public void fail(Object x, OperationResult mr) {
			if ((this.max > 0) && (((CharSequence)x).length() > this.max))
				mr.errorTr(404, x);		
			
			if ((this.min > 0) && (((CharSequence)x).length() < this.min))
				mr.errorTr(405, x);		
			
			if (this.enums != null) {
				boolean fnd = false;
				
				for (String ev : this.enums) {
					if (ev.equals(x)) {
						fnd = true;
						break;
					}
				}
				
				if (!fnd)
					mr.errorTr(406, x);		
			}
			
			if ((this.pattern != null) && !this.pattern.matcher((CharSequence)x).matches())
				mr.errorTr(447, x);		
		}		

		@Override
		public RecordStruct toJsonDef() {
			RecordStruct def = new RecordStruct();
			
			if (this.max > 0) 
				def.setField("Max", this.max);
			
			if (this.min > 0) 
				def.setField("Min", this.min);
			
			if (this.enums != null) 
				def.setField("Enums", new ListStruct((Object[])this.enums));
			
			if (this.pattern != null) 
				def.setField("Pattern", this.pattern.pattern());
			
			return def;
		}		
	}
	
	enum ConformKind {
		Integer,
		BigInteger,
		Decimal,
		BigDecimal
	}
	
	class NumberRestriction implements IDataRestriction {
		ConformKind conform = null;
		Number min = null;
		Number max = null;
		
		@Override
		public void compile(XElement def, OperationResult mr) {
			String con = def.getAttribute("Conform");
			
			if ("Integer".equals(con))
				this.conform = ConformKind.Integer;
			else if ("BigInteger".equals(con))
				this.conform = ConformKind.BigInteger;
			else if ("Decimal".equals(con))
				this.conform = ConformKind.Decimal;
			else if ("BigDecimal".equals(con))
				this.conform = ConformKind.BigDecimal;
			
			if (def.hasAttribute("Max"))
				if (this.conform == ConformKind.Integer)
					this.max = StringUtil.parseInt(def.getAttribute("Max"), 0);
				else if (this.conform == ConformKind.BigInteger)
					this.max = new BigInteger(def.getAttribute("Max"));
				else if (this.conform == ConformKind.Decimal)
					this.max = new BigDecimal(def.getAttribute("Max"));
				else if (this.conform == ConformKind.BigDecimal)
					this.max = new BigDecimal(def.getAttribute("Max"));
			
			if (def.hasAttribute("Min"))
				if (this.conform == ConformKind.Integer)
					this.min = StringUtil.parseInt(def.getAttribute("Min"), 0);
				else if (this.conform == ConformKind.BigInteger)
					this.min = new BigInteger(def.getAttribute("Min"));
				else if (this.conform == ConformKind.Decimal)
					this.min = new BigDecimal(def.getAttribute("Min"));
				else if (this.conform == ConformKind.BigDecimal)
					this.min = new BigDecimal(def.getAttribute("Min"));
			
			// TODO support Digits, Whole and Fraction
		}

		@Override
		public boolean pass(Object x) {
			if (x instanceof Number) {
				if ((this.conform == ConformKind.BigDecimal) && !(x instanceof BigDecimal) && !(x instanceof BigInteger) && !(x instanceof Long))
					return false;
				
				if ((this.conform == ConformKind.Decimal) && !(x instanceof BigDecimal) && !(x instanceof Long))
					return false;
				
				if ((this.conform == ConformKind.BigInteger) && (!(x instanceof BigInteger) && !(x instanceof Long)))
					return false;
				
				if ((this.conform == ConformKind.Integer) && (!(x instanceof Long)))
					return false;
				
				// TODO support min/max and conform
				/*
				if ((this.max != null) && ())
					return false;
				
				if ((this.min > 0) && (((Memory)x).getLength() < this.min))
					return false;
				*/
				
				return true;
			}
			
			return false;
		}

		@Override
		public void fail(Object x, OperationResult mr) {
			if ((this.conform == ConformKind.BigDecimal) && !(x instanceof BigDecimal) && !(x instanceof BigInteger) && !(x instanceof Long))
				mr.errorTr(400, x);		
			
			if ((this.conform == ConformKind.Decimal) && !(x instanceof BigDecimal) && !(x instanceof Long))
				mr.errorTr(401, x);		
			
			if ((this.conform == ConformKind.BigInteger) && (!(x instanceof BigInteger) && !(x instanceof Long)))
				mr.errorTr(402, x);		
			
			if ((this.conform == ConformKind.Integer) && (!(x instanceof Long)))
				mr.errorTr(403, x);		
		}				

		@Override
		public RecordStruct toJsonDef() {
			RecordStruct def = new RecordStruct();
			
			if (this.max != null) 
				def.setField("Max", this.max);
			
			if (this.min != null) 
				def.setField("Min", this.min);
			
			if ((this.conform == ConformKind.Integer) || (this.conform == ConformKind.BigInteger))
				def.setField("AllowDecimal", false);
			
			return def;
		}		
	}
	
	class BinaryRestriction implements IDataRestriction {
		int max = 0;
		int min = 0;
		
		@Override
		public void compile(XElement def, OperationResult mr) {
			if (def.hasAttribute("MaxLength"))
				this.max = (int)StringUtil.parseInt(def.getAttribute("MaxLength"), 0);
			
			if (def.hasAttribute("MinLength"))
				this.min = (int)StringUtil.parseInt(def.getAttribute("MinLength"), 0);
			
			if (def.hasAttribute("Length"))
				this.max = this.min = (int)StringUtil.parseInt(def.getAttribute("Length"), 0);
		}

		@Override
		public boolean pass(Object x) {
			if (x instanceof Memory) {
				if ((this.max > 0) && (((Memory)x).getLength() > this.max))
					return false;
				
				if ((this.min > 0) && (((Memory)x).getLength() < this.min))
					return false;
				
				return true;
			}
			
			return false;
		}

		@Override
		public void fail(Object x, OperationResult mr) {
			// TODO Auto-generated method stub
			
		}				

		@Override
		public RecordStruct toJsonDef() {
			RecordStruct def = new RecordStruct();
			
			if (this.max > 0) 
				def.setField("Max", this.max);
			
			if (this.min > 0) 
				def.setField("Min", this.min);
			
			return def;
		}		
	}
}
