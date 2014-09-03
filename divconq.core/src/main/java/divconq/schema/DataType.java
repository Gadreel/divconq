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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import divconq.lang.OperationResult;
import divconq.schema.Field.ReqTypes;
import divconq.struct.CompositeStruct;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.ScalarStruct;
import divconq.struct.Struct;
import divconq.struct.builder.ICompositeBuilder;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class DataType {
	enum DataKind {
		Scalar(1),
		List(2),
		Record(3);
	    
	    private int code;

	    private DataKind(int c) {
	      code = c;
	    }

	    public int getCode() {
	      return code;
	    }
	}

	protected Schema schema = null;
	protected String id = null;
	protected DataKind kind = null;
	protected boolean compiled = false;
	protected XElement definition = null;	
	protected List<XElement> xtraDefinitions = null;  //new ArrayList<XElement>();
	
	// for record
	protected HashMap<String,Field> fields = null; 
	protected boolean anyRec = false;
	
	// for list
	protected TypeOptionsList items = null;
	protected int minItems = 0;
	protected int maxItems = 0;
	
	// for scalar
	protected CoreType core = null;

	public String getId() {
		return this.id;
	}
	
	public boolean isAnyRecord() {
		return this.anyRec;
	}
	
	public DataType(Schema schema) {
		this.schema = schema;
	}

	public RecordStruct toJsonDef(int lvl) {
		if (lvl == 0) {
			RecordStruct def = new RecordStruct();
			def.setField("Kind", DataKind.Scalar);
			def.setField("CoreType", new CoreType(RootType.Any).toJsonDef());
			return def;
		}
		
		RecordStruct def = new RecordStruct();
		
		if (StringUtil.isNotEmpty(this.id))
			def.setField("Id", this.id);
		
		def.setField("Kind", this.kind.getCode());
		
		if (this.kind == DataKind.Record) {
			if (this.anyRec)
				def.setField("AnyRec", true);
			
			ListStruct fields = new ListStruct();
			
			for (Field fld : this.fields.values()) 
				fields.addItem(fld.toJsonDef(lvl - 1));
			
			def.setField("Fields", fields);
		}
		else if (this.kind == DataKind.List) {
			if (this.maxItems > 0)
				def.setField("MaxItems", this.maxItems);
			
			if (this.minItems > 0)
				def.setField("MinItems", this.minItems);
			
			if (this.items != null)
				def.setField("Items", this.items.toJsonDef(lvl - 1));
		}
		else if (this.kind == DataKind.Scalar) {
			if (this.core != null)
				def.setField("CoreType", this.core.toJsonDef());
		}
		
		return def;
	}
	
	public Collection<Field> getFields() {
		if (this.fields == null)
			return new ArrayList<Field>();
		
		return this.fields.values();
	}
	
	public Field getField(String name) {
		if (this.fields == null)
			return null;
		
		return this.fields.get(name);
	}
	
	public void load(OperationResult or, XElement dtel) {
		if (this.definition != null) {
			if (this.xtraDefinitions == null)
				this.xtraDefinitions = new ArrayList<XElement>();
			
			this.xtraDefinitions.add(dtel);			
			return;
		}
		
		this.definition = dtel;
		
		String elname = dtel.getName();
		
		if ("Record".equals(elname) || "Table".equals(elname) || "Request".equals(elname) || "Response".equals(elname) || "RecRequest".equals(elname) || "RecResponse".equals(elname)) 
			this.kind = DataKind.Record;
		else if ("List".equals(elname) || "ListRequest".equals(elname) || "ListResponse".equals(elname)) 
			this.kind = DataKind.List;
		else 
			this.kind = DataKind.Scalar;
		
		this.id = dtel.getAttribute("Id");
	}

	public void compile(OperationResult mr) {
		if (this.compiled)
			return;
		
		// to prevent recursion issues, mark compiled immediately
		this.compiled = true;
		
		if (this.kind == DataKind.Record)
			this.compileRecord(mr);
		else if (this.kind == DataKind.List)
			this.compileList(mr);
		else
			this.compileScalar(mr);
	}

	protected void compileRecord(OperationResult mr) {
		List<String> inhlist = new ArrayList<String>();
		
		if ("True".equals(this.definition.getAttribute("Any")))
			this.anyRec = true;
		
		String inherits = this.definition.getAttribute("Inherits");
		
		if (StringUtil.isNotEmpty(inherits)) {
			String[] ilist = inherits.split(",");
			
			for (int i = 0; i < ilist.length; i++)
				inhlist.add(ilist[i]);
		}		

		List<DataType> inheritTypes = new ArrayList<DataType>();
		
		for (String iname : inhlist) {
			DataType dtype = this.schema.manager.getType(iname);
			
			if (dtype == null) {
				mr.errorTr(413, iname);
				continue;
			}
			
			dtype.compile(mr);
			
			inheritTypes.add(dtype);
		}
		
		this.fields = new HashMap<String,Field>();
		
		for (XElement fel : this.definition.selectAll("Field")) {
			Field f = new Field(this.schema);
			f.compile(fel, mr);
			this.fields.put(f.name, f);
		}
		
		if (this.xtraDefinitions != null) {
			for (XElement el : this.xtraDefinitions) {
				for (XElement fel : el.selectAll("Field")) {
					Field f = new Field(this.schema);
					f.compile(fel, mr);
					this.fields.put(f.name, f);
				}
			}
		}
		
		for (DataType dt : inheritTypes) {
			for (Field fld : dt.getFields()) {
				if (!this.fields.containsKey(fld.name))
					this.fields.put(fld.name, fld);
			}
		}
	}

	protected void compileList(OperationResult mr) {
		this.items = new TypeOptionsList(this.schema);		
		this.items.compile(this.definition, mr);
		
		if (this.definition.hasAttribute("MinCount"))
			this.minItems = (int)StringUtil.parseInt(this.definition.getAttribute("MinCount"), 0);
		
		if (this.definition.hasAttribute("MaxCount"))
			this.maxItems = (int)StringUtil.parseInt(this.definition.getAttribute("MaxCount"), 0);
	}

	protected void compileScalar(OperationResult mr) {
		this.core = new CoreType(this.schema);
		this.core.compile(this.definition, mr);
	}

	// don't call this with data == null from a field if field required - required means "not null" so put the error in
	public boolean match(Object data, OperationResult mr) {
		this.compile(mr);
		
		if (this.kind == DataKind.Record) {
			if (data instanceof RecordStruct)
				return this.matchRecord((RecordStruct)data, mr);
			
			return false;
		}
		
		if (this.kind == DataKind.List) {
			if (data instanceof RecordStruct)
				return this.matchList((RecordStruct)data, mr);

			return false;
		}

		return this.matchScalar(data, mr);
	}

	protected boolean matchRecord(RecordStruct data, OperationResult mr) {
		if (this.fields != null) {
			
			// match only if all required fields are present 
			for (Field fld : this.fields.values()) {
				if ((fld.required == ReqTypes.True) && !data.hasField(fld.name))
					return false;
				
				if ((fld.required == ReqTypes.IfPresent) && data.hasField(fld.name) && data.isFieldEmpty(fld.name))
					return false;
			}
			
			return true;
		}
		
		// this is an exception to the rule, there is no "non-null" state to return from this method
		return this.anyRec;
	}

	protected boolean matchList(CompositeStruct data, OperationResult mr) {
		return true;		
	}

	protected boolean matchScalar(Object data, OperationResult mr) {
		if (this.core == null) 
			return false;
		
		return this.core.match(data, mr);
	}
	
	// don't call this with data == null from a field if field required - required means "not null" so put the error in
	// returns true only if there was a non-null value present that conforms to the expected structure (record, list or scalar) 
	// null values that do not conform should not cause an false
	public boolean validate(Struct data, OperationResult mr) {
		if (data == null)
			return false;
		
		this.compile(mr);
		
		if (this.kind == DataKind.Record) {
			if (data instanceof ICompositeBuilder)
				data = ((ICompositeBuilder)data).toLocal();			// TODO may be a source of a major inefficiency - may need to have configuration around it...
			
			if (data instanceof RecordStruct)
				return this.validateRecord((RecordStruct)data, mr);

			mr.errorTr(414, data);
			return false;
		}
		
		if (this.kind == DataKind.List) {
			if (data instanceof ListStruct)
				return this.validateList((ListStruct)data, mr);

			mr.errorTr(415, data);		
			return false;
		}

		return this.validateScalar(data, mr);
	}

	protected boolean validateRecord(RecordStruct data, OperationResult mr) {
		if (this.fields != null) {
			// handles all but the case where data holds a field not allowed 
			for (Field fld : this.fields.values()) 
				fld.validate(data.hasField(fld.name), data.getField(fld.name), mr);
			
			if (!this.anyRec)
				for (FieldStruct fld : data.getFields()) {
					if (! this.fields.containsKey(fld.getName()))
						mr.errorTr(419, fld.getName(), data);	
				}
		}
		
		// this is an exception to the rule, there is no "non-null" state to return from this method
		return true;
	}

	protected boolean validateList(ListStruct data, OperationResult mr) {
		if (this.items == null) 
			mr.errorTr(416, data);   
		else
			for (Struct obj : data.getItems())
				this.items.validate(obj, mr);		
		
		if ((this.minItems > 0) && (data.getSize() < this.minItems))
			mr.errorTr(417, data);   
		
		if ((this.maxItems > 0) && (data.getSize() > this.maxItems))
			mr.errorTr(418, data);   
		
		return true;		
	}

	protected boolean validateScalar(Struct data, OperationResult mr) {
		if (this.core == null) {
			mr.errorTr(420, data);   
			return false;
		}
		
		// if we are expecting a special class, try to resolve validation via that class 
		if (this.definition.hasAttribute("Class") && (data != null)) {
			String cname = this.definition.getAttribute("Class");
			
			if (data.getClass().getName().equals(cname) && (data instanceof ScalarStruct)) 
				return this.core.validate(((ScalarStruct)data).toInternalValue(this.core.root), mr);
		}
		
		return this.core.validate(data, mr);
	}
	
	
	public Struct wrap(Object data, OperationResult mr) {
		if (data == null) 
			return null;
		
		this.compile(mr);
		
		if (this.kind == DataKind.Record) {
			if (data instanceof RecordStruct) {
				Struct s = (Struct)data;

				// TODO check that type/inheritance is ok
				if (!s.hasExplicitType())
					s.setType(this);
				
				return s;
			}
			
			mr.errorTr(421, data);		
			return null;
		}
		
		if (this.kind == DataKind.List) {
			if (data instanceof ListStruct) {
				Struct s = (Struct)data;
				
				// TODO check that type/inheritance is ok
				if (!s.hasExplicitType())
					s.setType(this);
				
				return s;
			}
			
			mr.errorTr(439, data);		
			return null;
		} 
		
		Struct s = this.core.wrap(data, mr);
		
		if (s != null) {
			if (!s.hasExplicitType())
				s.setType(this);
			
			return s;
		}
		
		return null;
	}
	
	public Struct wrapItem(Object data, OperationResult mr) {
		if (data == null) 
			return null;
		
		this.compile(mr);
		
		if (this.kind == DataKind.Record) {
			mr.errorTr(422, data);		
			return null;
		}
		
		if (this.kind == DataKind.List) 
			return this.items.wrap(data, mr);
		
		Struct s = this.core.wrap(data, mr);
		
		if (s != null) {
			if (!s.hasExplicitType())
				s.setType(this);
			
			return s;
		}
		
		return null;
	}
	
	public Struct create(OperationResult mr) {
		this.compile(mr);
		
		Struct st = null;
		
		if (this.kind == DataKind.Record) {
			st = new RecordStruct();
		}		
		if (this.kind == DataKind.List) {
			st = new ListStruct();
		}
		else {
			if (this.definition.hasAttribute("Class")) {
				try {
					Class<?> spectype = this.getClass().getClassLoader().loadClass(this.definition.getAttribute("Class"));
					st = (Struct) spectype.newInstance();
				} 
				catch (Exception x) {
					// TODO log
					return null;
				}
			}	
			else if (this.core != null)
				st = this.core.create(mr);
		}
		
		// TODO err message if null
		
		if (st != null)
			st.setType(this);
		
		return st; 
	}

	public DataType getPrimaryItemType() {
		if (this.items != null) 
			return this.items.getPrimaryType();
		
		return null;
	}
	
	public CoreType getCoreType() {
		return this.core;
	}
}
