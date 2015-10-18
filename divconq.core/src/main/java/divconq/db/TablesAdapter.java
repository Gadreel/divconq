package divconq.db;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import divconq.db.util.ByteUtil;
import divconq.hub.DomainInfo;
import divconq.hub.Hub;
import divconq.lang.BigDateTime;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.schema.DbField;
import divconq.schema.DbTable;
import divconq.schema.DbTrigger;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;
import static divconq.db.Constants.*;

public class TablesAdapter {
	protected DatabaseInterface conn = null;
	protected DatabaseTask task = null;
	
	// don't call for general code...
	public TablesAdapter(DatabaseInterface conn, DatabaseTask task) {
		this.conn = conn;
		this.task = task;
	}
	
	public FuncResult<String> createRecord(String table) {
		FuncResult<String> or = new FuncResult<String>();
		
		String hid = OperationContext.getHubId();
		
		byte[] metakey = ByteUtil.buildKey(DB_GLOBAL_RECORD_META, table, "Id", hid);
		
		// use common Id's across all domains so that merge works and so that 
		// sys domain records (users) can be reused across domains
		try {
			Long id = this.conn.inc(metakey);
			
			or.setResult(hid + "_" + StringUtil.leftPad(id.toString(), 15, '0')); 
		}
		catch (Exception x) {
			or.error("Unable to create record id: " + x);
		}
		
		return or;
	}
	
	public OperationResult checkFields(String table, RecordStruct fields, String inId) {
		OperationResult or = new OperationResult();
		
		BiConsumer<DbField,RecordStruct> fieldChecker = new BiConsumer<DbField,RecordStruct>() {
			@Override
			public void accept(DbField schema, RecordStruct data) {
				boolean retired = data.getFieldAsBooleanOrFalse("Retired");
				
				if (retired) {
					if (schema.isRequired()) 
						OperationContext.get().error("Field cannot be retired: " + table + " - " + schema.getName());
					
					return;
				}
				
				// validate data type
				Struct value = data.getField("Data");
				
				if (value == null) {
					if (schema.isRequired()) 
						OperationContext.get().error("Field cannot be null: " + table + " - " + schema.getName());
					
					return;
				}
				
				/*
				OperationResult cor = TablesAdapter.this.task.getSchema().validateType(value, schema.getTypeId()); 
				
				if (cor.hasErrors()) 
					return;
				*/
				
				FuncResult<Struct> cor = TablesAdapter.this.task.getSchema().normalizeValidateType(value, schema.getTypeId()); 
				
				if (cor.hasErrors()) 
					return;
				
				data.setField("Data", cor.getResult());
				
				Object cValue = Struct.objectToCore(value);
				
				if (cValue == null) {
					if (schema.isRequired()) 
						OperationContext.get().error("Field cannot be null: " + table + " - " + schema.getName());
					
					return;
				}
				
				if (!schema.isUnique())
					return;
				
				// make sure value is unique - null for when is fine because uniqueness is not time bound
				Object id = TablesAdapter.this.firstInIndex(table, schema.getName(), cValue, null, false);
				
				// if we are a new record
				if (inId == null) {
					if (id != null) {
						OperationContext.get().error("Field must be unique: " + table + " - " + schema.getName());
						return;
					}
					
				}
				// if we are not a new record
				else if (id != null) {
					if (!inId.equals(id)) {
						OperationContext.get().error("Field already in use, must be unique: " + table + " - " + schema.getName());
						return;
					}
				}
			}
		}; 
		
		// checking incoming fields for type correctness, uniqueness and requiredness
		for (FieldStruct field : fields.getFields()) {
			String fname = field.getName();
			
			try {
				DbField schema = this.task.getSchema().getDbField(table, fname);
				
				if (schema == null) {
					OperationContext.get().error("Field not defined: " + table + " - " + fname);
					continue;
				}
				
				// --------------------------------------
				// StaticScalar handling - Data or Retired (true) not both
				// --------------------------------------
				if (!schema.isList() && !schema.isDynamic()) {
					fieldChecker.accept(schema, (RecordStruct) field.getValue());
				}
				// --------------------------------------
				// StaticList handling
				// DynamicScalar handling
				// DynamicList handling
				// --------------------------------------
				else {
					for (FieldStruct subid : ((RecordStruct) field.getValue()).getFields()) 
						fieldChecker.accept(schema, (RecordStruct) subid.getValue());
				}
			}
			catch (Exception x) {
				or.error("Error checking field: " + fname);
			}
		}
			
		// if we are a new record, check that we have all the required fields
		if (inId == null) {
			for (DbField schema : this.task.getSchema().getDbFields(table)) {
				if (!schema.isRequired())
					continue;
				
				// all we need to do is check if the field is present, the checks above have already shown
				// that fields present pass the required check
				if (!fields.hasField(schema.getName())) 
					OperationContext.get().error("Field missing but required: " + table + " - " + schema.getName());
			}			
		}
		
		return or;
	}
	
	public OperationResult checkSetFields(String table, String id, RecordStruct fields) {
		OperationResult cor = this.checkFields(table, fields, id);
		
		if (cor.hasErrors())
			return cor;
		
		return this.setFields(table, id, fields);
	}
	
	public OperationResult setFields(String table, String id, RecordStruct fields) {
		OperationResult or = new OperationResult();
		
		boolean auditDisabled = this.conn.isAuditDisabled();
		BigDecimal stamp = this.task.getStamp();
		String did = this.task.getDomain();
		
		try {
			boolean recPresent = this.conn.hasAny(DB_GLOBAL_RECORD, did, table, id);
			
			if (!recPresent) {
				byte[] metacnt = ByteUtil.buildKey(DB_GLOBAL_RECORD_META, did, table, "Count");
				
				// update the record count
				this.conn.inc(metacnt);
			}
		}
		catch (Exception x) {
			or.error("Unable to check/update record count: " + x);
			return or;
		}

		// not just retired, but truly deleted - then proceed no further
		// could hit a race condition - DB cleanup will look for "deleted" records with data and remove TODO
		if (this.isDeleted(table, id)) {
			or.errorTr(50009);
			return or;
		}
		
		// --------------------------------------------------
		//   index updates herein may have race condition issues with the value counts
		//   this is ok as counts are just used for suggestions anyway
		//   TODO - DB cleanup on all indexes
		//   TODO - DB cleanup on all index value counts
		// --------------------------------------------------
		
		for (FieldStruct field : fields.getFields()) {
			String fname = field.getName();
			
			try {
				DbField schema = this.task.getSchema().getDbField(table, fname);
				
				if (schema == null) {
					OperationContext.get().error("Field not defined: " + table + " - " + fname);
					continue;
				}
				
				// --------------------------------------
				// StaticScalar handling - Data or Retired (true) not both
				//
				//   fields([field name],"Data") = [value]
				//   fields([field name],"Tags") = [value]
				//   fields([field name],"Retired") = [value]
				// --------------------------------------
				if (!schema.isList() && !schema.isDynamic()) {
					RecordStruct data = (RecordStruct) field.getValue();
					
					Object newValue = Struct.objectToCore(data.getField("Data"));
					String tags = data.getFieldAsString("Tags");
					boolean retired = data.getFieldAsBooleanOrFalse("Retired");
					boolean updateOnly = data.getFieldAsBooleanOrFalse("UpdateOnly");
					
					// find the first, newest, stamp 
					byte[] newerStamp = this.conn.nextPeerKey(DB_GLOBAL_RECORD, did, table, id, fname, null);
					
					boolean hasNewer = false;
					
					if (newerStamp != null) {
						BigDecimal newStamp = Struct.objectToDecimal(ByteUtil.extractValue(newerStamp));
						hasNewer = stamp.compareTo(newStamp) > 0;  // if we come after newer then we are older info, there is newer
					}
					
					// find the next, older, stamp after current
					byte[] olderStamp = this.conn.nextPeerKey(DB_GLOBAL_RECORD, did, table, id, fname, stamp);
					boolean oldIsSet = false;
					boolean oldIsRetired = false;
					Object oldValue = null;
					
					if (olderStamp != null) {
						BigDecimal oldStamp = Struct.objectToDecimal(ByteUtil.extractValue(olderStamp));
						
						// try to get the data if any - note retired fields have no data
						if (oldStamp != null) {
							oldIsRetired = this.conn.getAsBooleanOrFalse(DB_GLOBAL_RECORD, did, table, id, fname, oldStamp, "Retired");
							oldIsSet = this.conn.isSet(DB_GLOBAL_RECORD, did, table, id, fname, oldStamp, "Data");
							
							if (oldIsSet) 
								oldValue = this.conn.get(DB_GLOBAL_RECORD, did, table, id, fname, oldStamp, "Data");
						}
					}
					
					boolean effectivelyEqual = (retired && oldIsRetired) || ((oldValue == null) && (newValue == null)) || ((oldValue != null) && oldValue.equals(newValue));
					
					if (updateOnly && effectivelyEqual) 
						continue;
					
					// set either retired or data, not both
					if (retired) {
						if (oldIsSet && auditDisabled)
							this.conn.kill(DB_GLOBAL_RECORD, did, table, id, fname, stamp, "Data");
						
						this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, stamp, "Retired", retired);
					}
					else {						
						if (auditDisabled)
							this.conn.kill(DB_GLOBAL_RECORD, did, table, id, fname, stamp, "Retired");
						
						this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, stamp, "Data", newValue);
					}
					
					// add tags if any - ok even if retired
					if (StringUtil.isNotEmpty(tags))
						this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, stamp, "Tags", tags);
					else if (auditDisabled)
						this.conn.kill(DB_GLOBAL_RECORD, did, table, id, fname, stamp, "Tags");
						
					// don't bother with the indexes if not configured
					// or if there is a newer value for this field already set
					if (!schema.isIndexed() || hasNewer || effectivelyEqual)
						continue;
					
					if (oldIsSet && !oldIsRetired) {
						if (oldValue instanceof String)
							oldValue = oldValue.toString().trim().toLowerCase(Locale.ROOT);
						
						// decrement index count for the old value
						// remove the old index value
						this.conn.dec(DB_GLOBAL_INDEX, did, table, fname, oldValue);
						this.conn.kill(DB_GLOBAL_INDEX, did, table, fname, oldValue, id);
					}
					
					if (!retired) {
						if (newValue instanceof String)
							newValue = newValue.toString().trim().toLowerCase(Locale.ROOT);
						
						// increment index count
						// set the new index new
						this.conn.inc(DB_GLOBAL_INDEX, did, table, fname, newValue);
						this.conn.set(DB_GLOBAL_INDEX, did, table, fname, newValue, id, null);
					}
					
					continue;
				}
				
				// --------------------------------------
				//
				// Handling for other types
				//
				// StaticList handling
				//   fields([field name],sid,"Data",0) = [value]
				//   fields([field name],sid,"Tags") = [value]			|value1|value2|etc...
				//   fields([field name],sid,"Retired") = [value]			|value1|value2|etc...
				//
				// DynamicScalar handling
				//   fields([field name],sid,"Data",0) = [value]
				//   fields([field name],sid,"From") = [value]			null means always was
				//   fields([field name],sid,"Tags") = [value]
				//   fields([field name],sid,"Retired") = [value]			|value1|value2|etc...
				//
				// DynamicList handling
				//   fields([field name],sid,"Data",0) = [value]
				//   fields([field name],sid,"From") = [value]			null means always was
				//   fields([field name],sid,"To") = [value]				null means always will be
				//   fields([field name],sid,"Tags") = [value]
				//   fields([field name],sid,"Retired") = [value]			|value1|value2|etc...
				// --------------------------------------
				
				for (FieldStruct subid : ((RecordStruct) field.getValue()).getFields()) {
					String sid = subid.getName();
					
					RecordStruct data = (RecordStruct) subid.getValue();
					
					Object newValue = Struct.objectToCore(data.getField("Data"));
					String tags = data.getFieldAsString("Tags");
					boolean retired = data.getFieldAsBooleanOrFalse("Retired");
					boolean updateOnly = data.getFieldAsBooleanOrFalse("UpdateOnly");
					
					BigDateTime from = data.getFieldAsBigDateTime("From");
					BigDateTime to = data.getFieldAsBigDateTime("To");
					
					// find the first, newest, stamp 
					byte[] newerStamp = this.conn.nextPeerKey(DB_GLOBAL_RECORD, did, table, id, fname, sid, null);
					
					boolean hasNewer = false;
					
					if (newerStamp != null) {
						BigDecimal newStamp = Struct.objectToDecimal(ByteUtil.extractValue(newerStamp));
						hasNewer = stamp.compareTo(newStamp) > 0;  // if we come after newer then we are older info, there is newer
					}
					
					// find the next, older, stamp after current
					byte[] olderStamp = this.conn.nextPeerKey(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp);
					BigDecimal oldStamp = null;
					boolean oldIsSet = false;
					boolean oldIsRetired = false;
					Object oldValue = null;
					
					if (olderStamp != null) {
						oldStamp = Struct.objectToDecimal(ByteUtil.extractValue(olderStamp));
						
						// try to get the data if any - note retired fields have no data
						if (oldStamp != null) {
							oldIsRetired = this.conn.getAsBooleanOrFalse(DB_GLOBAL_RECORD, did, table, id, fname, sid, oldStamp, "Retired");
							oldIsSet = this.conn.isSet(DB_GLOBAL_RECORD, did, table, id, fname, sid, oldStamp, "Data");
							
							if (oldIsSet) 
								oldValue = this.conn.get(DB_GLOBAL_RECORD, did, table, id, fname, sid, oldStamp, "Data");
						}
					}
					
					boolean effectivelyEqual = (retired && oldIsRetired) || ((oldValue == null) && (newValue == null)) || ((oldValue != null) && oldValue.equals(newValue));
					
					if (updateOnly && effectivelyEqual) 
						// TODO for dynamic scalar (only) look at previous value (different sid) and skip if that has same value
						continue;
					
					// set either retired or data, not both
					if (retired) {
						// if we are retiring then get rid of old value
						if (auditDisabled && oldIsSet)
							this.conn.kill(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "Data");
						
						this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "Retired", retired);
					}
					else {
						// if we are not retiring then get rid of old Retired just in case it was set before
						if (auditDisabled)
							this.conn.kill(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "Retired");
						
						this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "Data", newValue);
					}
					
					// add tags if any - ok even if retired
					if (StringUtil.isNotEmpty(tags))
						this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "Tags", tags);
					else if (auditDisabled)
						this.conn.kill(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "Tags");
					
					if (from != null)
						this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "From", from);
					else if (auditDisabled)
						this.conn.kill(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "From");
					
					if (to != null)
						this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "To", to);
					else if (auditDisabled)
						this.conn.kill(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "To");
					
					// don't bother with the indexes if not configured
					// or if there is a newer value for this field already set
					if (!schema.isIndexed() || hasNewer || effectivelyEqual)
						continue;
					
					if (oldIsSet && !oldIsRetired) { 
						if (oldValue instanceof String)
							oldValue = oldValue.toString().trim().toLowerCase(Locale.ROOT);
						
						// decrement index count for the old value
						// remove the old index value
						this.conn.dec(DB_GLOBAL_INDEX_SUB, did, table, fname, oldValue);
						this.conn.kill(DB_GLOBAL_INDEX_SUB, did, table, fname, oldValue, id, sid);
					}
					
					if (!retired) {
						if (newValue instanceof String)
							newValue = newValue.toString().trim().toLowerCase(Locale.ROOT);
						
						String range = null;
						
						if (from != null)
							range = from.toString();
						
						if (to != null) {
							if (range == null)
								range = ":" + to.toString();
							else
								range += ":" + to.toString();
						}
						
						// increment index count
						// set the new index new
						this.conn.inc(DB_GLOBAL_INDEX_SUB, did, table, fname, newValue);
						this.conn.set(DB_GLOBAL_INDEX_SUB, did, table, fname, newValue, id, sid, range);
					}
					
					continue;
				}
			}
			catch (Exception x) {
				or.error("Error updating field: " + fname);
			}
		}
		
		return or;
	}
	
	public OperationResult setStaticScalar(String table, String id, String field, Object data) {
		RecordStruct fields = new RecordStruct(
				new FieldStruct(field, new RecordStruct(
						new FieldStruct("Data", data)
				))
		);
		
		return this.setFields(table, id, fields);
	}
	
	public OperationResult setStaticList(String table, String id, String field, String subid, Object data) {
		RecordStruct fields = new RecordStruct(
				new FieldStruct(field, new RecordStruct(
						new FieldStruct(subid, 
								new RecordStruct(new FieldStruct("Data", data))
						)
				))
		);
		
		return this.setFields(table, id, fields);
	}
	
	// from is ms since 1970
	public OperationResult setDynamicScalar(String table, String id, String field, String subid, BigDateTime from, Object data) {
		RecordStruct fields = new RecordStruct(
				new FieldStruct(field, new RecordStruct(
						new FieldStruct(subid, 
								new RecordStruct(
										new FieldStruct("Data", data),
										new FieldStruct("From", from)
								)
						)
				))
		);
		
		return this.setFields(table, id, fields);
	}
	
	// from and to are ms since 1970
	public OperationResult setDynamicList(String table, String id, String field, String subid, BigDateTime from, BigDateTime to, Object data) {
		RecordStruct fields = new RecordStruct(
				new FieldStruct(field, new RecordStruct(
						new FieldStruct(subid, 
								new RecordStruct(
										new FieldStruct("Data", data),
										new FieldStruct("From", from),
										new FieldStruct("To", to)
								)
						)
				))
		);
		
		return this.setFields(table, id, fields);
	}
	
	/* 
	public void rebiuldIndex(String table) {
	 ;
	 ; don't run when Java connector is active
	indexTable(table) n field
	 f  s field=$o(^dcSchema($p(table,"#"),"Fields",field)) q:field=""  d indexField(table,field)
	 quit
	 ;
	 ; don't run when Java connector is active
	indexField(table,field) n val,id,stamp,sid,fschema
	 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
	 ;
	 m fschema=^dcSchema($p(table,"#"),"Fields",field)
	 ;
	 quit:'fschema("Indexed") 
	 ;
	 i 'fschema("List")&'fschema("Dynamic") k ^dcIndex1(table,field)
	 e  k ^dcIndex2(table,field)
	 ;
	 f  s id=$o(^dcRecord(table,id)) q:id=""  d
	 . i 'fschema("List")&'fschema("Dynamic") d  q
	 . . s stamp=$o(^dcRecord(table,id,field,""),-1) q:stamp=""   
	 . . s val=^dcRecord(table,id,field,stamp,"Data",0)
	 . . s val=$$val2Ndx(val)    
	 . . ;
	 . . ; don't index null
	 . . i val="" q
	 . . ;
	 . . s ^dcIndex1(table,field,val,id)=1
	 . . s ^dcIndex1(table,field,val)=^dcIndex1(table,field,val)+1
	 . ;
	 . f  s sid=$o(^dcRecord(table,id,field,sid),-1) q:sid=""  d
	 . . s stamp=$o(^dcRecord(table,id,field,sid,""),-1) q:stamp="" 	
	 . . ;
	 . . s val=^dcRecord(table,id,field,sid,stamp,"Data",0)
	 . . s val=$$val2Ndx(val)    
	 . . ;
	 . . ; don't index null
	 . . i val="" q
	 . . ;
	 . . s ^dcIndex2(table,field,val,id,sid)=1
	 . . s ^dcIndex2(table,field,val)=^dcIndex2(table,field,val)+1
	 ;
	 quit
	 ;
	}	
	 */
	
	public boolean isDeleted(String table, String id) {
		try {
			return this.conn.getAsBooleanOrFalse(DB_GLOBAL_RECORD, task.getDomain(), table, id, "Deleted");
		} 
		catch (DatabaseException x) {
			// TODO logger
		}
		
		return false;
	}
	
	public boolean isRetired(String table, String id) {
		try {
			if (!this.conn.hasAny(DB_GLOBAL_RECORD, task.getDomain(), table, id))
				return true;
			
			if (this.conn.getAsBooleanOrFalse(DB_GLOBAL_RECORD, task.getDomain(), table, id, "Deleted"))
				return true;
			
			if (Struct.objectToBooleanOrFalse(this.getStaticScalar(table, id, "Retired")))
					return true;
		} 
		catch (DatabaseException x) {
			// TODO logger
		}
		
		return false;
	}
	
	/*
	 ; check not only retired, but if this record was active during the period of time
	 ; indicated by "when".  If a record has no From then it is considered to be 
	 ; active indefinitely in the past, prior to To.  If there is no To then record
	 ; is active current and since From.
	 */
	public boolean isCurrent(String table, String id, BigDateTime when, boolean historical) {
		if (this.isRetired(table, id))
			return false;
		
		if (when == null)
			return true;
		
		if (!historical) {
			BigDateTime to = Struct.objectToBigDateTime(this.getStaticScalar(table, id, "To"));
			
			// when must come before to
			if ((to != null) && (when.compareTo(to) != -1))
				return false;
		}
		
		BigDateTime from = Struct.objectToBigDateTime(this.getStaticScalar(table, id, "From"));
		
		// when must come after - or at - from
		if ((from != null) && (when.compareTo(from) >= 0))
			return false;
		
		return true;
	}
	
	public Object getStaticScalar(String table, String id, String field) {
		return this.getStaticScalar(table, id, field, null);
	}
	
	public Object getStaticScalar(String table, String id, String field, String format) {
		// checks the Retired flag 
		BigDecimal stamp = this.getStaticScalarStamp(table, id, field);
		
		if (stamp == null)
			return null;
		
		try {
			Object val = this.conn.get(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, stamp, "Data");
			
			// TODO format
			
			return val;
		}
		catch (Exception x) {
			OperationContext.get().error("getStaticScalar error: " + x);
		}
		
		return null;
	}
	
	public byte[] getStaticScalarRaw(String table, String id, String field) {
		// checks the Retired flag 
		BigDecimal stamp = this.getStaticScalarStamp(table, id, field);
		
		if (stamp == null)
			return null;
		
		try {
			return this.conn.getRaw(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, stamp, "Data");
		}
		catch (Exception x) {
			OperationContext.get().error("getStaticScalar error: " + x);
		}
		
		return null;
	}
	
	public RecordStruct getStaticScalarExtended(String table, String id, String field, String format) {
		BigDecimal stamp = this.getStaticScalarStamp(table, id, field);
		
		if (stamp == null)
			return null;
		
		try {
			RecordStruct ret = new RecordStruct();
			
			// TODO format data
			
			ret.setField("Data", this.conn.get(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, stamp, "Data"));
			ret.setField("Tags", this.conn.get(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, stamp, "Tags"));
			ret.setField("Retired", this.conn.get(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, stamp, "Retired"));
			
			return ret;
		}
		catch (Exception x) {
			OperationContext.get().error("getStaticScalar error: " + x);
		}
		
		return null;
	}
	
	public BigDecimal getStaticScalarStamp(String table, String id, String field) {
		try {
			byte[] olderStamp = this.conn.getOrNextPeerKey(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, this.task.getStamp());
			
			if (olderStamp == null) 
				return null;
			
			BigDecimal oldStamp = Struct.objectToDecimal(ByteUtil.extractValue(olderStamp));
			
			if (this.conn.getAsBooleanOrFalse(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, oldStamp, "Retired"))
				return null;
			
			return oldStamp;
		}
		catch (Exception x) {
			OperationContext.get().error("getStaticScalar error: " + x);
		}
		
		return null;
	}
	
	public Object getStaticList(String table, String id, String field, String subid) {
		return this.getStaticList(table, id, field, subid, null);
	}
	
	public Object getStaticList(String table, String id, String field, String subid, String format) {
		// checks the Retired flag 
		BigDecimal stamp = this.getListStamp(table, id, field, subid, null);
		
		if (stamp == null)
			return null;
		
		try {
			Object val = this.conn.get(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, subid, stamp, "Data");
			
			// TODO format
			
			return val;
		}
		catch (Exception x) {
			OperationContext.get().error("getStaticList error: " + x);
		}
		
		return null;
	}
	
	public byte[] getStaticListRaw(String table, String id, String field, String subid) {
		// checks the Retired flag 
		BigDecimal stamp = this.getListStamp(table, id, field, subid, null);
		
		if (stamp == null)
			return null;
		
		try {
			return this.conn.getRaw(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, subid, stamp, "Data");
		}
		catch (Exception x) {
			OperationContext.get().error("getStaticList error: " + x);
		}
		
		return null;
	}
	
	public RecordStruct getStaticListExtended(String table, String id, String field, String subid, String format) {
		BigDecimal stamp = this.getListStamp(table, id, field, subid, null);
		
		if (stamp == null)
			return null;
		
		try {
			RecordStruct ret = new RecordStruct();
			
			// TODO format
			
			ret.setField("SubId", subid);
			ret.setField("Data", this.conn.get(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, subid, stamp, "Data"));
			ret.setField("Tags", this.conn.get(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, subid, stamp, "Tags"));
			ret.setField("Retired", this.conn.get(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, subid, stamp, "Retired"));
			
			return ret;
		}
		catch (Exception x) {
			OperationContext.get().error("getStaticList error: " + x);
		}
		
		return null;
	}
	
	public List<String> getStaticListKeys(String table, String id, String field) {
		List<String> ret = new ArrayList<>();
		
		try {
			byte[] subid = this.conn.nextPeerKey(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, null);
			
			while (subid != null) {
				Object sid = ByteUtil.extractValue(subid);
				
				ret.add(Struct.objectToString(sid));
				
				subid = this.conn.nextPeerKey(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, sid);
			}
		}
		catch (Exception x) {
			OperationContext.get().error("getStaticList error: " + x);
		}
		
		return ret;
	}
	
	public Object getDynamicScalar(String table, String id, String field, BigDateTime when) {
		return this.getDynamicScalar(table, id, field, when, null, false);
	}
	
	public Object getDynamicScalar(String table, String id, String field, BigDateTime when, String format, boolean historical) {
		String subid = this.getDynamicScalarSubId(table, id, field, when, historical);
		
		if (StringUtil.isEmpty(subid))
			return null;
		
		// checks the Retired flag 
		BigDecimal stamp = this.getListStamp(table, id, field, subid, when);
		
		if (stamp == null)
			return null;
		
		try {
			Object val = this.conn.get(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, subid, stamp, "Data");
			
			// TODO format
			
			return val;
		}
		catch (Exception x) {
			OperationContext.get().error("getDynamicScalar error: " + x);
		}
		
		return null;
	}
	
	public byte[] getDynamicScalarRaw(String table, String id, String field, BigDateTime when, boolean historical) {
		String subid = this.getDynamicScalarSubId(table, id, field, when, historical);
		
		if (StringUtil.isEmpty(subid))
			return null;
		
		// checks the Retired flag 
		BigDecimal stamp = this.getListStamp(table, id, field, subid, when);
		
		if (stamp == null)
			return null;
		
		try {
			return this.conn.getRaw(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, subid, stamp, "Data");
		}
		catch (Exception x) {
			OperationContext.get().error("getDynamicScalar error: " + x);
		}
		
		return null;
	}
	
	public RecordStruct getDynamicScalarExtended(String table, String id, String field, BigDateTime when, String format, boolean historical) {
		String subid = this.getDynamicScalarSubId(table, id, field, when, historical);
		
		if (StringUtil.isEmpty(subid))
			return null;
		
		BigDecimal stamp = this.getListStamp(table, id, field, subid, when);
		
		if (stamp == null)
			return null;
		
		try {
			RecordStruct ret = new RecordStruct();
			
			ret.setField("SubId", subid);
			ret.setField("Data", this.conn.get(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, subid, stamp, "Data"));
			ret.setField("Tags", this.conn.get(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, subid, stamp, "Tags"));
			ret.setField("Retired", this.conn.get(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, subid, stamp, "Retired"));
			ret.setField("From", this.conn.get(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, subid, stamp, "From"));
			
			return ret;
		}
		catch (Exception x) {
			OperationContext.get().error("getDynamicScalar error: " + x);
		}
		
		return null;
	}
	
	public String getDynamicScalarSubId(String table, String id, String field, BigDateTime when, boolean historical) {
		if (when == null)
			when = new BigDateTime();
		
		if (!historical) {
			BigDateTime to = Struct.objectToBigDateTime(this.getStaticScalar(table, id, "To"));
			
			// when must come before to
			if ((to != null) && (when.compareTo(to) != -1))
				return null;
		}
		
		BigDateTime matchWhen = null;
		String matchSid = null;
		
		try {
			byte[] subid = this.conn.nextPeerKey(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, null);
			
			while (subid != null) {
				Object sid = ByteUtil.extractValue(subid);
				
				byte[] stmp = this.conn.getOrNextPeerKey(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, sid, this.task.getStamp());
				
				if (stmp != null) {
					Object stamp = ByteUtil.extractValue(stmp);
					
					if (!this.conn.getAsBooleanOrFalse(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, sid, stamp, "Retired")) {
						BigDateTime from = this.conn.getAsBigDateTime(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, sid, stamp, "From");
						
						if (from == null) 
							from = Struct.objectToBigDateTime(this.getStaticScalar(table, id, "From"));
						
						if ((from == null) && (matchWhen == null))
							matchSid = Struct.objectToString(sid);
						
						// if `from` is before or at `when` and if `from` is greater than a previous match 
						else if ((from != null) && (from.compareTo(when) <= 0)) {
							if ((matchWhen == null) || (from.compareTo(matchWhen) > 0)) {
								matchWhen = from;
								matchSid = Struct.objectToString(sid);
							}
						}
					}					
				}
				
				subid = this.conn.nextPeerKey(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, sid);
			}
		}
		catch (Exception x) {
			OperationContext.get().error("getDynamicScalar error: " + x);
		}
		
		return matchSid;
	}
	
	public List<String> getDynamicScalarKeys(String table, String id, String field) {
		List<String> ret = new ArrayList<>();
		
		try {
			byte[] subid = this.conn.nextPeerKey(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, null);
			
			while (subid != null) {
				Object sid = ByteUtil.extractValue(subid);
				
				ret.add(Struct.objectToString(sid));
				
				subid = this.conn.nextPeerKey(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, sid);
			}
		}
		catch (Exception x) {
			OperationContext.get().error("getDynamicScalar error: " + x);
		}
		
		return ret;
	}
	
	public Object getDynamicList(String table, String id, String field, String subid, BigDateTime when) {
		return this.getDynamicList(table, id, field, subid, when, null);
	}
	
	public Object getDynamicList(String table, String id, String field, String subid, BigDateTime when, String format) {
		// checks the Retired flag 
		BigDecimal stamp = this.getListStamp(table, id, field, subid, when);
		
		if (stamp == null)
			return null;
		
		try {
			Object val = this.conn.get(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, subid, stamp, "Data");
			
			// TODO format
			
			return val;
		}
		catch (Exception x) {
			OperationContext.get().error("getDynamicList error: " + x);
		}
		
		return null;
	}
	
	public byte[] getDynamicListRaw(String table, String id, String field, String subid, BigDateTime when) {
		// checks the Retired flag 
		BigDecimal stamp = this.getListStamp(table, id, field, subid, when);
		
		if (stamp == null)
			return null;
		
		try {
			return this.conn.getRaw(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, subid, stamp, "Data");
		}
		catch (Exception x) {
			OperationContext.get().error("getDynamicList error: " + x);
		}
		
		return null;
	}
	
	public RecordStruct getDynamicListExtended(String table, String id, String field, String subid, BigDateTime when, String format) {
		BigDecimal stamp = this.getListStamp(table, id, field, subid, when);
		
		if (stamp == null)
			return null;
		
		try {
			RecordStruct ret = new RecordStruct();
			
			ret.setField("SubId", subid);
			ret.setField("Data", this.conn.get(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, subid, stamp, "Data"));
			ret.setField("Tags", this.conn.get(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, subid, stamp, "Tags"));
			ret.setField("Retired", this.conn.get(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, subid, stamp, "Retired"));
			ret.setField("From", this.conn.get(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, subid, stamp, "From"));
			ret.setField("To", this.conn.get(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, subid, stamp, "To"));
			
			return ret;
		}
		catch (Exception x) {
			OperationContext.get().error("getDynamicList error: " + x);
		}
		
		return null;
	}
		
	public BigDecimal getListStamp(String table, String id, String field, String subid, BigDateTime when) {
		try {
			byte[] olderStamp = this.conn.getOrNextPeerKey(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, subid, this.task.getStamp());
			
			if (olderStamp == null) 
				return null;
			
			BigDecimal oldStamp = Struct.objectToDecimal(ByteUtil.extractValue(olderStamp));
			
			if (this.conn.getAsBooleanOrFalse(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, subid, oldStamp, "Retired"))
				return null;
			
			if (when == null)
				return oldStamp;
			
			BigDateTime to = this.conn.getAsBigDateTime(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, subid, oldStamp, "To");
			
			if (to == null) 
				to = Struct.objectToBigDateTime(this.getStaticScalar(table, id, "To"));
			
			// if `to` is before or at `when` then bad
			if ((to != null) && (to.compareTo(when) <= 0))
				return null;
			
			BigDateTime from = this.conn.getAsBigDateTime(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, subid, oldStamp, "From");
			
			if (from == null) 
				from = Struct.objectToBigDateTime(this.getStaticScalar(table, id, "From"));
			
			if (from == null) 
				return oldStamp;
			
			// if `from` is before or at `when` then good
			if (from.compareTo(when) <= 0)
				return oldStamp;
		}
		catch (Exception x) {
			OperationContext.get().error("getDynamicList error: " + x);
		}
		
		return null;
	}
	
	public List<String> getDynamicListKeys(String table, String id, String field) {
		List<String> ret = new ArrayList<>();
		
		try {
			byte[] subid = this.conn.nextPeerKey(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, null);
			
			while (subid != null) {
				Object sid = ByteUtil.extractValue(subid);
				
				ret.add(Struct.objectToString(sid));
				
				subid = this.conn.nextPeerKey(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, sid);
			}
		}
		catch (Exception x) {
			OperationContext.get().error("getDynamicList error: " + x);
		}
		
		return ret;
	}
	
	public Object get(String table, String id, String field, BigDateTime when) {
		return this.get(table, id, field, when, null, false);
	}
	
	public List<Object> get(String table, String id, String field, BigDateTime when, String format, boolean historical) {
		List<Object> ret = new ArrayList<>();
		
		DbField schema = this.task.getSchema().getDbField(table, field);
		
		if (schema == null)
			return ret;
		
		if (!schema.isList() && !schema.isDynamic()) {
			ret.add(this.getStaticScalar(table, id, field, format));
			return ret;
		}
		
		if (!schema.isList() && schema.isDynamic()) {
			ret.add(this.getDynamicScalar(table, id, field, when, format, historical));
			return ret;
		}
		
		try {
			byte[] subid = this.conn.nextPeerKey(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, null);
			
			while (subid != null) {
				Object sid = ByteUtil.extractValue(subid);
				
				if (schema.isList() && !schema.isDynamic())
					ret.add(this.getStaticList(table, id, field, Struct.objectToString(sid), format));
				else
					ret.add(this.getDynamicList(table, id, field, Struct.objectToString(sid), when, format));
				
				subid = this.conn.nextPeerKey(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, sid);
			}
		}
		catch (Exception x) {
			OperationContext.get().error("getDynamicList error: " + x);
		}
		
		return ret;
	}
	
	public List<Object> getExtended(String table, String id, String field, BigDateTime when, String format, boolean historical) {
		List<Object> ret = new ArrayList<>();
		
		DbField schema = this.task.getSchema().getDbField(table, field);
		
		if (schema == null)
			return ret;
		
		if (!schema.isList() && !schema.isDynamic()) {
			ret.add(this.getStaticScalarExtended(table, id, field, format));
			return ret;
		}
		
		if (!schema.isList() && schema.isDynamic()) {
			ret.add(this.getDynamicScalarExtended(table, id, field, when, format, historical));
			return ret;
		}
		
		try {
			byte[] subid = this.conn.nextPeerKey(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, null);
			
			while (subid != null) {
				Object sid = ByteUtil.extractValue(subid);
				
				if (schema.isList() && !schema.isDynamic())
					ret.add(this.getStaticListExtended(table, id, field, Struct.objectToString(sid), format));
				else
					ret.add(this.getDynamicListExtended(table, id, field, Struct.objectToString(sid), when, format));
				
				subid = this.conn.nextPeerKey(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, sid);
			}
		}
		catch (Exception x) {
			OperationContext.get().error("getDynamicList error: " + x);
		}
		
		return ret;
	}
	
	// subid null for all
	public List<byte[]> getRaw(String table, String id, String field, String subid, BigDateTime when, boolean historical) {
		List<byte[]> ret = new ArrayList<>();
		
		DbField schema = this.task.getSchema().getDbField(table, field);
		
		if (schema == null)
			return ret;
		
		if (!schema.isList() && !schema.isDynamic()) {
			ret.add(this.getStaticScalarRaw(table, id, field));
			return ret;
		}
		
		if (!schema.isList() && schema.isDynamic()) {
			ret.add(this.getDynamicScalarRaw(table, id, field, when, historical));
			return ret;
		}
		
		if (subid != null) {
			if (schema.isList() && !schema.isDynamic())
				ret.add(this.getStaticListRaw(table, id, field, subid));
			else
				ret.add(this.getDynamicListRaw(table, id, field, subid, when));	// TODO check if this returns null sometimes, not what we want right?
		}
		else {
			try {
				byte[] bsubid = this.conn.nextPeerKey(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, null);
				
				while (bsubid != null) {
					Object sid = ByteUtil.extractValue(bsubid);
					
					if (schema.isList() && !schema.isDynamic())
						ret.add(this.getStaticListRaw(table, id, field, Struct.objectToString(sid)));
					else
						ret.add(this.getDynamicListRaw(table, id, field, Struct.objectToString(sid), when));	// TODO check if this returns null sometimes, not what we want right?
					
					bsubid = this.conn.nextPeerKey(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, sid);
				}
			}
			catch (Exception x) {
				OperationContext.get().error("getDynamicList error: " + x);
			}
		}
		
		return ret;
	}
	
	/* TODO something like this...
	 ; check to see if value is current with the given when+historical settings
	has(value,table,id,field,when,format,historical) i (table="")!(id="")!(field="") quit 0
	 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
	 ;
	 n fschema m fschema=^dcSchema($p(table,"#"),"Fields",field)
	 i 'fschema("List")&'fschema("Dynamic") quit ($$get1(table,id,field,format)=value)
	 i 'fschema("List")&fschema("Dynamic") quit ($$get3(table,id,field,when,format,historical)=value)
	 ;
	 n sid,fnd
	 f  s sid=$o(^dcRecord(table,id,field,sid)) q:sid=""  d  q:fnd
	 . s:($$get4(table,id,field,sid,when,format)=value) fnd=1
	 ;
	 quit fnd
	 ;
*/
	
	public Object formatField(String table, String fname, Object value, String format) {
		if ("Tr".equals(format)) {
			// TODO translate $$tr^dcStrUtil("_enum_"_table_"_"_field_"_"_val)
		}
		
		// TODO format date/time to chrono
		
		// TODO format numbers to locale
		
		// TODO split? pad? custom format function?
		
		return value;
	}
	
	public boolean checkSelect(String table, String id, BigDateTime when, RecordStruct where, boolean historical) {
		if (!this.isCurrent(table, id, when, historical))
			return false;
		
		if (where == null)
			return true;
		
		boolean ret = false;
		
		String expression = where.getFieldAsString("Expression");		// expression name - "And", "Or", "Equal", etc.  
		
		if (StringUtil.isEmpty(expression))
			return ret;
		
		/*
		 ;
		 ; Complex: And | Or | Not
		 ; Simple:  Equal | NotEqual | In | Any | Filter | Is | IsNot
		 ;			LessThan | GreaterThan | LessThanOrEqual | GreaterThanOrEqual | Between
		 ;			StartsWith | Contains 
		*/
		
		if ("And".equals(expression)) {
			ListStruct children = where.getFieldAsList("Children");
			
			if (children != null) {
				for (Struct s : children.getItems()) {
					RecordStruct child = (RecordStruct) s;
					
					ret = this.checkSelect(table, id, when, child, historical);
					
					if (!ret)
						break;
				}
			}
			
			return ret;
		}
		
		if ("Or".equals(expression)) {
			ListStruct children = where.getFieldAsList("Children");
			
			if (children != null) {
				for (Struct s : children.getItems()) {
					RecordStruct child = (RecordStruct) s;
					
					ret = this.checkSelect(table, id, when, child, historical);
					
					if (ret)
						break;
				}
			}
			
			return ret;
		}
		
		// Not is only allowed 1 child
		if ("Not".equals(expression)) {
			ListStruct children = where.getFieldAsList("Children");
			
			if (children != null) {
				RecordStruct child = children.getItemAsRecord(0);
				
				ret = !this.checkSelect(table, id, when, child, historical);
			}
			
			return ret;
		}
		
		if ("Filter".equals(expression)) {
			/*
 . s filter=where("Filter")
 . i (filter'="")&(^dcProg("wherefilter",filter)'="") x "s res=$$"_^dcProg("wherefilter",filter)_"()"
			 */
			
			return ret;
		}
		
		String[] parts = new String[] { "A", "B", "C" };
		List<List<byte[]>> values = new ArrayList<List<byte[]>>();
		
		for (int i = 0; i < parts.length; i++) {
			values.add(null);	// start with null
			
			RecordStruct pdef = where.getFieldAsRecord(parts[i]);
			
			if (pdef == null)
				continue;
			
			String pfname = pdef.getFieldAsString("Field");
			String subid = pdef.getFieldAsString("SubId");
			// TODO String pformat = pdef.getFieldAsString("Format");
			// add support for Format, this converts from byte to object, then formats object, then back to byte for compares
			
			if (StringUtil.isNotEmpty(pfname)) {
				if ("Id".equals(pfname)) {
					ArrayList<byte[]> vl = new ArrayList<>();
					vl.add(ByteUtil.buildValue(id));
					values.set(i, vl);
				}
				else
					values.set(i, this.getRaw(table, id, pfname, subid, when, historical));
				
				continue;
			}
			
			String comp = pdef.getFieldAsString("Composer");
			
			if (StringUtil.isNotEmpty(comp)) {
				values.set(i, null);		// TODO composer
				continue;
			}
			
			Object val = pdef.getField("Value");
			
			if (val != null) {
				ArrayList<byte[]> vl = new ArrayList<>();
				
				if (val instanceof ListStruct) {
					for (int i2 = 0; i2 < ((ListStruct)val).getSize(); i2++) 
						vl.add(ByteUtil.buildValue(((ListStruct)val).getItem(i2)));
				}
				else {
					vl.add(ByteUtil.buildValue(val));
				}
				
				values.set(i, vl);
			}
		}
		
		List<byte[]> a = values.get(0);
		List<byte[]> b = values.get(1);
		List<byte[]> c = values.get(2);
		
		if ("Equal".equals(expression)) {
			if ((a == null) && (b == null))
				return true;
			
			// rule out one being null 
			if ((a == null) || (b == null))
				return false;
			
			for (int i = 0; i < Math.max(a.size(), b.size()); i++) {
				if (i >= a.size())					
					return false;
				
				if (i >= b.size())
					return false;
				
				byte[] a1 = a.get(i);
				byte[] b1 = b.get(i);
				
				if ((a1 == null) && (b1 == null))
					return true;
				
				// rule out one being null 
				if ((a1 == null) || (b1 == null))
					return false;				
				
				if (ByteUtil.compareKeys(a1, b1) != 0)
					return false;
			}
			
			return true;
		}
		
		if ("NotEqual".equals(expression)) {
			if ((a == null) || (b == null))
				return false;
			
			// rule out one being null 
			if (a != b)
				return true;
			
			for (int i = 0; i < Math.max(a.size(), b.size()); i++) {
				if (i >= a.size())					
					return true;
				
				if (i >= b.size())
					return true;
				
				if (ByteUtil.compareKeys(a.get(i), b.get(i)) == 0)
					return false;
			}
			
			return true;
		}
		
		if ("Is".equals(expression)) {
			if (a == null)
				return false;
			
			// TODO enhance for more than null?
			for (int i = 0; i < a.size(); i++) {
				if (ByteUtil.compareKeys(a.get(i), Constants.DB_EMPTY_ARRAY) == 0)
					return false;
			}
			
			return true;
		}
		
		if ("IsNot".equals(expression)) {
			if (a == null)
				return true;
			
			// TODO enhance for more than null?
			for (int i = 0; i < a.size(); i++) {
				if (ByteUtil.compareKeys(a.get(i), Constants.DB_EMPTY_ARRAY) == 0)
					return true;
			}
			
			return false;
		}
		
		if ("LessThan".equals(expression)) {
			if ((a == null) && (b == null))
				return false;
			
			if (a == null)
				return true;
			
			if (b == null)
				return false;
			
			int max = Math.max(a.size(), b.size());
			
			for (int i = 0; i < max; i++) {
				if (i >= a.size())					
					return true;
				
				if (i >= b.size())
					return false;
				
				int comp = ByteUtil.compareKeys(a.get(i), b.get(i));
				
				if ((comp == 0) && (i < max -1))
					continue;
				
				// first compare that is smaller wins
				if (comp < 0)
					return true;
				
				// first compare that is larger loses
				if (comp > 0)
					return false;
			}
			
			// must be equal here
			return false;
		}
		
		if ("GreaterThan".equals(expression)) {
			if ((a == null) && (b == null))
				return false;
			
			if (a == null)
				return false;
			
			if (b == null)
				return true;
			
			int max = Math.max(a.size(), b.size());
			
			for (int i = 0; i < max; i++) {
				if (i >= a.size())					
					return false;
				
				if (i >= b.size())
					return true;
				
				int comp = ByteUtil.compareKeys(a.get(i), b.get(i));
				
				// first compare that is smaller loses
				if (comp < 0)
					return false;
				
				// first compare that is larger wins
				if (comp > 0)
					return true;
			}
			
			// we must be equal here
			return false;
		}
		
		if ("LessThanOrEqual".equals(expression)) {
			if ((a == null) && (b == null))
				return true;
			
			if (a == null)
				return true;
			
			if (b == null)
				return false;
			
			int max = Math.max(a.size(), b.size());
			
			for (int i = 0; i < max; i++) {
				if (i >= a.size())					
					return true;
				
				if (i >= b.size())
					return false;
				
				int comp = ByteUtil.compareKeys(a.get(i), b.get(i));
				
				// first compare that is smaller wins
				if (comp < 0)
					return true;
				
				// first compare that is larger loses
				if (comp > 0)
					return false;
			}
			
			return true;
		}
		
		if ("GreaterThanOrEqual".equals(expression)) {
			if ((a == null) && (b == null))
				return true;
			
			if (a == null)
				return false;
			
			if (b == null)
				return true;
			
			int max = Math.max(a.size(), b.size());
			
			for (int i = 0; i < max; i++) {
				if (i >= a.size())					
					return false;
				
				if (i >= b.size())
					return true;
				
				int comp = ByteUtil.compareKeys(a.get(i), b.get(i));
				
				// first compare that is smaller loses
				if (comp < 0)
					return false;
				
				// first compare that is larger wins
				if (comp > 0)
					return true;
			}
			
			return true;
		}
		
		if ("Between".equals(expression)) {
			if ((a == null) && (b == null))
				return true;
			
			if (a == null)
				return false;
			
			if (b != null) {
				// check the greater than or equal condition first
				int max = Math.max(a.size(), b.size());
				
				for (int i = 0; i < max; i++) {
					if (i >= a.size())					
						return false;
					
					if (i >= b.size())
						break;
					
					int comp = ByteUtil.compareKeys(a.get(i), b.get(i));
					
					// first compare that is smaller loses
					if (comp < 0)
						return false;
					
					// first compare that is larger wins
					if (comp > 0)
						break;
				}
			}			
			
			// now check the less than condition
			if (c == null)
				return false;
			
			int max = Math.max(a.size(), c.size());
			
			for (int i = 0; i < max; i++) {
				if (i >= a.size())					
					return true;
				
				if (i >= c.size())
					return false;
				
				int comp = ByteUtil.compareKeys(a.get(i), c.get(i));
				
				// first compare that is smaller wins
				if (comp < 0)
					return true;
				
				// first compare that is larger loses
				if (comp > 0)
					return false;
			}
			
			return true;
		}
		
		if ("Any".equals(expression)) {
			if ((a == null) && (b == null))
				return true;
			
			// rule out one being null 
			if ((a == null) || (b == null))
				return false;
			
			for (int i = 0; i < a.size(); i++) {
				for (int i2 = 0; i2 < b.size(); i2++) {
					if (ByteUtil.compareKeys(a.get(i), b.get(i2)) == 0)
						return true;
				}
			}
			
			return false;
		}
		
		/* reconsider TODO
		if ("In".equals(expression)) {
			/*
 i name="In" d  quit res
 . f  s a=$o(aa(a)) q:a=""  d  q:res
 . . s x="|"_aa(a)_"|"
 . . i bb(0)[x s res=1
			 * /
			
			return ret;
		}
		*/
		
		if ("StartsWith".equals(expression)) {
			// b is only allowed one value here
			if ((a == null) || (b == null) || (b.size() != 1))
				return false;
			
			Object bv = ByteUtil.extractValue(b.get(0));
			
			if (bv == null) 
				return false;
			
			String bs = bv.toString();
			
			for (int i = 0; i < a.size(); i++) {
				Object av = ByteUtil.extractValue(a.get(i));
				
				if (av == null) 
					return false;
				
				if (!av.toString().startsWith(bs))
					return false;
			}
			
			return true;
		}
		
		if ("EndsWith".equals(expression)) {
			// b is only allowed one value here
			if ((a == null) || (b == null) || (b.size() != 1))
				return false;
			
			Object bv = ByteUtil.extractValue(b.get(0));
			
			if (bv == null) 
				return false;
			
			String bs = bv.toString();
			
			for (int i = 0; i < a.size(); i++) {
				Object av = ByteUtil.extractValue(a.get(i));
				
				if (av == null) 
					return false;
				
				if (!av.toString().endsWith(bs))
					return false;
			}
			
			return true;
		}
		
		if ("Contains".equals(expression)) {
			// b is only allowed one value here
			if ((a == null) || (b == null) || (b.size() != 1))
				return false;
			
			Object bv = ByteUtil.extractValue(b.get(0));
			
			if (bv == null) 
				return false;
			
			String bs = bv.toString();
			
			for (int i = 0; i < a.size(); i++) {
				Object av = ByteUtil.extractValue(a.get(i));
				
				if (av == null) 
					return false;
				
				if (!av.toString().contains(bs))
					return false;
			}
			
			return true;
		}
		
		return ret;
	}

	public void traverseSubIds(String table, String id, String fname, BigDateTime when, boolean historical, Consumer<Object> out) {
		String did = this.task.getDomain();
		
		try {
			byte[] subid = this.conn.nextPeerKey(DB_GLOBAL_RECORD, did, table, id, fname, null);
			
			while (subid != null) {
				Object sid = ByteUtil.extractValue(subid);
				
				// if stamp is null it means Retired
				if (this.getListStamp(table, id, fname, sid.toString(), when) != null)
					out.accept(sid);
				
				subid = this.conn.nextPeerKey(DB_GLOBAL_RECORD, did, table, id, fname, sid);
			}
		}
		catch (Exception x) {
			OperationContext.get().error("getDynamicList error: " + x);
		}
	}

	public void traverseRecords(String table, BigDateTime when, boolean historical, Consumer<Object> out) {
		String did = this.task.getDomain();
		
		try {
			byte[] id = this.conn.nextPeerKey(DB_GLOBAL_RECORD, did, table, null);
			
			while (id != null) {
				Object oid = ByteUtil.extractValue(id);
				
				if (this.isCurrent(table, oid.toString(), when, historical)) 
					out.accept(oid);
				
				id = this.conn.nextPeerKey(DB_GLOBAL_RECORD, did, table, oid);
			}
		}
		catch (Exception x) {
			OperationContext.get().error("getDynamicList error: " + x);
		}
	}
	
	public void traverseIndex(String table, String fname, Object val, BigDateTime when, boolean historical, Consumer<Object> out) {
		this.traverseIndex(table, fname, val, null, when, historical, out);
	}
	
	public void traverseIndex(String table, String fname, Object val, String subid, BigDateTime when, boolean historical, Consumer<Object> out) {
		String did = this.task.getDomain();
		
		DbField ffdef = this.task.getSchema().getDbField(table, fname);
		
		if (ffdef == null)
			return;
		
		if (val instanceof String)
			val = val.toString().trim().toLowerCase(Locale.ROOT);
		
		try {
			byte[] recid = conn.nextPeerKey(ffdef.getIndexName(), did, table, fname, val, null);
			
			while (recid != null) {
				Object rid = ByteUtil.extractValue(recid);

				if (this.isCurrent(table, rid.toString(), when, historical)) {
					if (ffdef.isStaticScalar()) {
						out.accept(rid);
					}
					else {
						byte[] recsid = conn.nextPeerKey(DB_GLOBAL_INDEX_SUB, did, table, fname, val, rid, null);
						
						while (recsid != null) {
							Object rsid = ByteUtil.extractValue(recsid);
							
							if ((subid == null) || subid.equals(rsid)) {							
								String range = conn.getAsString(DB_GLOBAL_INDEX_SUB, did, table, fname, val, rid, rsid);
								
								if (StringUtil.isEmpty(range) || (when == null)) {
									out.accept(rid);
								}
								else {
									int pos = range.indexOf(':');
									
									BigDateTime from = null;
									BigDateTime to = null;
									
									if (pos == -1) {
										from = BigDateTime.parseOrNull(range);
									}
									else if (pos == 0) {
										to = BigDateTime.parseOrNull(range.substring(1));
									}
									else {
										from = BigDateTime.parseOrNull(range.substring(0, pos));
										to = BigDateTime.parseOrNull(range.substring(pos + 1));
									}
									
									if (((from == null) || (when.compareTo(from) >= 0)) && ((to == null) || (when.compareTo(to) < 0))) 
										out.accept(rid);
								}
							}
							
							recsid = conn.nextPeerKey(DB_GLOBAL_INDEX_SUB, did, table, fname, val, rid, rsid);
						}
					}
				}
				
				recid = conn.nextPeerKey(ffdef.getIndexName(), did, table, fname, val, rid);
			}
		}
		catch (Exception x) {
			OperationContext.get().error("traverseIndex error: " + x);
		}
	}
	
	public Object firstInIndex(String table, String fname, Object val, BigDateTime when, boolean historical) {
		String did = this.task.getDomain();
		
		DbField ffdef = this.task.getSchema().getDbField(table, fname);
		
		if (ffdef == null)
			return null;
		
		if (val instanceof String)
			val = val.toString().trim().toLowerCase(Locale.ROOT);
		
		try {
			byte[] recid = conn.nextPeerKey(ffdef.getIndexName(), did, table, fname, val, null);
			
			while (recid != null) {
				Object rid = ByteUtil.extractValue(recid);
				
				if (this.isCurrent(table, rid.toString(), when, historical)) { 
	
					if (ffdef.isStaticScalar()) 
						return rid;

					byte[] recsid = conn.nextPeerKey(DB_GLOBAL_INDEX_SUB, did, table, fname, val, rid, null);
					
					while (recsid != null) {
						Object rsid = ByteUtil.extractValue(recsid);
						
						String range = conn.getAsString(DB_GLOBAL_INDEX_SUB, did, table, fname, val, rid, rsid);
						
						if (StringUtil.isEmpty(range) || (when == null))
							return rid;
						
						int pos = range.indexOf(':');
						
						BigDateTime from = null;
						BigDateTime to = null;
						
						if (pos == -1) {
							from = BigDateTime.parseOrNull(range);
						}
						else if (pos == 0) {
							to = BigDateTime.parseOrNull(range.substring(1));
						}
						else {
							from = BigDateTime.parseOrNull(range.substring(0, pos));
							to = BigDateTime.parseOrNull(range.substring(pos + 1));
						}
						
						if (((from == null) || (when.compareTo(from) >= 0)) && ((to == null) || (when.compareTo(to) < 0))) 
							return rid;
						
						recsid = conn.nextPeerKey(DB_GLOBAL_INDEX_SUB, did, table, fname, val, rsid);
					}
				}
				
				recid = conn.nextPeerKey(ffdef.getIndexName(), did, table, fname, val, rid);
			}
		}
		catch (Exception x) {
			OperationContext.get().error("traverseIndex error: " + x);
		}
		
		return null;
	}	
	
	// traverse the values
	public void traverseIndexValRange(String table, String fname, Object fromval, Object toval, BigDateTime when, boolean historical, Consumer<Object> out) {
		String did = this.task.getDomain();
		
		DbField ffdef = this.task.getSchema().getDbField(table, fname);
		
		if (ffdef == null)
			return;
		
		if (fromval instanceof String)
			fromval = fromval.toString().trim().toLowerCase(Locale.ROOT);
		
		if (toval instanceof String)
			toval = toval.toString().trim().toLowerCase(Locale.ROOT);
		
		try {
			byte[] valb = conn.getOrNextPeerKey(ffdef.getIndexName(), did, table, fname, fromval);
			byte[] valfin = (toval != null) ? ByteUtil.buildKey(toval) : null;
			
			while (valb != null) {
				// check if past "To"
				if ((valfin != null) && (ByteUtil.compareKeys(valb, valfin) >= 0))
					break;
				
				Object val = ByteUtil.extractValue(valb);

				out.accept(val);
				
				valb = conn.nextPeerKey(ffdef.getIndexName(), did, table, fname, val);
			}
		}
		catch (Exception x) {
			OperationContext.get().error("traverseIndex error: " + x);
		}
	}	
	
	// traverse the record ids
	public void traverseIndexRange(String table, String fname, Object fromval, Object toval, BigDateTime when, boolean historical, Consumer<Object> out) {
		String did = this.task.getDomain();
		
		DbField ffdef = this.task.getSchema().getDbField(table, fname);
		
		if (ffdef == null)
			return;
		
		if (fromval instanceof String)
			fromval = fromval.toString().trim().toLowerCase(Locale.ROOT);
		
		if (toval instanceof String)
			toval = toval.toString().trim().toLowerCase(Locale.ROOT);
		
		try {
			byte[] valb = conn.getOrNextPeerKey(ffdef.getIndexName(), did, table, fname, fromval);
			byte[] valfin = (toval != null) ? ByteUtil.buildKey(toval) : null;
			
			while (valb != null) {
				// check if past "To"
				if ((valfin != null) && (ByteUtil.compareKeys(valb, valfin) >= 0))
					break;
				
				Object val = ByteUtil.extractValue(valb);

				byte[] recid = conn.nextPeerKey(ffdef.getIndexName(), did, table, fname, val, null);
				
				while (recid != null) {
					Object rid = ByteUtil.extractValue(recid);

					if (this.isCurrent(table, rid.toString(), when, historical)) {
						if (ffdef.isStaticScalar()) {
							out.accept(rid);
						}
						else {
							byte[] recsid = conn.nextPeerKey(DB_GLOBAL_INDEX_SUB, did, table, fname, val, rid, null);
							
							while (recsid != null) {
								Object rsid = ByteUtil.extractValue(recsid);
								
								String range = conn.getAsString(DB_GLOBAL_INDEX_SUB, did, table, fname, val, rid, rsid);
								
								if (StringUtil.isEmpty(range) || (when == null)) {
									out.accept(rid);
								}
								else {
									int pos = range.indexOf(':');
									
									BigDateTime from = null;
									BigDateTime to = null;
									
									if (pos == -1) {
										from = BigDateTime.parseOrNull(range);
									}
									else if (pos == 0) {
										to = BigDateTime.parseOrNull(range.substring(1));
									}
									else {
										from = BigDateTime.parseOrNull(range.substring(0, pos));
										to = BigDateTime.parseOrNull(range.substring(pos + 1));
									}
									
									if (((from == null) || (when.compareTo(from) >= 0)) && ((to == null) || (when.compareTo(to) < 0))) 
										out.accept(rid);
								}
								
								recsid = conn.nextPeerKey(DB_GLOBAL_INDEX_SUB, did, table, fname, val, rsid);
							}
						}
					}
					
					recid = conn.nextPeerKey(ffdef.getIndexName(), did, table, fname, val, rid);
				}
				
				valb = conn.nextPeerKey(ffdef.getIndexName(), did, table, fname, val);
			}
		}
		catch (Exception x) {
			OperationContext.get().error("traverseIndex error: " + x);
		}
	}	
	
	public OperationResult executeTrigger(String table, String op, DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		OperationResult or = new OperationResult();
		
		List<DbTrigger> trigs = this.task.getSchema().getDbTriggers(table, op);
		
		for (DbTrigger trig : trigs) {
			String spname = trig.execute;
			
			try {
				Class<?> spclass = Class.forName(spname);				
				IStoredProc sp = (IStoredProc) spclass.newInstance();
				sp.execute(conn, task, log);
			} 
			catch (Exception x) {
				or.error("Unable to load/start tigger class: " + x);
			}
		}
		
		return or;
	}
	
	/*
 ;	TODO improve so source can be table or script
 
 ; Params("Sources",[table name],"Title")=[field name]
 ; Params("Sources",[table name],"Body")=[field name]
 ; Params("Sources",[table name],"Extras",[field name])=1  
 ;
 		TODO wrap up to above sids are part of a source
 ; Params("AllowedSids",[table name],[field name],[sid])=1					- if field name not present then assume all
 ;
 ; Params("RequiredWords",[word],"Term")=1
 ;                              ,"Exact")=[exact word for final match in Original]
 ;
 ; Params("AllowedWords",[word],"Term")=1
 ;                              ,"Exact")=[exact word for final match in Original]
 ;
 ; Params("ProhibitedWords",[word],"Term")=1
 ;                              ,"Exact")=[exact word for final match in Original]
 ;
 ; returns top results first, returns only 1000 
 ;   and no more than 100 or so per field...that is all one should ever need
 ;
 ; RETURN = [
 ;		{
 ;			Table: N,
 ;			Id: N,
 ;			Score: N,
 ;			TitlePositions: [ N, n ],		// relative to Title, where 0 is first character
 ;			Title: SSSS
 ;			BodyPositions: [ N, n ],		// relative to Body, where 0 is first character
 ;			Body: SSSS
 ;		}
 ; ]
 ;
srchTxt n params2,table,field,ret,match,score,id,ttitle,tbody,tab,sid,word,plist,i
 ;
 ; srchTxt2 is the heart of the searching, this routine just formats those results
 ; so we need to prep some parameters for srchTxt2
 ;
 m params2("RequiredWords")=Params("RequiredWords")
 m params2("AllowedWords")=Params("AllowedWords")
 m params2("ProhibitedWords")=Params("ProhibitedWords")
 m params2("AllowedSids")=Params("AllowedSids")
 ;
 ; convert sources to the srchTxt2 structure
 ;
 f  s table=$o(Params("Sources",table))  q:table=""  d
 . s ttitle=Params("Sources",table,"Title")
 . s:ttitle'="" params2("Sources",table,ttitle)=1
 . ;
 . s tbody=Params("Sources",table,"Body")
 . s:tbody'="" params2("Sources",table,tbody)=1
 . ;
 . k field  f  s field=$o(Params("Sources",table,"Extras",field))  q:field=""  d
 . . s params2("Sources",table,field)=1
 ;
 ; collect search results
 ;
 d srchTxt2(.params2,.ret)
 ;
 ; return the results
 ;
 w StartList
 ;
 f  s score=$o(ret(score),-1)  q:score=""  d
 . f  s table=$o(ret(score,table))  q:table=""  d
 . . s tab=$p(table,"#",1)
 . . f  s id=$o(ret(score,table,id))  q:id=""  d
 . . . w StartRec
 . . . w Field_"Table"_ScalarStr_tab
 . . . w Field_"Id"_ScalarStr_id
 . . . w Field_"Score"_ScalarInt_score
 . . . ;
 . . . s ttitle=Params("Sources",tab,"Title")
 . . . s tbody=Params("Sources",tab,"Body")
 . . . ;
 . . . w Field_"Title"_ScalarStr
 . . . s sid=$o(^dcTextRecord(table,id,ttitle,""))
 . . . ;
 . . . i sid'="" d
 . . . . w ^dcTextRecord(table,id,ttitle,sid,"Original",0)		; titles are no more than one line
 . . . . ;
 . . . . i $d(ret(score,table,id,ttitle,sid))>0  d
 . . . . . w Field_"TitlePositions"_StartList
 . . . . . ;
 . . . . . f  s word=$o(ret(score,table,id,ttitle,sid,word))  q:word=""  d
 . . . . . . s plist=ret(score,table,id,ttitle,sid,word)
 . . . . . . f i=1:1:$l(plist,",") w ScalarInt_$p(plist,",",i)
 . . . . . ;
 . . . . . w EndList
 . . . . ;
 . . . s tbody=Params("Sources",tab,"Body")
 . . . ;
 . . . w Field_"Body"_ScalarStr
 . . . k sid  f  s sid=$o(^dcTextRecord(table,id,tbody,sid))  q:sid=""  d
 . . . . ;
 . . . . ;i $d(ret(score,table,id,tbody,sid))>0  d  q
 . . . . ;. ; TODO find the positions and cut out parts
 . . . . ;
 . . . . ; if we get here then we are just writing the top 30 words
 . . . . s sentence=^dcTextRecord(table,id,tbody,sid,"Original",0)
 . . . . k wcnt  f i=1:1:$l(sentence) q:wcnt=30  i $e(sentence,i)=" " s wcnt=wcnt+1
 . . . . w $e(sentence,1,i-1)
 . . . ;
 . . . w EndRec
 ;
 w EndList
 ;
 quit
 ;
 ;
 ; Params("Sources",[table name],[field name])=1  
 ;
 ; Params("AllowedSids",[table name],[field name],[sid])=1					- if field name not present then assume all
 ;				TODO AllowedSids not yet coded!!
 ; Params("RequiredWords",[word],"Term")=1
 ;                              ,"Exact")=[exact word for final match in Original]
 ;
 ; Params("AllowedWords",[word],"Term")=1
 ;                              ,"Exact")=[exact word for final match in Original]
 ;
 ; Params("ProhibitedWords",[word],"Term")=1
 ;                              ,"Exact")=[exact word for final match in Original]
 ;
 ; ret
 ;		ret(score,table,id,field,sid,word)=[comma list of positions]
 ;
srchTxt2(params,ret) n score,table,field,id,sid,pos,word,sources,find,fnd,sscore,tabled
 n exact,lnum,matches,fmatch,ismatch,word2,entry,term,nxtscore,collect
 ;
 ; create one list of all words we are searching for
 ;
 f  s word=$o(params("RequiredWords",word))  q:word=""  s find(word)=1,lnum=lnum+1
 f  s word=$o(params("AllowedWords",word))  q:word=""  s find(word)=1,lnum=lnum+1
 ;
 s lnum=$s(lnum>5:20,lnum>3:50,1:100)    ; limit how many partial matches we look at if we have many words
 ;
 ; prime the sources array - we want the top scoring word for each table and field
 ; we'll then use this array to figure the top score of all.  as we loop the sources
 ; we'll keep adding more top matches so we find the next top scoring
 ;
 f  s table=$o(params("Sources",table))  q:table=""  d
 . i (table'["#")&(Domain'="") s tabled=table_"#"_Domain     ; support table instances
 . e  s tabled=table
 . ;
 . f  s field=$o(params("Sources",table,field))  q:field=""  d
 . . f  s word=$o(find(word))  q:word=""  d
 . . . s score=$o(^dcTextIndex(tabled,field,word,""),-1)  
 . . . i score'="" s sources(score,word,tabled,field)=1  						; sources will get filled out further down
 . . . ;
 . . . i (params("RequiredWords",word,"Exact")'="")!(params("AllowedWords",word,"Exact")'="") q
 . . . k matches 
 . . . s word2=word
 . . . f  s word2=$o(^dcTextIndex(tabled,field,word2))  q:word2=""  d  q:matches>(lnum-1) 
 . . . . i $f(word2,word)'=($l(word)+1) s matches=lnum q	; if not starting with the original word then stop looking
 . . . . s score=$o(^dcTextIndex(tabled,field,word2,""),-1)  
 . . . . i score'="" s sources(score,word2,tabled,field)=1,matches=matches+1
 ;
 ; find our top scoring fields/words and then use the text index to find possible
 ; record matches.
 ;
 k score,matches
 f  s score=$o(sources(score),-1)  q:score=""  d  q:matches>999
 . f  s word=$o(sources(score,word))  q:word=""  d
 . . f  s table=$o(sources(score,word,table))  q:table=""  d
 . . . k field
 . . . f  s field=$o(sources(score,word,table,field))  q:field=""!(fmatch(table,field)>99)  d
 . . . . k id
 . . . . f  s id=$o(^dcTextIndex(table,field,word,score,id))  q:id=""!(fmatch(table,field)>99)  d
 . . . . . k sid
 . . . . . f  s sid=$o(^dcTextIndex(table,field,word,score,id,sid))  q:sid=""!(fmatch(table,field)>99)  d
 . . . . . . ; check exact matches - if a required or allowed word is to have an exact match
 . . . . . . ;
 . . . . . . k ismatch
 . . . . . . ;
 . . . . . . i params("RequiredWords",word) d  i 1
 . . . . . . . s exact=params("RequiredWords",word,"Exact")  i exact="" s ismatch=1 q
 . . . . . . . k lnum  f  s lnum=$o(^dcTextRecord(table,id,field,sid,"Original",lnum))  q:lnum=""  d  q:ismatch
 . . . . . . . . i ^dcTextRecord(table,id,field,sid,"Original",lnum)[exact s ismatch=1 
 . . . . . . ; 
 . . . . . . e  i params("AllowedWords",word) d  i 1
 . . . . . . . s exact=params("AllowedWords",word,"Exact")  i exact="" s ismatch=1 q
 . . . . . . . k lnum  f  s lnum=$o(^dcTextRecord(table,id,field,sid,"Original",lnum))  q:lnum=""  d  q:ismatch
 . . . . . . . . i ^dcTextRecord(table,id,field,sid,"Original",lnum)[exact s ismatch=1 
 . . . . . . ;
 . . . . . . e  s ismatch=1
 . . . . . . ;
 . . . . . . q:'ismatch
 . . . . . . ;
 . . . . . . ; check prohibited - see if a prohibited word is in a match
 . . . . . . ; 
 . . . . . . k word2  f  s word2=$o(params("ProhibitedWords",word2))  q:word2=""  d  q:'ismatch
 . . . . . . . s exact=params("ProhibitedWords",word2,"Exact")  
 . . . . . . . s entry=$s(exact="":"Analyzed",1:"Original"),term=$s(exact="":"|"_word2_":",1:word2)
 . . . . . . . k lnum  f  s lnum=$o(^dcTextRecord(table,id,field,sid,entry,lnum))  q:lnum=""  d  q:'ismatch
 . . . . . . . . i ^dcTextRecord(table,id,field,sid,term,lnum)[term s ismatch=0 
 . . . . . . ;
 . . . . . . q:'ismatch
 . . . . . . ;
 . . . . . . s collect(table,id,field,sid,word)=score		; collect contains the values we need for ordering our results
 . . . . . . ;
 . . . . . . s fmatch(table,field)=fmatch(table,field)+1,matches=matches+1
 . . . . ; 
 . . . . s nxtscore=$o(^dcTextIndex(table,field,word,score),-1)  q:nxtscore=""
 . . . . s sources(nxtscore,word,table,field)=1									; filling out sources 
 ;
 ; build return value - we now have enough words, just want to put them in the right order
 ;
 k table,field,id,sid,word,matches
 ;
 f  s table=$o(collect(table))  q:table=""  d  q:matches>249
 . f  s id=$o(collect(table,id))  q:id=""  d  q:matches>249
 . . s ismatch=1
 . . ;
 . . ; ensure all required are present - unlike prohibited, which we can check above,
 . . ; we have to check required across all potential fields for a given record
 . . ; 
 . . k word2  f  s word2=$o(params("RequiredWords",word2))  q:word2=""  d  q:'ismatch
 . . . q:word=word2  ; already checked
 . . . s exact=params("RequiredWords",word2,"Exact"),fnd=0  
 . . . s entry=$s(exact="":"Analyzed",1:"Original"),term=$s(exact="":"|"_word2_":",1:word2)
 . . . ;
 . . . ; check all fields/sids
 . . . ; 
 . . . k field  f  s field=$o(params("Sources",$p(table,"#",1),field))  q:field=""  d  q:fnd
 . . . . k sid  f  s sid=$o(^dcTextRecord(table,id,field,sid))  q:sid=""  d  q:fnd
 . . . . . k lnum  f  s lnum=$o(^dcTextRecord(table,id,field,sid,entry,lnum))  q:lnum=""  d  q:fnd
 . . . . . . i ^dcTextRecord(table,id,field,sid,entry,lnum)[term s fnd=1 
 . . . ;
 . . . s:'fnd ismatch=0
 . . ;
 . . q:'ismatch 
 . . ;
 . . ; compute score for the record
 . . s score=0
 . . ;
 . . k field  f  s field=$o(collect(table,id,field))  q:field=""  d  
 . . . k sid  f  s sid=$o(collect(table,id,field,sid))  q:sid=""  d
 . . . . k word  f  s word=$o(collect(table,id,field,sid,word))  q:word=""  d
 . . . . . s sscore=collect(table,id,field,sid,word)
 . . . . . ;
 . . . . . ; bonus if the word we are scoring matches one of the original words
 . . . . . i params("AllowedWords",word)!params("RequiredWords",word) d
 . . . . . . s lnum=collect(table,id,field,sid,word)
 . . . . . . s sscore=sscore+($l(^dcTextIndex(table,field,word,lnum,id,sid),",")*2)           ; bonus for each word occurance
 . . . . . ;
 . . . . . s score=score+sscore
 . . ;
 . . ; we now have the score for the record
 . . ;
 . . k field  f  s field=$o(collect(table,id,field))  q:field=""  d   
 . . . k sid  f  s sid=$o(collect(table,id,field,sid))  q:sid=""  d
 . . . . k word  f  s word=$o(collect(table,id,field,sid,word))  q:word=""  d
 . . . . . s lnum=collect(table,id,field,sid,word)
 . . . . . s ret(score,table,id,field,sid,word)=^dcTextIndex(table,field,word,lnum,id,sid)
 . . . ;
 . . s matches=matches+1    ; record matches, not word matches
 ;
 quit
 ;
 ;
 	 * 
	 */

	public void rebuildIndexes() {
		this.rebuildIndexes(Hub.instance.getDomainInfo(this.task.getDomain()), BigDateTime.nowDateTime());
	}

	public void rebuildIndexes(DomainInfo di, BigDateTime when) {
		try {
			for (DbTable tbl : di.getSchema().getDbTables()) {
				this.rebuildTableIndex(di, tbl.getName(), when);
			}
			
			/*
			byte[] traw = this.conn.nextPeerKey(DB_GLOBAL_RECORD, di.getId(), null);
			
			while (traw != null) {
				Object table = ByteUtil.extractValue(traw);
				
				this.rebuildTableIndex(di, table.toString(), when);
				
				traw = this.conn.nextPeerKey(DB_GLOBAL_RECORD, di.getId(), table);
			}
			*/
		}
		catch (Exception x) {
			OperationContext.get().error("rebuildDomainIndexes error: " + x);
		}
		finally {
			task.popDomain();
		}
	}

	public void rebuildTableIndex(String table) {
		this.rebuildTableIndex(Hub.instance.getDomainInfo(this.task.getDomain()), table, BigDateTime.nowDateTime());
	}
	
	public void rebuildTableIndex(DomainInfo di, String table, BigDateTime when) {
		try {
			// kill the indexes
			this.conn.kill(DB_GLOBAL_INDEX_SUB, di.getId(), table);			
			this.conn.kill(DB_GLOBAL_INDEX, di.getId(), table);
			
			// see if there is even such a table in the schema
			//DomainInfo di = this.dm.getDomainInfo(did);
			
			if (!di.getSchema().hasTable(table)) {
				System.out.println("Skipping table, not known by this domain: " + table);
			}
			else {
				System.out.println("Indexing table: " + table);
				
				this.traverseRecords(table, when, false, new Consumer<Object>() {
					@Override
					public void accept(Object id) {
						for (DbField schema : di.getSchema().getDbFields(table)) {
							if (!schema.isIndexed())
								continue;
							
							String did = di.getId();
							
							try {
								// --------------------------------------
								// StaticScalar handling 
								// --------------------------------------
								if (!schema.isList() && !schema.isDynamic()) {
									
									// find the first, newest, stamp 
									byte[] nstamp = TablesAdapter.this.conn.nextPeerKey(DB_GLOBAL_RECORD, did, table, id, schema.getName(), null);
									
									if (nstamp == null)
										continue;
									
									BigDecimal stamp = Struct.objectToDecimal(ByteUtil.extractValue(nstamp));
									
									if (stamp == null)
										continue;
									
									if (TablesAdapter.this.conn.getAsBooleanOrFalse(DB_GLOBAL_RECORD, did, table, id, schema.getName(), stamp, "Retired"))
										continue;
									
									if (!TablesAdapter.this.conn.isSet(DB_GLOBAL_RECORD, did, table, id, schema.getName(), stamp, "Data"))
										continue;
										
									Object value = TablesAdapter.this.conn.get(DB_GLOBAL_RECORD, did, table, id, schema.getName(), stamp, "Data");
									
									if (value instanceof String)
										value = value.toString().toLowerCase(Locale.ROOT);
								
									// increment index count
									// set the new index new
									TablesAdapter.this.conn.inc(DB_GLOBAL_INDEX, did, table, schema.getName(), value);
									TablesAdapter.this.conn.set(DB_GLOBAL_INDEX, did, table, schema.getName(), value, id, null);
								}				
								else {
									TablesAdapter.this.traverseSubIds(table, id.toString(), schema.getName(), when, false, new Consumer<Object>() {
										@Override
										public void accept(Object sid) {
											try {
												// find the first, newest, stamp 
												byte[] nstamp = TablesAdapter.this.conn.nextPeerKey(DB_GLOBAL_RECORD, did, table, id, schema.getName(), sid, null);
												
												if (nstamp == null)
													return;
												
												BigDecimal stamp = Struct.objectToDecimal(ByteUtil.extractValue(nstamp));
												
												if (stamp == null)
													return;
												
												if (TablesAdapter.this.conn.getAsBooleanOrFalse(DB_GLOBAL_RECORD, did, table, id, schema.getName(), sid, stamp, "Retired"))
													return;
												
												if (!TablesAdapter.this.conn.isSet(DB_GLOBAL_RECORD, did, table, id, schema.getName(), sid, stamp, "Data"))
													return;
														
												Object value = TablesAdapter.this.conn.get(DB_GLOBAL_RECORD, did, table, id, schema.getName(), sid, stamp, "Data");
												Object from = TablesAdapter.this.conn.get(DB_GLOBAL_RECORD, did, table, id, schema.getName(), sid, stamp, "From");
												Object to = TablesAdapter.this.conn.get(DB_GLOBAL_RECORD, did, table, id, schema.getName(), sid, stamp, "To");
												
												if (value instanceof String)
													value = value.toString().toLowerCase(Locale.ROOT);
												
												String range = null;
												
												if (from != null)
													range = from.toString();
												
												if (to != null) {
													if (range == null)
														range = ":" + to.toString();
													else
														range += ":" + to.toString();
												}
												
												// increment index count
												// set the new index new
												TablesAdapter.this.conn.inc(DB_GLOBAL_INDEX_SUB, did, table, schema.getName(), value);
												TablesAdapter.this.conn.set(DB_GLOBAL_INDEX_SUB, did, table, schema.getName(), value, id, sid, range);
											}
											catch (Exception x) {
												System.out.println("Error indexing table: " + table + " - " + schema.getName() + " - " + id + " - " + sid + ": " + x);
											}
										}
									});									
								}
							}
							catch (Exception x) {
								System.out.println("Error indexing table: " + table + " - " + schema.getName() + " - " + id + ": " + x);
							}
						}
					}
				});
			}
		} 
		catch (DatabaseException x) {
			System.out.println("Error indexing table: " + table + ": " + x);
		}
	}		
}
