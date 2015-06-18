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

import divconq.lang.op.OperationContext;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class TypeOptionsList {
	protected Schema schema = null;
	protected List<DataType> options = new ArrayList<DataType>();
	
	public TypeOptionsList(Schema schema) {
		this.schema = schema;
	}

	public RecordStruct toJsonDef(int lvl) {
		RecordStruct def = new RecordStruct();
		
		ListStruct rests = new ListStruct();
		
		for (DataType dt : this.options) 
			rests.addItem(dt.toJsonDef(lvl));
		
		def.setField("Options", rests);
		
		return def;
	}
	
	public void compile(XElement def) {		
		String t1 = def.getAttribute("Type");
		
		if (StringUtil.isNotEmpty(t1)) {
			this.options = this.schema.manager.lookupOptionsType(t1);
			return;
		}
		
		for (XElement dtel : def.selectAll("*")) { 
			DataType dt = new DataType(this.schema);
			dt.load(dtel);
			dt.compile();
			this.options.add(dt);
		}
	}
	
	// don't call this with data == null from a field if field required - required means "not null" so put the error in
	public boolean validate(Struct data) {
		if (data == null)
			return false;
		
		if (this.options.size() == 0) {
			OperationContext.get().errorTr(437, data);			
			return false;
		}
		
		if (this.options.size() == 1) 
			return this.options.get(0).validate(data);
		
		for (DataType dt : this.options) {
			if (dt.match(data)) 
				return dt.validate(data);
		}
		
		OperationContext.get().errorTr(438, data);			
		return false;
	}
	
	public Struct normalizeValidate(Struct data) {
		if (data == null)
			return null;
		
		if (this.options.size() == 0) {
			OperationContext.get().errorTr(437, data);			
			return null;
		}
		
		if (this.options.size() == 1) 
			return this.options.get(0).normalizeValidate(data);
		
		for (DataType dt : this.options) {
			if (dt.match(data)) 
				return dt.normalizeValidate(data);
		}
		
		OperationContext.get().errorTr(438, data);			
		return null;
	}
	
	public Struct wrap(Object data) {
		if (data == null) 
			return null;
		
		if (this.options.size() == 0) 
			return null;
		
		if (this.options.size() == 1) 
			return this.options.get(0).wrap(data);
		
		for (DataType dt : this.options) {
			if (dt.match(data)) 
				return dt.wrap(data);
		}
		
		return null;
	}
	
	public DataType getPrimaryType() {
		if (this.options.size() == 0) 
			return null;
		
		return this.options.get(0);
	}
}
