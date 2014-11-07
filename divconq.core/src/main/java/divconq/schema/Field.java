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
import java.util.List;

import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationResult;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class Field {
	enum ReqTypes {
		True(1),
		False(2),
		IfPresent(3);
	    
	    private int code;

	    private ReqTypes(int c) {
	      code = c;
	    }

	    public int getCode() {
	      return code;
	    }
	}

	protected Schema schema = null;
	protected List<DataType> options = new ArrayList<DataType>();
	protected String name = null;
	protected ReqTypes required = ReqTypes.False;
	
	public Field(Schema schema) {
		this.schema = schema;
	}

	public RecordStruct toJsonDef(int lvl) {
		RecordStruct def = new RecordStruct();
		
		def.setField("Name", this.name);
		
		ListStruct rests = new ListStruct();
		
		for (DataType dt : this.options) 
			rests.addItem(dt.toJsonDef(lvl));
		
		def.setField("Options", rests);
		
		def.setField("Required", this.required.getCode());
		
		return def;
	}
	
	public void compile(XElement fel, OperationResult mr) {
		this.name = fel.getAttribute("Name");
		
		String req = fel.getAttribute("Required");
		
		if ("True".equals(req))
			this.required = ReqTypes.True;
		else if ("IfPresent".equals(req))
			this.required = ReqTypes.IfPresent;
		
		String t1 = fel.getAttribute("Type");
		
		if (StringUtil.isNotEmpty(t1)) {
			this.options = this.schema.manager.lookupOptionsType(t1, mr);
			return;
		}
		
		String f1 = fel.getAttribute("ForeignKey");
		
		if (StringUtil.isNotEmpty(f1)) {
			this.options = this.schema.manager.lookupOptionsType("Id", mr);
			return;
		}
		
		for (XElement dtel : fel.selectAll("*")) { 
			DataType dt = new DataType(this.schema);
			dt.load(mr, dtel);
			dt.compile(mr);
			this.options.add(dt);
		}
	}
		
	// don't call this with data == null from a field if field required - required means "not null" so put the error in
	public void validate(boolean present, Struct data, OperationResult mr) {
		if (data == null) {
			this.valueUnresolved(present, data, mr);
			return;
		}   
		
		if (this.options.size() == 0) {
			mr.errorTr(423, data);			
			return;
		}
		
		if (this.options.size() == 1) { 
			if (!this.options.get(0).validate(data, mr))
				this.valueUnresolved(present, data, mr);
			
			return;
		}
		
		for (DataType dt : this.options) {
			if (dt.match(data, mr)) {
				if (!dt.validate(data, mr))
					this.valueUnresolved(present, data, mr);
				
				return;
			}
		}
		
		mr.errorTr(440, data);			
		return;
	}
	
	protected void valueUnresolved(boolean present, Object data, OperationResult mr) {
		if (data != null) {
			mr.errorTr(440, data);			
			return;
		}
		
		if (this.required == ReqTypes.False)
			return;
		
		if (this.required == ReqTypes.IfPresent && !present)
			return;
		
		mr.errorTr(424, data, this.name);			
	}
	
	public Struct wrap(Object data, OperationResult mr) {
		if (data == null) 
			return null;
		
		if (this.options.size() == 0) 
			return null;
		
		if (this.options.size() == 1) 
			return this.options.get(0).wrap(data, mr);
		
		for (DataType dt : this.options) {
			if (dt.match(data, mr)) 
				return dt.wrap(data, mr);
		}
		
		return null;
	}
	
	public FuncResult<Struct> create() {
		if (this.options.size() == 0) 
			return null;
		
		return this.options.get(0).create();
	}
	
	public DataType getPrimaryType() {
		if (this.options.size() == 0) 
			return null;
		
		return this.options.get(0);
	}
}
