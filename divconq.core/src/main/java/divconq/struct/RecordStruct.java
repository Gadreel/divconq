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

import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import divconq.hub.Hub;
import divconq.lang.BigDateTime;
import divconq.lang.FuncResult;
import divconq.lang.Memory;
import divconq.lang.OperationResult;
import divconq.schema.DataType;
import divconq.schema.Field;
import divconq.script.ExecuteState;
import divconq.script.StackEntry;
import divconq.struct.builder.BuilderStateException;
import divconq.struct.builder.ICompositeBuilder;
import divconq.struct.scalar.BooleanStruct;
import divconq.struct.scalar.NullStruct;
import divconq.struct.scalar.StringStruct;
import divconq.util.ClassicIterableAdapter;
import divconq.util.IAsyncIterable;
import divconq.util.StringUtil;
import divconq.xml.XElement;

/**
 * DivConq uses a specialized type system that provides type consistency across services 
 * (including web services), database fields and stored procedures, as well as scripting.
 * 
 * All scalars (including primitives) and composites (collections) are wrapped by some
 * subclass of Struct.  Map collections are expressed by this class - records have fields
 * and fields are a name value pair.  This class is analogous to an Object in JSON but may
 * contain type information as well, similar to Yaml.
 * 
 *  TODO link to blog entries.
 * 
 * @author Andy
 *
 */
public class RecordStruct extends CompositeStruct implements IItemCollection, GroovyObject /*, JSObject */ {
	// this defines valid field name pattern (same as json)
	static protected final Pattern FIELD_NAME_PATTERN =
			Pattern.compile("(^[a-zA-Z][a-zA-Z0-9\\$_\\-]*$)|(^[\\$_][a-zA-Z][a-zA-Z0-9\\$_\\-]*$)");

	// TODO check field names inside of "set field" etc.
	static public boolean validateFieldName(String v) {
		if (StringUtil.isEmpty(v))
			return false;

		return RecordStruct.FIELD_NAME_PATTERN.matcher(v).matches();
	}	
	
	protected Map<String,FieldStruct> fields = new HashMap<String,FieldStruct>();

	@Override
	public DataType getType() {
		if (this.explicitType != null)
			return super.getType();

		// implied only, not explicit
		return Hub.instance.getSchema().getType("AnyRecord");
	}

	/**
	 * Provide data type info (schema for fields) and a list of initial fields
	 * 
	 * @param type field schema
	 * @param fields initial pairs
	 */
	public RecordStruct(DataType type, FieldStruct... fields) {
		super(type);
		this.setField(fields);
	}
	
	/**
	 * Optionally provide a list of initial fields
	 * 
	 * @param fields initial pairs
	 */
	public RecordStruct(FieldStruct... fields) {
		this.setField(fields);
	}
	
	/* (non-Javadoc)
	 * @see divconq.struct.CompositeStruct#select(divconq.struct.PathPart[])
	 */
	@Override
	public Struct select(PathPart... path) {
		if (path.length == 0)
			return this;
		
		PathPart part = path[0];
		
		if (!part.isField()) {			
			OperationResult log = part.getLog();
			
			if (log != null) 
				log.warnTr(504, this);
			
			return NullStruct.instance;
		}
		
		String fld = part.getField();
		
		if (!this.fields.containsKey(fld)) {
			//OperationResult log = part.getLog();
			
			//if (log != null) 
			//	log.warnTr(505, fld);
			
			return NullStruct.instance;
		}
		
		Struct o = this.getField(fld);

		if (path.length == 1) 
			return (o != null) ? o : NullStruct.instance;
		
		if (o instanceof CompositeStruct) 
			return ((CompositeStruct)o).select(Arrays.copyOfRange(path, 1, path.length));		
		
		OperationResult log = part.getLog();
		
		if (log != null) 
			log.warnTr(503, o);
		
		return NullStruct.instance;
	}
	
	/* (non-Javadoc)
	 * @see divconq.struct.CompositeStruct#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return (this.fields.size() == 0);
	}
	
	/* (non-Javadoc)
	 * @see divconq.struct.builder.ICompositeOutput#toBuilder(divconq.struct.builder.ICompositeBuilder)
	 */
	@Override
	public void toBuilder(ICompositeBuilder builder) throws BuilderStateException {
		builder.startRecord();
		
		for (FieldStruct f : this.fields.values()) 
			f.toBuilder(builder);
		
		builder.endRecord();
	}

	/**
	 * Adds or replaces a list of fields within the record.
	 * 
	 * @param fields
	 * @return a log of messages about success of the call
	 */
	public OperationResult setField(FieldStruct... fields) {
		OperationResult or = new OperationResult();
		
		for (FieldStruct f : fields) {
			Struct svalue = f.getValue();
			
			if (!f.prepped) {
				// take the original value and convert to a struct, fields hold structures
				Object value = f.orgvalue;
				
				if (value instanceof ICompositeBuilder)
					value = ((ICompositeBuilder)value).toLocal();
				
				if (this.explicitType != null) {
					Field fld = this.explicitType.getField(f.getName());
					
					if (fld != null) {
						Struct sv = fld.wrap(value, or);
						
						if (sv != null)
							svalue = sv;
					}
				}
				
				if (svalue == null) 
					svalue = Struct.objectToStruct(value); 
				
				f.setValue(svalue);
				f.prepped = true;
			}
			
			//FieldStruct old = this.fields.get(f.getName());
			
			//if (old != null)
			//	old.dispose();
			
			this.fields.put(f.getName(), f);
		}
		
		return or;
	}
	
	/**
	 * Add or replace a specific field with a value.
	 * 
	 * @param name of field
	 * @param value to store with field
	 * @return a log of messages about success of the call
	 */
	public OperationResult setField(String name, Object value) {
		return this.setField(new FieldStruct(name, value));
	}
	
	/**
	 * 
	 * @return collection of all the fields this record holds
	 */
	public Iterable<FieldStruct> getFields() {
		return this.fields.values();
	}
	
	/**
	 * 
	 * @param name of the field desired
	 * @return the struct for that field
	 */
	public Struct getField(String name) {
		if (!this.fields.containsKey(name)) 
			return null;
		
		FieldStruct fs = this.fields.get(name);
		
		if (fs == null)
			return null;
		
		return fs.value;
	}
	
	/**
	 * 
	 * @param name of the field desired
	 * @return the struct for that field
	 */
	public FieldStruct getFieldStruct(String name) {
		if (!this.fields.containsKey(name)) 
			return null;
		
		return this.fields.get(name);
	}
	
	/**
	 * 
	 * @param name of the field desired
	 * @return the struct for that field
	 */
	public void renameField(String from, String to) {
		FieldStruct f = this.fields.remove(from);
		
		if (f != null) {
			f.setName(to);
			this.fields.put(to, f);
		}
	}
	
	/**
	 * Unlike getField, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param name of the field desired
	 * @return field's "inner" value as an Object
	 */
	public Object getFieldAsAny(String name) {
		Struct st = this.getField(name);
		
		if (st == null)
			return null;
		
		if (st instanceof ScalarStruct) 
			return ((ScalarStruct)st).getGenericValue();
		
		if (st instanceof CompositeStruct) 
			return ((CompositeStruct)st).toString();
		
		return null;
	}
	
	/**
	 * If the record has schema, lookup the schema for a given field.
	 * 
	 * @param name of the field desired
	 * @return field's schema
	 */
	public DataType getFieldType(String name) {
		// look first at the field value, if it has schema return
		Struct fs = this.getField(name);
		
		if ((fs != null) && (fs.hasExplicitType()))
				return fs.getType();
		
		// look next at this records schema
		if (this.explicitType != null) {
			Field fld = this.explicitType.getField(name);
			
			if (fld != null)
				return fld.getPrimaryType();
		}
		
		// give up, we don't know the schema
		return null;
	}
	
	/**
	 * Like getField, except if the field does not exist it will be created and added
	 * to the Record (unless that field name violates the schema).
	 * 
	 * @param name of the field desired
	 * @return log of messages from call plus the requested structure
	 */
	public FuncResult<Struct> getOrAllocateField(String name) {
		FuncResult<Struct> fr = new FuncResult<Struct>();
		
		if (!this.fields.containsKey(name)) {
			Struct value = null;
			
			if (this.explicitType != null) {
				Field fld = this.explicitType.getField(name);
				
				if (fld != null) 
					value = fld.create(fr);
				else if (this.explicitType.isAnyRecord()) 
					value = NullStruct.instance;
			}
			else
				value = NullStruct.instance;
			
			if (value != null) {
				FieldStruct f = new FieldStruct(name, value);
				f.value = value;
				this.fields.put(name, f);
				
				fr.setResult(value);
			}
		}
		else {
			Struct value = this.getField(name);
			
			if (value == null)
				value = NullStruct.instance;
			
			fr.setResult(value);
		}
		
		return fr;
	}
	
	/**
	 * 
	 * @param name of field
	 * @return true if field exists 
	 */
	public boolean hasField(String name) {
		return this.fields.containsKey(name);
	}
	
	/**
	 * 
	 * @param name of field
	 * @return true if field does not exist or if field is string and its value is empty 
	 */
	public boolean isFieldEmpty(String name) {
		Struct f = this.getField(name);
		
		if (f == null) 
			return true;
		
		return f.isEmpty();
	}
	
	/**
	 * Unlike getField, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param name of the field desired
	 * @return field's "inner" value as Integer (DivConq thinks of integers as 64bit)
	 */
	public Long getFieldAsInteger(String name) {
		return Struct.objectToInteger(this.getField(name));
	}
	
	public long getFieldAsInteger(String name, long defaultval) {
		Long x = Struct.objectToInteger(this.getField(name));
		
		if (x == null)
			return defaultval;
		
		return x;
	}
	
	/**
	 * Unlike getField, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param name of the field desired
	 * @return field's "inner" value as BigInteger 
	 */
	public BigInteger getFieldAsBigInteger(String name) {
		return Struct.objectToBigInteger(this.getField(name));
	}
	
	public BigInteger getFieldAsBigInteger(String name, BigInteger defaultval) {
		BigInteger x = Struct.objectToBigInteger(this.getField(name));
		
		if (x == null)
			return defaultval;
		
		return x;
	}
	
	/**
	 * Unlike getField, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param name of the field desired
	 * @return field's "inner" value as BigDecimal
	 */
	public BigDecimal getFieldAsDecimal(String name) {
		return Struct.objectToDecimal(this.getField(name));
	}
	
	public BigDecimal getFieldAsDecimal(String name, BigDecimal defaultval) {
		BigDecimal x = Struct.objectToDecimal(this.getField(name));
		
		if (x == null)
			return defaultval;
		
		return x;
	}
	
	/**
	 * Unlike getField, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param name of the field desired
	 * @return field's "inner" value as Boolean
	 */
	public Boolean getFieldAsBoolean(String name) {
		return Struct.objectToBoolean(this.getField(name));
	}

	public boolean getFieldAsBooleanOrFalse(String name) {
		Boolean b = Struct.objectToBoolean(this.getField(name));
		
		return (b == null) ? false : b.booleanValue();
	}
	
	/**
	 * Unlike getField, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param name of the field desired
	 * @return field's "inner" value as DateTime
	 */
	public DateTime getFieldAsDateTime(String name) {
		return Struct.objectToDateTime(this.getField(name));
	}
	
	/**
	 * Unlike getField, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param name of the field desired
	 * @return field's "inner" value as BigDateTime
	 */
	public BigDateTime getFieldAsBigDateTime(String name) {
		return Struct.objectToBigDateTime(this.getField(name));
	}
	
	/**
	 * Unlike getField, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param name of the field desired
	 * @return field's "inner" value as Date
	 */
	public LocalDate getFieldAsDate(String name) {
		return Struct.objectToDate(this.getField(name));
	}
	
	/**
	 * Unlike getField, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param name of the field desired
	 * @return field's "inner" value as Time
	 */
	public LocalTime getFieldAsTime(String name) {
		return Struct.objectToTime(this.getField(name));
	}
	
	/**
	 * Unlike getField, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param name of the field desired
	 * @return field's "inner" value as a String
	 */
	public String getFieldAsString(String name) {
		return Struct.objectToString(this.getField(name));
	}
	
	/**
	 * Unlike getField, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param name of the field desired
	 * @return field's "inner" value as Memory 
	 */
	public Memory getFieldAsBinary(String name) {
		return Struct.objectToBinary(this.getField(name));
	}
	
	/**
	 * Unlike getField, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param name of the field desired
	 * @return field's "inner" value as a Record
	 */
	public RecordStruct getFieldAsRecord(String name) {
		return Struct.objectToRecord(this.getField(name));
	}
	
	/**
	 * Unlike getField, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param name of the field desired
	 * @return field's "inner" value as a List
	 */
	public ListStruct getFieldAsList(String name) {
		return Struct.objectToList(this.getField(name));
	}
	
	/**
	 * Unlike getField, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param name of the field desired
	 * @return field's "inner" value as CompositeStruct
	 */
	public CompositeStruct getFieldAsComposite(String name) {
		return Struct.objectToComposite(this.getField(name));
	}
	
	/**
	 * Unlike getField, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param name of the field desired
	 * @return field's "inner" value as Struct
	 */
	public Struct getFieldAsStruct(String name) {
		return Struct.objectToStruct(this.getField(name));
	}
	
    public <T extends Object> T getFieldAsStruct(String name, Class<T> type) {
    	Struct s = this.getField(name);
    	
          if (type.isAssignableFrom(s.getClass()))
                return type.cast(s);
         
          return null;
    }
    
	/**
	 * Unlike getField, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param name of the field desired
	 * @return field's "inner" value as Xml (will parse if value is string)
	 */
	public XElement getFieldAsXml(String name) {
		return Struct.objectToXml(this.getField(name));
	}
	
	/**
	 * 
	 * @return number of fields held by this record
	 */
	public int getFieldCount() {
		return this.fields.size();
	}
	
	/*
	public String checkRequiredFields(String... fields) {
		for (String fld : fields) {
			if (this.isFieldBlank(fld))
				return fld;
		}
		
		return null;
	}
	
	public String checkRequiredIfPresentFields(String... fields) {
		for (String fld : fields) {
			if (this.hasField(fld) && this.isFieldBlank(fld))
				return fld;
		}
		
		return null;
	}
	
	public String checkFieldRange(String... fields) {
		for (FieldStruct fld : this.getFields()) {
			boolean fnd = false;
			
			for (String fname : fields) {
				if (fld.getName().equals(fname)) {
					fnd = true;
					break;
				}
			}
			
			if (!fnd)
				return fld.getName();
		}
		
		return null;
	}
	*/

	/**
	 * 
	 * @param name of field to remove
	 */
	public void removeField(String name) {
		this.fields.remove(name);
	}

	public Struct sliceField(String name) {
		FieldStruct fld = this.fields.get(name);
		
		this.fields.remove(name);
		
		return fld.sliceValue();
	}

    @Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	RecordStruct nn = (RecordStruct)n;
    	
    	for (FieldStruct fld : this.fields.values())
    		nn.setField(fld.deepCopy());
    }
    
	@Override
	public Struct deepCopy() {
		RecordStruct cp = new RecordStruct();
		this.doCopy(cp);
		return cp;
	}
	
	public RecordStruct deepCopyExclude(String... exclude) {
		RecordStruct cp = new RecordStruct();
    	super.doCopy(cp);
    	
    	for (FieldStruct fld : this.fields.values()) {
			boolean fnd = false;
			
			for (String x : exclude)
				if (fld.getName().equals(x)) {
					fnd = true;
					break;
				}
			
			if (!fnd)
				cp.setField(fld.deepCopy());				
		}    	
		
		return cp;
	}

	/**
	 * Remove all child fields.
	 */
	@Override
	public void clear() {		
		this.fields.clear();
	}
	
	@Override
	public void operation(StackEntry stack, XElement code) {
		if ("Set".equals(code.getName())) {
			this.clear();
			
			String json = stack.resolveValueToString(code.getText());
			
			if (StringUtil.isNotEmpty(json)) {
				RecordStruct pjson = (RecordStruct) CompositeParser.parseJson(" { " + json + " } ").getResult();

				this.copyFields(pjson);
			}
			
			stack.resume();
			return;
		}

		if ("SetField".equals(code.getName())) {
            String def = stack.stringFromElement(code, "Type");
            String name = stack.stringFromElement(code, "Name");
			
			if (StringUtil.isEmpty(name)) {
				// TODO log
				stack.resume();
				return;
			}
            
            Struct var = null;
            
            if (StringUtil.isNotEmpty(def))
            	var = stack.getActivity().createStruct(def);

			if (code.hasAttribute("Value")) {
		        Struct var3 = stack.refFromElement(code, "Value");
		        
				if (var == null) 
	            	var = stack.getActivity().createStruct(var3.getType().getId());					
				
				if (var instanceof ScalarStruct) 
					((ScalarStruct) var).adaptValue(var3);
				else
					var = var3;
			}
            
			if (var == null) {
				stack.setState(ExecuteState.Done);
				stack.log().errorTr(520);
				stack.resume();
				return;
			}
			
			this.setField(name, var);
			stack.resume();
			return;
		}

		if ("RemoveField".equals(code.getName())) {
			String name = stack.stringFromElement(code, "Name");
			
			if (StringUtil.isEmpty(name)) {
				// TODO log
				stack.resume();
				return;
			}
			
			this.removeField(name);
			
			stack.resume();
			return;
		}

		if ("NewList".equals(code.getName())) {
			String name = stack.stringFromElement(code, "Name");
			
			if (StringUtil.isEmpty(name)) {
				// TODO log
				stack.resume();
				return;
			}
			
			this.removeField(name);
			
			this.setField(name, new ListStruct());
			
			stack.resume();
			return;
		}

		if ("NewRecord".equals(code.getName())) {
			String name = stack.stringFromElement(code, "Name");
			
			if (StringUtil.isEmpty(name)) {
				// TODO log
				stack.resume();
				return;
			}
			
			this.removeField(name);
			
			this.setField(name, new RecordStruct());
			
			stack.resume();
			return;
		}

		if ("HasField".equals(code.getName())) {
			String name = stack.stringFromElement(code, "Name");
			
			if (StringUtil.isEmpty(name)) {
				// TODO log
				stack.resume();
				return;
			}
			
	        String handle = stack.stringFromElement(code, "Handle");
			if (handle != null) 
	            stack.addVariable(handle, new BooleanStruct(this.hasField(name)));
			
			stack.resume();
			return;
		}

		if ("IsFieldEmpty".equals(code.getName())) {
			String name = stack.stringFromElement(code, "Name");
			
			if (StringUtil.isEmpty(name)) {
				// TODO log
				stack.resume();
				return;
			}
			
	        String handle = stack.stringFromElement(code, "Handle");
			if (handle != null) 
	            stack.addVariable(handle, new BooleanStruct(this.isFieldEmpty(name)));
			
			stack.resume();
			return;
		}
		
		super.operation(stack, code);
	}

	public void copyFields(RecordStruct src, String... except) {
		if (src != null)
			for (FieldStruct fld : src.getFields()) {
				boolean fnd = false;
				
				for (String x : except)
					if (fld.getName().equals(x)) {
						fnd = true;
						break;
					}
				
				if (!fnd)
					this.setField(fld);				
			}
	}

	@Override
	public Iterable<Struct> getItems() {
		List<String> tkeys = new ArrayList<String>();
		
		for (String key : this.fields.keySet())
			tkeys.add(key);

		Collections.sort(tkeys);
		
		List<Struct> keys = new ArrayList<Struct>();
		
		for (String key : tkeys)
			keys.add(new StringStruct(key));
		
		return keys;
	}
	
	@Override
	public IAsyncIterable<Struct> getItemsAsync() {
		return new ClassicIterableAdapter<Struct>(this.getItems());
	}
	
	@Override
	public boolean equals(Object obj) {
		// TODO go deep
		if (obj instanceof RecordStruct) {
			RecordStruct data = (RecordStruct) obj;
			
			for (FieldStruct fld : this.fields.values()) {
				if (!data.hasField(fld.name))
					return false;
				
				Struct ds = data.getField(fld.name);
				Struct ts = fld.value;
				
				if ((ds == null) && (ts == null))
					continue;
				
				if ((ds == null) && (ts != null))
					return false;
				
				if ((ds != null) && (ts == null))
					return false;
				
				if (!ts.equals(ds))
					return false;
			}
			
			// don't need to check match the other way around, we already know matching fields have good values  
			for (FieldStruct fld : data.fields.values()) {
				if (!this.hasField(fld.name))
					return false;
			}
			
			return true;
		}
		
		return super.equals(obj);
	}
	
	@Override
    public Object getProperty(String name) { 
		Struct v = this.getField(name);
		
		if (v == null)
			return null;
		
		if (v instanceof CompositeStruct)
			return v;
		
		return ((ScalarStruct) v).getGenericValue();
    }
    
	@Override
    public void setProperty(String name, Object value) { 
		this.setField(name, value);
    }

	// TODO generate only on request
	private transient MetaClass metaClass = null;

	@Override
	public void setMetaClass(MetaClass v) {
		this.metaClass = v;
	}
	
	@Override
	public MetaClass getMetaClass() {
        if (this.metaClass == null) 
        	this.metaClass = InvokerHelper.getMetaClass(getClass());
        
        return this.metaClass;
	}

	@Override
	public Object invokeMethod(String name, Object arg1) {
		// is really an object array
		Object[] args = (Object[])arg1;
		
		if (args.length > 0)
			System.out.println("G2: " + name + " - " + args[0]);
		else
			System.out.println("G2: " + name);
		
		// TODO Auto-generated method stub
		return null;
	}
	
	/*
	@Override
	public boolean hasMember(String name) {
		return this.hasField(name);
	}
	
	@Override
	public Object getMember(String name) {
		// TODO there is probably a better way...
		if ("getFieldAsInteger".equals(name)) {
			return new AbstractJSObject() {
				@Override
				public Object call(Object thiz, Object... args) {
					return RecordStruct.this.getFieldAsInteger((String)args[0]);		// TODO saftey
				}
			};
		}
		
		// TODO there is probably a better way...
		if ("cbTest".equals(name)) {
			return new AbstractJSObject() {
				@Override
				public Object call(Object thiz, Object... args) {
					System.out.println(args[0]);
					
					//ScriptFunction x = null;
					
					new Thread(() -> {
						try {
							((ScriptFunction)args[0]).getBoundInvokeHandle(args[0]).invoke(new RecordStruct(new FieldStruct("Data", "atad")));
						} 
						catch (Throwable x) {
							System.out.println("Invoke Error: " + x);
							x.printStackTrace();
						}
					}).start();
					
					return null;
				}
			};
		}
		
		Struct v = this.getField(name);
		
		if (v instanceof CompositeStruct)
			return v;
		
		return ((ScalarStruct) v).getGenericValue();
	}
	
	@Override
	public void removeMember(String name) {
		this.removeField(name);
	}
	
	@Override
	public void setMember(String name, Object value) {
		this.setField(name, value);
	}
	
	@Override
	public Collection<Object> values() {
		System.out.println("call to values...");
		
		return null;  //this.fields.values();  TODO
	}
	
	@Override
	public Set<String> keySet() {
		System.out.println("call to keyset...");
		
		return this.fields.keySet();
	}
	
	@Override
	public Object call(Object thiz, Object... args) {
		System.out.println("call to call... " + thiz);
		
		return null;  //super.call(thiz, args);  TODO
	}
	
	@Override
	public Object eval(String s) {
		System.out.println("a");
		
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean isArray() {
		System.out.println("b");
		
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean isFunction() {
		System.out.println("c");
		
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public Object newObject(Object... args) {
		System.out.println("d");
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getClassName() {
		System.out.println("e");
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getSlot(int arg0) {
		System.out.println("f");
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasSlot(int arg0) {
		System.out.println("g");
		
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isInstance(Object arg0) {
		System.out.println("h");
		
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isInstanceOf(Object arg0) {
		System.out.println("i");
		
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isStrictFunction() {
		System.out.println("j");
		
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setSlot(int arg0, Object arg1) {
		System.out.println("k");
		// TODO Auto-generated method stub
		
	}

	@Override
	public double toNumber() {
		System.out.println("l");
		// TODO Auto-generated method stub
		return 0;
	}
	*/
}
