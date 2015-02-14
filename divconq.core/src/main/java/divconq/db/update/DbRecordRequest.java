/* ************************************************************************
#
#  DivConq
#
#  http://divconq.com/
#
#  Copyright:
#    Copyright 2012 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package divconq.db.update;

import java.util.ArrayList;
import java.util.List;

import divconq.db.ReplicatedDataRequest;
import divconq.lang.BigDateTime;
import divconq.lang.op.OperationContext;
import divconq.struct.CompositeStruct;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;

/**
 * Insert a new record into dcDatabase, see dcInsertRecord schema.
 * 
 * @author Andy
 *
 */
abstract public class DbRecordRequest extends ReplicatedDataRequest {
	protected List<FieldRequest> fields = new ArrayList<>();	
	protected ListStruct sets = new ListStruct();
	
	protected String table = null;
	protected String id = null;
	protected BigDateTime when = BigDateTime.nowDateTime();
	
	public DbRecordRequest(String proc) {
		super(proc);
	}
		
	public DbRecordRequest withTable(String v) {
		this.table = v;
		
		this.fields.clear();
		
		return this;
	}
		
	public DbRecordRequest withId(String id) {
		this.id = id;
		return this;
	}

	public DbRecordRequest withFields(FieldRequest... fields) {
		for (FieldRequest fld : fields)
			this.fields.add(fld);		
		
		return this;
	}	

	public DbRecordRequest withSetField(String name, Object value) {
		if (value instanceof ConditionalValue) {
			if (!((ConditionalValue)value).set)
				return this;
			
			value = ((ConditionalValue)value).value;
		}
		
		divconq.schema.DbField fld = OperationContext.get().getSchema().getDbField(this.table, name);
		
		if (fld == null) 
			return this;
		
		FieldRequest dfld = new FieldRequest()
			.withName(name)
			.withValue(value);
		
		this.withFields(dfld);
		
		if (fld.dynamic)
			dfld.withRandomSubKey().withFrom(this.when);
		else if (fld.list)		
			dfld.withRandomSubKey();
		
		return this;
	}
	
	public DbRecordRequest withUpdateField(String name, Object value) {
		if (value instanceof ConditionalValue) {
			if (!((ConditionalValue)value).set)
				return this;
			
			value = ((ConditionalValue)value).value;
		}
		
		divconq.schema.DbField fld = OperationContext.get().getSchema().getDbField(this.table, name);
		
		if (fld == null) 
			return this;
		
		FieldRequest dfld = new FieldRequest()
			.withName(name)
			.withValue(value)
			.withUpdateOnly();
		
		this.withFields(dfld);
		
		if (fld.dynamic)
			dfld.withRandomSubKey().withFrom(this.when);
		else if (fld.list)		
			dfld.withRandomSubKey();
		
		return this;
	}

	public DbRecordRequest withSetField(String name, String subkey, Object value) {
		if (value instanceof ConditionalValue) {
			if (!((ConditionalValue)value).set)
				return this;
			
			value = ((ConditionalValue)value).value;
		}
		
		divconq.schema.DbField fld = OperationContext.get().getSchema().getDbField(this.table, name);
		
		if (fld == null) 
			return this;
		
		FieldRequest dfld = new FieldRequest()
			.withName(name)
			.withValue(value)
			.withSubKey(subkey);
		
		this.withFields(dfld);
		
		if (fld.dynamic)
			dfld.withFrom(this.when);
		
		return this;
	}

	public DbRecordRequest withUpdateField(String name, String subkey, Object value) {
		if (value instanceof ConditionalValue) {
			if (!((ConditionalValue)value).set)
				return this;
			
			value = ((ConditionalValue)value).value;
		}
		
		divconq.schema.DbField fld = OperationContext.get().getSchema().getDbField(this.table, name);
		
		if ((fld == null) || (!fld.dynamic && !fld.list)) 
			return this;
		
		FieldRequest dfld = new FieldRequest()
			.withName(name)
			.withValue(value)
			.withSubKey(subkey)
			.withUpdateOnly();
		
		this.withFields(dfld);
		
		if (fld.dynamic)
			dfld.withFrom(this.when);

		return this;
	}

	public DbRecordRequest withSetField(String name, String subkey, Object value, BigDateTime from) {
		if (value instanceof ConditionalValue) {
			if (!((ConditionalValue)value).set)
				return this;
			
			value = ((ConditionalValue)value).value;
		}
		
		divconq.schema.DbField fld = OperationContext.get().getSchema().getDbField(this.table, name);
		
		if ((fld == null) || !fld.dynamic) 
			return this;
		
		FieldRequest dfld = new FieldRequest()
			.withName(name)
			.withValue(value)
			.withSubKey(subkey)
			.withFrom(from);
		
		this.withFields(dfld);
		
		return this;
	}

	public DbRecordRequest withSetField(String name, String subkey, Object value, BigDateTime from, BigDateTime to) {
		if (value instanceof ConditionalValue) {
			if (!((ConditionalValue)value).set)
				return this;
			
			value = ((ConditionalValue)value).value;
		}
		
		divconq.schema.DbField fld = OperationContext.get().getSchema().getDbField(this.table, name);
		
		if ((fld == null) || !fld.dynamic || !fld.list) 
			return this;
		
		FieldRequest dfld = new FieldRequest()
			.withName(name)
			.withValue(value)
			.withSubKey(subkey)
			.withFrom(from)
			.withTo(to);
		
		this.withFields(dfld);
		
		return this;
	}
	
	// where pairs = even are source and odd are dest
	public DbRecordRequest withConditionallySetFields(RecordStruct source, String... pairs) {
		for (int i = 0; i < (pairs.length - 1); i += 2) {
			if (source.hasField(pairs[i])) 
				this.withSetField(pairs[i+1], source.getField(pairs[i]));
		}
		
		return this;
	}
	
	public DbRecordRequest withConditionallyUpdateFields(RecordStruct source, String... pairs) {
		for (int i = 0; i < (pairs.length - 1); i += 2) {
			if (source.hasField(pairs[i])) 
				this.withUpdateField(pairs[i+1], source.getField(pairs[i]));
		}
		
		return this;
	}
	
	public DbRecordRequest withSetList(String name, ListStruct values) {
		this.sets.addItem(new RecordStruct()
			.withField("Field", name)
			.withField("Values", values)
		);
		
		return this;
	}
	
	public DbRecordRequest withConditionallySetList(RecordStruct source, String sname, String dname) {
		if (!source.hasField(sname))
			return this;
		
		this.sets.addItem(new RecordStruct()
			.withField("Field", dname)
			.withField("Values", source.getFieldAsList(sname))
		);
		
		return this;
	}
	
	public DbRecordRequest withRetireField(String name) {
		divconq.schema.DbField fld = OperationContext.get().getSchema().getDbField(this.table, name);
		
		if (fld == null) 
			return this;
		
		FieldRequest dfld = new FieldRequest()
			.withName(name)
			.withRetired();
		
		this.withFields(dfld);
		
		return this;
	}

	public DbRecordRequest withRetireField(String name, String subkey) {
		divconq.schema.DbField fld = OperationContext.get().getSchema().getDbField(this.table, name);
		
		if (fld == null) 
			return this;
		
		FieldRequest dfld = new FieldRequest()
			.withName(name)
			.withRetired()
			.withSubKey(subkey);
		
		this.withFields(dfld);
		
		return this;
	}
	
	@Override
	public CompositeStruct buildParams() {
		RecordStruct flds = new RecordStruct();
		
		for (FieldRequest fld : this.fields) 
			flds.setField(fld.getName(), fld.getParams(flds));		
		
		RecordStruct params = new RecordStruct(
				new FieldStruct("Table", this.table),	
				new FieldStruct("Fields", flds)
		);
		
		if (StringUtil.isNotEmpty(this.id))
			params.setField("Id", this.id);
		
		params.setField("When", this.when);
		
		if (this.sets.getSize() > 0)
			params.setField("Sets", this.sets);
		
		this.parameters = params;
		
		return this.parameters;
	}
}
