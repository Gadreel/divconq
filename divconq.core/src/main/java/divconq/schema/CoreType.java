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

import divconq.hub.Hub;
import divconq.lang.Memory;
import divconq.lang.op.OperationContext;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.ScalarStruct;
import divconq.struct.Struct;
import divconq.struct.scalar.NullStruct;
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
	
	public void compile(XElement def) {
		this.def = def;
		
		String name = def.getName();
		
		if ("StringType".equals(name)) {
			this.root = RootType.String;
			
			for (XElement dtel : def.selectAll("StringRestriction")) { 
				StringRestriction dt = new StringRestriction();
				dt.compile(dtel);
				this.restrictions.add(dt);
			}
		}
		else if ("BinaryType".equals(name)) {
			this.root = RootType.Binary;
			
			for (XElement dtel : def.selectAll("BinaryRestriction")) { 
				BinaryRestriction dt = new BinaryRestriction();
				dt.compile(dtel);
				this.restrictions.add(dt);
			}
		}
		else if ("NumberType".equals(name)) {
			this.root = RootType.Number;
			
			for (XElement dtel : def.selectAll("NumberRestriction")) { 
				NumberRestriction dt = new NumberRestriction();
				dt.compile(dtel);
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

	public boolean match(Object data) {
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
	
	public String getClassName() {
		if (this.def.hasAttribute("Class")) 
			return this.def.getAttribute("Class");
		
		if (this.root == RootType.String)
			return "divconq.struct.scalar.StringStruct";
		
		if (this.root == RootType.Number) 
			return "divconq.struct.scalar.DecimalStruct";
		
		if (this.root == RootType.Binary) 
			return "divconq.struct.scalar.BiaryStruct";
		
		if (this.root == RootType.Boolean)
			return "divconq.struct.scalar.BooleanStruct";
		
		if (this.root == RootType.Null) 
			return "divconq.struct.scalar.NullStruct";
		
		return "divconq.struct.scalar.AnyStruct";
	}
	
	public ScalarStruct create(DataType dt) {
		if (this.root == RootType.Null) 
			return NullStruct.instance;
		
		// TODO support groovy code too - like Class 
		
		ScalarStruct data = (ScalarStruct) Hub.instance.getInstance(this.getClassName());
		
		data.setType(dt);
		
		return data;
	}
	
	// don't call this with data == null from a field if field required - required means "not null" so put the error in
	public boolean validate(DataType dt, ScalarStruct in) {
		if (in == null)
			return false;
		
		if (this.root == null) {
			OperationContext.get().errorTr(407, in);			
			return false;
		}
		
		// make sure we are using the correct class to do the validation
		in = this.reuseOrWrap(dt, in);
		
		if (this.validateData(in.toInternalValue(this.root)))
			return in.validateData(dt);
		
		return false;
	}
	
	// don't call this with data == null from a field if field required - required means "not null" so put the error in
	public boolean validateData(Object data) {
		if (data == null)
			return false;
		
		if (this.root == RootType.String) {
			CharSequence x = Struct.objectToCharsStrict(data);		// TODO use this more
			
			if (x == null) {
				// shouldn't count as error, null is fine if not required - mr.errorTr(408, data);
				return false;
			}
			
			if (this.restrictions.size() == 0)
				return true;
			
			if (StringUtil.containsRestrictedChars(x)) {
				OperationContext.get().error("String contains restricted characters!");
				return false;
			}
			
			for (IDataRestriction dr : this.restrictions) {
				if (dr.pass(x)) 
					return true;
			}
			
			for (IDataRestriction dr : this.restrictions) 
				dr.fail(x);
			
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
				dr.fail(x);
			
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
				dr.fail(x);
			
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
		
		OperationContext.get().errorTr(412, data);			
		return false;
	}
	
	// don't call this with data == null from a field if field required - required means "not null" so put the error in
	public ScalarStruct normalizeValidate(DataType dt, ScalarStruct in) {
		if (in == null)
			return null;
		
		if (this.root == null) {
			OperationContext.get().errorTr(407, in);			
			return null;
		}
		
		// make sure we are using the correct class to do the validation
		in = this.reuseOrWrap(dt, in);
		
		if (this.validateData(in.toInternalValue(this.root)))
			return in;
		
		return null;
	}

	public ScalarStruct normalize(DataType dt, Object data) {
		if (data instanceof ScalarStruct)
			return this.reuseOrWrap(dt, (ScalarStruct) data);
		
		return this.wrap(dt, data);
	}

	public ScalarStruct reuseOrWrap(DataType dt, ScalarStruct data) {
		if (data == null)
			return null;
		
		if (this.root == RootType.Any)
			return data;
		
		String cname = this.getClassName();
		
		// TODO support groovy code too - like Class 
		
		// if we are expecting a special class, try to resolve validation via that class 
		if (data.getClass().getName().equals(cname)) 
			return data;
		
		return this.wrap(dt, data.getGenericValue());
	}

	// don't pass in scalar struct here, use normalize instead
	public ScalarStruct wrap(DataType dt, Object data) {
		if (this.root == null) 
			return null;
		
		// TODO support groovy code too - like Class 
		
		//String cname = this.getClassName();
		
		//System.out.println("Normalizing object from " + data.getClass().getName() + " to " + cname);
		
		ScalarStruct nv  = this.create(dt);
		nv.adaptValue(data);
		
		if (nv.isNull())
			nv = null;
		
		//System.out.println("New value: " + nv + " - old value: " + data);
		
		return nv;
	}

	interface IDataRestriction {
		void compile(XElement def);
		boolean pass(Object x);
		void fail(Object x);
		RecordStruct toJsonDef();
	}
	
	class StringRestriction implements IDataRestriction {
		String[] enums = null;
		int max = 0;
		int min = 0;
		Pattern pattern = null;
		
		@Override
		public void compile(XElement def) {
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
		public void fail(Object x) {
			if ((this.max > 0) && (((CharSequence)x).length() > this.max))
				OperationContext.get().errorTr(404, x);		
			
			if ((this.min > 0) && (((CharSequence)x).length() < this.min))
				OperationContext.get().errorTr(405, x);		
			
			if (this.enums != null) {
				boolean fnd = false;
				
				for (String ev : this.enums) {
					if (ev.equals(x)) {
						fnd = true;
						break;
					}
				}
				
				if (!fnd)
					OperationContext.get().errorTr(406, x);		
			}
			
			if ((this.pattern != null) && !this.pattern.matcher((CharSequence)x).matches())
				OperationContext.get().errorTr(447, x);		
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
		public void compile(XElement def) {
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
				
				if ((this.conform == ConformKind.Decimal) && !(x instanceof BigDecimal) && !(x instanceof BigInteger) && !(x instanceof Long))
					return false;
				
				if ((this.conform == ConformKind.BigInteger) && (!(x instanceof BigInteger) && !(x instanceof Long)))
					return false;
				
				if ((this.conform == ConformKind.Integer) && (!(x instanceof Long))) {
					if (x instanceof BigInteger)
						try {
							((BigInteger)x).longValueExact();
							return true;
						}
						catch (ArithmeticException x2) {
						}
					
					return false;
				}
				
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
		public void fail(Object x) {
			if ((this.conform == ConformKind.BigDecimal) && !(x instanceof BigDecimal) && !(x instanceof BigInteger) && !(x instanceof Long))
				OperationContext.get().errorTr(400, x);		
			
			if ((this.conform == ConformKind.Decimal) && !(x instanceof BigDecimal) && !(x instanceof BigInteger) && !(x instanceof Long))
				OperationContext.get().errorTr(401, x);		
			
			if ((this.conform == ConformKind.BigInteger) && (!(x instanceof BigInteger) && !(x instanceof Long)))
				OperationContext.get().errorTr(402, x);		
			
			if ((this.conform == ConformKind.Integer) && (!(x instanceof Long))) 
				OperationContext.get().errorTr(403, x);		
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
		public void compile(XElement def) {
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
		public void fail(Object x) {
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
