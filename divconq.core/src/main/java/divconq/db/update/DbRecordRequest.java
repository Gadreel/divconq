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
import divconq.hub.Hub;
import divconq.lang.BigDateTime;
import divconq.schema.DbTable;
import divconq.struct.CompositeStruct;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;

/**
 * Insert a new record into dcDatabase, see dcInsertRecord schema.
 * 
 * @author Andy
 *
 */
abstract public class DbRecordRequest extends ReplicatedDataRequest {
	protected List<DbField> fields = new ArrayList<>();	
	protected ListStruct sets = new ListStruct();
	
	protected String table = null;
	protected String id = null;
	protected String filter = null;
	protected Struct extra = null;
	protected BigDateTime when = BigDateTime.nowDateTime();
	
	protected DbTable schema = null;
	
	public DbRecordRequest(String proc) {
		super(proc);
	}
		
	public DbRecordRequest withTable(String v) {
		this.table = v;
		
		this.schema = Hub.instance.getSchema().getDb().getTable(table);
		this.fields.clear();
		
		return this;
	}
		
	public DbRecordRequest withId(String id) {
		this.id = id;
		return this;
	}
	
	public DbRecordRequest withFilter(String filter) {
		this.filter = filter;
		return this;
	}
	
	public DbRecordRequest withExtra(Struct extra) {
		this.extra = extra;
		return this;
	}

	public DbRecordRequest withFields(DbField... fields) {
		for (DbField fld : fields)
			this.fields.add(fld);		
		
		return this;
	}	

	public DbRecordRequest withSetOrRetireField(String name, Object value) {
		if (value == null) {
			this.withRetireField(name);
			return this;
		}
		
		if ((value instanceof String) && StringUtil.isEmpty((String)value)) {
			this.withRetireField(name);
			return this;
		}

		this.withSetField(name, value);
		
		return this;
	}
	
	public DbRecordRequest withSetField(String name, Object value) {
		if (value instanceof ConditionalValue) {
			if (!((ConditionalValue)value).set)
				return this;
			
			value = ((ConditionalValue)value).value;
		}
		
		if (this.schema == null)
			return this;
		
		divconq.schema.DbField fld = this.schema.getField(name);
		
		if (fld == null) 
			return this;
		
		if (fld.dynamic && fld.list)		
			this.withFields(new DynamicListField(name, null, value, this.when));
		else if (fld.dynamic)		
			this.withFields(new DynamicScalarField(name, null, value, this.when));
		else if (fld.list)		
			this.withFields(new ListField(name, null, value));
		else		
			this.withFields(new ScalarField(name, value));
		
		return this;
	}

	public DbRecordRequest withSetOrRetireField(String name, String subkey, Object value) {
		if (value == null) {
			this.withRetireField(name, subkey);
			return this;
		}
		
		if ((value instanceof String) && StringUtil.isEmpty((String)value)) {
			this.withRetireField(name, subkey);
			return this;
		}

		this.withSetField(name, subkey, value);
		
		return this;
	}

	public DbRecordRequest withSetField(String name, String subkey, Object value) {
		if (value instanceof ConditionalValue) {
			if (!((ConditionalValue)value).set)
				return this;
			
			value = ((ConditionalValue)value).value;
		}
		
		if (this.schema == null)
			return this;
		
		divconq.schema.DbField fld = this.schema.getField(name);
		
		if (fld == null) 
			return this;
		
		if (fld.dynamic && fld.list)		
			this.withFields(new DynamicListField(name, subkey, value, this.when));
		else if (fld.dynamic)		
			this.withFields(new DynamicScalarField(name, subkey, value, this.when));
		else if (fld.list)		
			this.withFields(new ListField(name, subkey, value));
		
		return this;
	}

	public DbRecordRequest withSetOrRetireField(String name, String subkey, Object value, BigDateTime from) {
		if (value == null) {
			this.withRetireField(name, subkey);
			return this;
		}
		
		if ((value instanceof String) && StringUtil.isEmpty((String)value)) {
			this.withRetireField(name, subkey);
			return this;
		}

		this.withSetField(name, subkey, value, from);
		
		return this;
	}

	public DbRecordRequest withSetField(String name, String subkey, Object value, BigDateTime from) {
		if (value instanceof ConditionalValue) {
			if (!((ConditionalValue)value).set)
				return this;
			
			value = ((ConditionalValue)value).value;
		}
		
		if (this.schema == null)
			return this;
		
		divconq.schema.DbField fld = this.schema.getField(name);
		
		if ((fld == null) || !fld.dynamic) 
			return this;
		
		if (fld.list)		
			this.withFields(new DynamicListField(name, subkey, value, from));
		else 		
			this.withFields(new DynamicScalarField(name, subkey, value, from));
		
		return this;
	}

	public DbRecordRequest withSetOrRetireField(String name, String subkey, Object value, BigDateTime from, BigDateTime to) {
		if (value == null) {
			this.withRetireField(name, subkey);
			return this;
		}
		
		if ((value instanceof String) && StringUtil.isEmpty((String)value)) {
			this.withRetireField(name, subkey);
			return this;
		}

		this.withSetField(name, subkey, value, from, to);
		
		return this;
	}

	public DbRecordRequest withSetField(String name, String subkey, Object value, BigDateTime from, BigDateTime to) {
		if (value instanceof ConditionalValue) {
			if (!((ConditionalValue)value).set)
				return this;
			
			value = ((ConditionalValue)value).value;
		}
		
		if (this.schema == null)
			return this;
		
		divconq.schema.DbField fld = this.schema.getField(name);
		
		if ((fld == null) || !fld.dynamic || !fld.list) 
			return this;
		
		this.withFields(new DynamicListField(name, subkey, value, from, to));
		
		return this;
	}

	public DbRecordRequest withListToField(String name, boolean valueAsSubkey, ListStruct list) {
		if (this.schema == null)
			return this;
		
		divconq.schema.DbField fld = this.schema.getField(name);
		
		if (fld == null || !fld.list) 
			return this;
		
		for (Struct item : list.getItems())
			if (fld.dynamic)
				this.withFields(new DynamicListField(name, valueAsSubkey ? item.toString() : null, item, this.when));
			else
				this.withFields(new ListField(name, valueAsSubkey ? item.toString() : null, item));
		
		return this;
	}
	
	public DbRecordRequest withConditionallySetField(RecordStruct source, String sname) {
		if (source.hasField(sname)) 
			this.withSetField(sname, source.getField(sname));
		
		return this;
	}
	
	public DbRecordRequest withConditionallySetField(RecordStruct source, String sname, String dname) {
		if (source.hasField(sname)) 
			this.withSetField(dname, source.getField(sname));
		
		return this;
	}
	
	/**
	 * meaningless for any list
	 * 
	 * @param name
	 */
	public DbRecordRequest withRetireField(String name) {
		if (this.schema == null)
			return this;
		
		divconq.schema.DbField fld = this.schema.getField(name);
		
		if (fld == null) 
			return this;
		
		if (!fld.dynamic && !fld.list)		
			this.withFields(new ScalarField(name));
		else if (fld.dynamic && !fld.list) {
			DynamicScalarField dfld = new DynamicScalarField(name, null, (Object)null);
			dfld.setWhen(this.when);
			this.withFields(dfld);
		}
		
		return this;
	}

	public DbRecordRequest withRetireField(String name, String subkey) {
		if (this.schema == null)
			return this;
		
		divconq.schema.DbField fld = this.schema.getField(name);
		
		if (fld == null) 
			return this;
		
		if (fld.dynamic && fld.list)		
			this.withFields(new DynamicListField(name, subkey));
		else if (fld.dynamic) {		
			DynamicScalarField dfld = new DynamicScalarField(name, subkey, (Object)null);
			dfld.setWhen(this.when);
			this.withFields(dfld);
		}
		else if (fld.list)		
			this.withFields(new ListField(name, subkey));
		
		return this;
	}
	
	// scalar

	public DbRecordRequest withSetScalar(String name, Object value) {
		if (value instanceof ConditionalValue) {
			if (!((ConditionalValue)value).set)
				return this;
			
			value = ((ConditionalValue)value).value;
		}
		
		this.withFields(new ScalarField(name, value));
		
		return this;
	}

	public DbRecordRequest withRetireScalar(String dname) {
		this.withFields(new ScalarField(dname));
		
		return this;
	}

	public DbRecordRequest withSetScalar(RecordStruct source, String sname) {
		this.withSetScalar(source, sname, sname);
		
		return this;
	}

	public DbRecordRequest withSetScalar(RecordStruct source, String sname, String dname) {
		this.withFields(new ScalarField(dname, source.getField(sname)));
		
		return this;
	}

	public DbRecordRequest withConditionallySetScalar(RecordStruct source, String sname) {
		this.withConditionallySetScalar(source, sname, sname);
		
		return this;
	}

	public DbRecordRequest withConditionallySetScalar(RecordStruct source, String sname, String dname) {
		if (source.hasField(sname)) 
			this.withFields(new ScalarField(dname, source.getField(sname)));
		
		return this;
	}
	
	// dynamic scalar
	
	public DbRecordRequest withSetDynamicScalar(String name, String subkey, Object value) {
		this.withSetDynamicScalar(name, subkey, value, this.when);
		
		return this;
	}
	
	public DbRecordRequest withSetDynamicScalar(String name, String subkey, Object value, BigDateTime when) {
		if (value instanceof ConditionalValue) {
			if (!((ConditionalValue)value).set)
				return this;
			
			value = ((ConditionalValue)value).value;
		}
		
		this.withFields(new DynamicScalarField(name, subkey, value, when));
		
		return this;
	}
	
	public DbRecordRequest withRetireDynamicScalar(String dname, String subkey) {
		this.withFields(new DynamicScalarField(dname, subkey, this.when));
		
		return this;
	}
	
	public DbRecordRequest withSetDynamicScalar(RecordStruct source, String sname, String subkey) {
		this.withSetDynamicScalar(source, sname, sname, subkey);
		
		return this;
	}
	
	public DbRecordRequest withSetDynamicScalar(RecordStruct source, String sname, String dname, String subkey) {
		this.withFields(new DynamicScalarField(dname, subkey, source.getField(sname), this.when));
		
		return this;
	}
	
	public DbRecordRequest withConditionallySetDynamicScalar(RecordStruct source, String sname, String subkey) {
		this.withConditionallySetDynamicScalar(source, sname, sname, subkey);
		
		return this;
	}
	
	public DbRecordRequest withConditionallySetDynamicScalar(RecordStruct source, String sname, String dname, String subkey) {
		if (source.hasField(sname)) 
			this.withFields(new DynamicScalarField(dname, subkey, source.getField(sname), this.when));
		
		return this;
	}

	// list
	
	public DbRecordRequest withSetList(String name, String subkey, Object value) {
		if (value instanceof ConditionalValue) {
			if (!((ConditionalValue)value).set)
				return this;
			
			value = ((ConditionalValue)value).value;
		}
		
		this.withFields(new ListField(name, subkey, value));
		
		return this;
	}

	public DbRecordRequest withReplaceList(String name, ListStruct values) {
		this.sets.addItem(new RecordStruct()
			.withField("Field", name)
			.withField("Values", values)
		);
		
		return this;
	}

	public DbRecordRequest withCopyList(String name, boolean valueAsSubkey, ListStruct list) {
		if (list != null) {
			for (Struct item : list.getItems()) 
				this.withFields(new ListField(name, valueAsSubkey ? item.toString() : null, item));					
		}
		
		return this;
	}
	
	public DbRecordRequest withRetireList(String name, String subkey) {
		this.withFields(new ListField(name, subkey));
		
		return this;
	}
	
	public DbRecordRequest withSetList(RecordStruct source, String sname, String subkey) {
		this.withSetList(source, sname, sname, subkey);
		
		return this;
	}

	public DbRecordRequest withSetList(RecordStruct source, String sname, String dname, String subkey) {
		this.withFields(new ListField(dname, subkey, source.getField(sname)));
		
		return this;
	}

	public DbRecordRequest withCopyList(RecordStruct source, String sname, String dname, boolean valueAsSubkey) {
		if (source.hasField(sname)) {
			Struct list = source.getField(sname);
			
			if (list instanceof ListStruct) {
				for (Struct item : ((ListStruct) list).getItems()) 
					this.withFields(new ListField(dname, valueAsSubkey ? item.toString() : null, item));					
			}			
		}
		
		return this;
	}

	public DbRecordRequest withConditionallyReplaceList(RecordStruct source, String sname, String dname) {
		if (!source.hasField(sname))
			return this;
		
		this.sets.addItem(new RecordStruct()
			.withField("Field", dname)
			.withField("Values", source.getFieldAsList(sname))
		);
		
		return this;
	}

	public DbRecordRequest withConditionallySetList(RecordStruct source, String sname, String subkey) {
		this.withConditionallySetList(source, sname, sname, subkey);
		
		return this;
	}

	public DbRecordRequest withConditionallySetList(RecordStruct source, String sname, String dname, String subkey) {
		if (source.hasField(sname)) 
			this.withFields(new ListField(dname, subkey, source.getField(sname)));
		
		return this;
	}

	// dynamic list
	
	public DbRecordRequest withSetDynamicList(String name, String subkey, Object value) {
		this.withSetDynamicList(name, subkey, value, this.when, null);
		
		return this;
	}
	
	public DbRecordRequest withSetDynamicList(String name, String subkey, Object value, BigDateTime from) {
		this.withSetDynamicList(name, subkey, value, from, null);
		
		return this;
	}
	
	public DbRecordRequest withSetDynamicList(String name, String subkey, Object value, BigDateTime from, BigDateTime to) {
		if (value instanceof ConditionalValue) {
			if (!((ConditionalValue)value).set)
				return this;
			
			value = ((ConditionalValue)value).value;
		}
		
		this.withFields(new DynamicListField(name, subkey, value, from, to));
		
		return this;
	}

	public DbRecordRequest withCopyDynamicList(String name, boolean valueAsSubkey, ListStruct list) {
		if (list != null) {
			for (Struct item : list.getItems()) 
				this.withFields(new DynamicListField(name, valueAsSubkey ? item.toString() : null, item, this.when));					
		}
		
		return this;
	}
	
	public DbRecordRequest withRetireDynamicList(String name, String subkey) {
		this.withFields(new DynamicListField(name, subkey, this.when));
		
		return this;
	}
	
	public DbRecordRequest withSetDynamicList(RecordStruct source, String sname, String subkey) {
		this.withSetDynamicList(source, sname, sname, subkey);
		
		return this;
	}

	public DbRecordRequest withSetDynamicList(RecordStruct source, String sname, String dname, String subkey) {
		this.withFields(new DynamicListField(dname, subkey, source.getField(sname), this.when));
		
		return this;
	}

	public DbRecordRequest withCopyDynamicList(RecordStruct source, String sname, String dname, boolean valueAsSubkey) {
		if (source.hasField(sname)) {
			Struct list = source.getField(sname);
			
			if (list instanceof ListStruct) {
				for (Struct item : ((ListStruct) list).getItems()) 
					this.withFields(new DynamicListField(dname, valueAsSubkey ? item.toString() : null, item, this.when));					
			}			
		}
		
		return this;
	}

	public DbRecordRequest withConditionallySetDynamicList(RecordStruct source, String sname, String subkey) {
		this.withConditionallySetDynamicList(source, sname, sname, subkey);
		
		return this;
	}

	public DbRecordRequest withConditionallySetDynamicList(RecordStruct source, String sname, String dname, String subkey) {
		if (source.hasField(sname)) {
			this.withFields(new DynamicListField(dname, subkey, source.getField(sname), this.when));
		}
		
		return this;
	}
	
	@Override
	public CompositeStruct buildParams() {
		RecordStruct flds = new RecordStruct();
		
		for (DbField fld : this.fields) 
			flds.setField(fld.getName(), fld.getParams(flds));		
		
		RecordStruct params = new RecordStruct(
				new FieldStruct("Table", this.table),	
				new FieldStruct("Fields", flds)
		);
		
		if (StringUtil.isNotEmpty(this.id))
			params.setField("Id", this.id);
		
		if (StringUtil.isNotEmpty(this.filter))
			params.setField("Filter", this.filter);
		
		if (this.extra != null)
			params.setField("Extra", this.extra);
		
		params.setField("When", this.when);
		
		if (this.sets.getSize() > 0)
			params.setField("Sets", this.sets);
		
		this.parameters = params;
		
		return this.parameters;
	}
}
