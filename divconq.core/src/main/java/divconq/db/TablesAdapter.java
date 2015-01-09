package divconq.db;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import divconq.db.util.ByteUtil;
import divconq.hub.Hub;
import divconq.lang.BigDateTime;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.schema.DbField;
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
	
	public OperationResult checkFields(String table, RecordStruct fields) {
		OperationResult or = new OperationResult();
		
		// TODO
		
		return or;
	}
	
	public OperationResult checkSetFields(String table, String id, RecordStruct fields) {
		OperationResult cor = this.checkFields(table, fields);
		
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
				DbField schema = Hub.instance.getSchema().getDb().getField(table, fname);
				
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
					
					// --------------------------------------
					// StaticScalar w/o audit handling 
					// --------------------------------------
					
					// if audit mode is off then stamp is always the same (zero) for all updates
					if (auditDisabled) {
						// check if we have a value currently for this field
						boolean oldIsSet = this.conn.isSet(DB_GLOBAL_RECORD, did, table, id, fname, stamp, "Data");					
						Object oldValue = this.conn.get(DB_GLOBAL_RECORD, did, table, id, fname, stamp, "Data");
						
						if (retired) {
							// if we are retiring then get rid of old value
							if (oldIsSet)
								this.conn.kill(DB_GLOBAL_RECORD, did, table, id, fname, stamp, "Data");
							
							this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, stamp, "Retired", retired);
						}
						else {
							// if we are not retiring then get rid of old Retired just in case it was set before
							this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, stamp, "Data", newValue);
							this.conn.kill(DB_GLOBAL_RECORD, did, table, id, fname, stamp, "Retired");
						}
						
						// retiring or not, tags can be associated - set to new, if any, or be sure we remove old
						if (StringUtil.isNotEmpty(tags))
							this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, stamp, "Tags", tags);
						else
							this.conn.kill(DB_GLOBAL_RECORD, did, table, id, fname, stamp, "Tags");
						
						// don't bother with the indexes if not configured
						if (!schema.isIndexed())
							continue;
						
						// check if value is already in index (same as oldValue), if so skip any more 
						if (oldIsSet && this.conn.isSet(DB_GLOBAL_INDEX_1, did, table, fname, newValue, id, stamp))
							continue;
	
						if (oldIsSet) { 
							// decrement index count for the old value
							this.conn.dec(DB_GLOBAL_INDEX_1, did, table, fname, oldValue);
	
							// remove the old index value
							this.conn.kill(DB_GLOBAL_INDEX_1, did, table, fname, oldValue, id, stamp);
						}
	
						// increment index count
						this.conn.inc(DB_GLOBAL_INDEX_1, did, table, fname, newValue);					
	
						// set the new index new
						this.conn.set(DB_GLOBAL_INDEX_1, did, table, fname, newValue, id, stamp, null);
						
						continue;
					}
					
					// --------------------------------------
					// StaticScalar w/ audit handling 
					// --------------------------------------
					
					// set either retired or data, not both
					if (retired)
						this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, stamp, "Retired", retired);
					else
						this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, stamp, "Data", newValue);
					
					// add tags if any - ok even if retired
					if (StringUtil.isNotEmpty(tags))
						this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, stamp, "Tags", tags);
					
					// don't bother with the indexes if not configured
					if (!schema.isIndexed())
						continue;
					
					BigDecimal endStamp = null;
					
					// find the next, older, stamp after current
					byte[] olderStamp = this.conn.nextPeerKey(DB_GLOBAL_RECORD, did, table, id, fname, stamp);
					
					if (olderStamp != null) {
						BigDecimal oldStamp = Struct.objectToDecimal(ByteUtil.extractValue(olderStamp));
						
						// try to get the data if any - note retired fields have no data
						if (oldStamp != null) {
							boolean oldIsSet = this.conn.isSet(DB_GLOBAL_RECORD, did, table, id, fname, oldStamp, "Data");
							
							if (oldIsSet) {
								Object oldValue = this.conn.get(DB_GLOBAL_RECORD, did, table, id, fname, oldStamp, "Data");
							
								// read the stamp from the old index, this will now be ours
								endStamp = this.conn.getAsDecimal(DB_GLOBAL_INDEX_1, did, table, fname, oldValue, id, oldStamp);
					
								// update the old index with current stamp
								this.conn.set(DB_GLOBAL_INDEX_1, did, table, fname, oldValue, id, oldStamp, stamp);
							}
						}
					}
					
					if (retired)
						continue;
					
					// if there was no older stamp then get the endStamp from the next newer stamp
					if (endStamp == null) {
						byte[] newerStamp = this.conn.prevPeerKey(DB_GLOBAL_RECORD, did, table, id, fname, stamp);
						
						if (newerStamp != null) 
							endStamp = Struct.objectToDecimal(ByteUtil.extractValue(newerStamp));
						
						// no need to update newer index, it doesn't point to our stamp
					}
					
					// only count an id once, so only if id is not currently set should we do increment
					if (!this.conn.hasAny(DB_GLOBAL_INDEX_1, did, table, fname, newValue, id)) 
						// increment index count
						this.conn.inc(DB_GLOBAL_INDEX_1, did, table, fname, newValue);
					
					// update the next index with end stamp - which may be null or the stamp for a future stamp
					this.conn.set(DB_GLOBAL_INDEX_1, did, table, fname, newValue, id, stamp, endStamp);
					
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
					
					BigDateTime from = data.getFieldAsBigDateTime("From");
					BigDateTime to = data.getFieldAsBigDateTime("To");
					
					// --------------------------------------
					// w/o audit handling 
					// --------------------------------------
					
				 					
					// if audit mode is off then stamp is always the same (zero) for all updates
					if (auditDisabled) {
						// check if we have a value currently for this field
						boolean oldIsSet = this.conn.isSet(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "Data");					
						Object oldValue = this.conn.get(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "Data");
						
						if (retired) {
							// if we are retiring then get rid of old value
							if (oldIsSet)
								this.conn.kill(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "Data");
							
							this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "Retired", retired);
						}
						else {
							// if we are not retiring then get rid of old Retired just in case it was set before
							this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "Data", newValue);
							this.conn.kill(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "Retired");
						}
						
						// retiring or not, tags can be associated - set to new, if any, or be sure we remove old
						if (StringUtil.isNotEmpty(tags))
							this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "Tags", tags);
						else
							this.conn.kill(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "Tags");
						
						if (from != null)
							this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "From", from);
						else
							this.conn.kill(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "From");
						
						if (to != null)
							this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "To", to);
						else
							this.conn.kill(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "To");
						
						// don't bother with the indexes if not configured
						if (!schema.isIndexed())
							continue;
						
						// check if value is already in index (same as oldValue), if so skip any more 
						if (oldIsSet && this.conn.isSet(DB_GLOBAL_INDEX_2, did, table, fname, newValue, id, sid, stamp))
							continue;
	
						if (oldIsSet) { 
							// decrement index count for the old value
							this.conn.dec(DB_GLOBAL_INDEX_2, did, table, fname, oldValue);
	
							// remove the old index value
							this.conn.kill(DB_GLOBAL_INDEX_2, did, table, fname, oldValue, id, sid, stamp);
						}
	
						// increment index count
						this.conn.inc(DB_GLOBAL_INDEX_2, did, table, fname, newValue);
	
						// set the new index new
						this.conn.set(DB_GLOBAL_INDEX_2, did, table, fname, newValue, id, sid, stamp, null);
						
						continue;
					}
					
					// --------------------------------------
					// w/ audit handling 
					// --------------------------------------
					
					// set either retired or data, not both
					if (retired)
						this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "Retired", retired);
					else
						this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "Data", newValue);
					
					// add tags if any - ok even if retired
					if (StringUtil.isNotEmpty(tags))
						this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "Tags", tags);
					
					if (from != null)
						this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "From", from);
					
					if (to != null)
						this.conn.set(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp, "To", to);
					
					// don't bother with the indexes if not configured
					if (!schema.isIndexed())
						continue;
					
					BigDecimal endStamp = null;
					
					// find the next, older, stamp after current
					byte[] olderStamp = this.conn.nextPeerKey(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp);
					
					if (olderStamp != null) {
						BigDecimal oldStamp = Struct.objectToDecimal(ByteUtil.extractValue(olderStamp));
						
						// try to get the data if any - note retired fields have no data
						if (oldStamp != null) {
							boolean oldIsSet = this.conn.isSet(DB_GLOBAL_RECORD, did, table, id, fname, sid, oldStamp, "Data");
							
							if (oldIsSet) {
								Object oldValue = this.conn.get(DB_GLOBAL_RECORD, did, table, id, fname, sid, oldStamp, "Data");
							
								// read the stamp from the old index, this will now be ours
								endStamp = this.conn.getAsDecimal(DB_GLOBAL_INDEX_2, did, table, fname, oldValue, id, sid, oldStamp);
					
								// update the old index with current stamp
								this.conn.set(DB_GLOBAL_INDEX_2, did, table, fname, oldValue, id, sid, oldStamp, stamp);
							}
						}
					}
					
					if (retired)
						continue;
					
					// if there was no older stamp then get the endStamp from the next newer stamp
					if (endStamp == null) {
						byte[] newerStamp = this.conn.prevPeerKey(DB_GLOBAL_RECORD, did, table, id, fname, sid, stamp);
						
						if (newerStamp != null) 
							endStamp = Struct.objectToDecimal(ByteUtil.extractValue(newerStamp));
						
						// no need to update newer index, it doesn't point to our stamp
					}
					
					// only count an id once, so only if id is not currently set should we do increment
					if (!this.conn.hasAny(DB_GLOBAL_INDEX_2, did, table, fname, newValue, id)) 
						// increment index count
						this.conn.inc(DB_GLOBAL_INDEX_2, did, table, fname, newValue);
					
					// update the next index with end stamp - which may be null or the stamp for a future stamp
					this.conn.set(DB_GLOBAL_INDEX_2, did, table, fname, newValue, id, sid, stamp, endStamp);
					
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
	
	public void rebiuldIndex(String table) {
/* 
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
	 */
	}	
	
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
	 ; active indefinately in the past, prior to To.  If there is no To then record
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
		
		DbField schema = Hub.instance.getSchema().getDb().getField(table, field);
		
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
		
		DbField schema = Hub.instance.getSchema().getDb().getField(table, field);
		
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
	
	public List<byte[]> getRaw(String table, String id, String field, BigDateTime when, boolean historical) {
		List<byte[]> ret = new ArrayList<>();
		
		DbField schema = Hub.instance.getSchema().getDb().getField(table, field);
		
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
		
		try {
			byte[] subid = this.conn.nextPeerKey(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, null);
			
			while (subid != null) {
				Object sid = ByteUtil.extractValue(subid);
				
				if (schema.isList() && !schema.isDynamic())
					ret.add(this.getStaticListRaw(table, id, field, Struct.objectToString(sid)));
				else
					ret.add(this.getDynamicListRaw(table, id, field, Struct.objectToString(sid), when));
				
				subid = this.conn.nextPeerKey(DB_GLOBAL_RECORD, this.task.getDomain(), table, id, field, sid);
			}
		}
		catch (Exception x) {
			OperationContext.get().error("getDynamicList error: " + x);
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
			// TODO String pformat = pdef.getFieldAsString("Format");
			// add support for Format, this converts from byte to object, then formats object, then back to byte for compares
			
			if (StringUtil.isNotEmpty(pfname)) {
				values.set(i, this.getRaw(table, id, pfname, when, historical));
				continue;
			}
			
			String comp = pdef.getFieldAsString("Composer");
			
			if (StringUtil.isNotEmpty(comp)) {
				values.set(i, null);		// TODO composer
				continue;
			}
			
			Object val = pdef.getFieldAsAny("Value");
			
			if (val != null) {
				ArrayList<byte[]> vl = new ArrayList<>();
				vl.add(ByteUtil.buildValue(val));
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
				
				if (ByteUtil.compareKeys(a.get(i), b.get(i)) != 0)
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
			
			for (int i = 0; i < Math.max(a.size(), b.size()); i++) {
				if (i >= a.size())					
					return false;
				
				if (i >= b.size())
					return false;
				
				if (ByteUtil.compareKeys(a.get(i), b.get(i)) == 0)
					return true;
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
		String did = this.task.getDomain();
		BigDecimal stamp = this.task.getStamp();
		
		DbField ffdef = Hub.instance.getSchema().getDb().getField(table, fname);
		
		try {
			if (!ffdef.dynamic && !ffdef.list) {
				byte[] foreignid = conn.nextPeerKey(DB_GLOBAL_INDEX_1, did, table, fname, val, null);
				
				while (foreignid != null) {
					Object fid = ByteUtil.extractValue(foreignid);
					
					byte[] foreignstamp = conn.getOrNextPeerKey(DB_GLOBAL_INDEX_1, did, table, fname, val, fid, stamp);
					
					if (foreignstamp != null) {
						BigDecimal fstamp = Struct.objectToDecimal(ByteUtil.extractValue(foreignstamp));
						
						BigDecimal endstamp = conn.getAsDecimal(DB_GLOBAL_INDEX_1, did, table, fname, val, fid, fstamp);

						if ((endstamp == null) || (stamp.compareTo(endstamp) < 0)) {
							if (this.isCurrent(table, fid.toString(), when, historical)) 
								out.accept(fid);
						}
					}
					
					foreignid = conn.nextPeerKey(DB_GLOBAL_INDEX_1, did, table, fname, val, fid);
				}
			}
			else {
				byte[] foreignid = conn.nextPeerKey(DB_GLOBAL_INDEX_2, did, table, fname, val, null);
				
				while (foreignid != null) {
					Object fid = ByteUtil.extractValue(foreignid);
					
					byte[] foreignsid = conn.nextPeerKey(DB_GLOBAL_INDEX_2, did, table, fname, val, fid, null);
					
					while (foreignsid != null) {
						Object fsid = ByteUtil.extractValue(foreignsid);
						
						byte[] foreignstamp = conn.getOrNextPeerKey(DB_GLOBAL_INDEX_2, did, table, fname, val, fid, fsid, stamp);
						
						if (foreignstamp != null) {
							BigDecimal fstamp = Struct.objectToDecimal(ByteUtil.extractValue(foreignstamp));
							
							BigDecimal endstamp = conn.getAsDecimal(DB_GLOBAL_INDEX_2, did, table, fname, val, fid, fsid, fstamp);

							if ((endstamp == null) || (stamp.compareTo(endstamp) < 0)) {
								if (this.isCurrent(table, fid.toString(), when, historical)) { 
									out.accept(fid);
									
									// skip this id, we got it once
									break;
								}
							}
						}
						
						foreignsid = conn.nextPeerKey(DB_GLOBAL_INDEX_2, did, table, fname, val, fsid);
					}
					
					foreignid = conn.nextPeerKey(DB_GLOBAL_INDEX_2, did, table, fname, val, fid);
				}
			}
		}
		catch (Exception x) {
			OperationContext.get().error("traverseIndex error: " + x);
		}
	}
	
	/* TODO
 ;
  ;
 
 ; get all (unique) id's associated with a given value-field pair
loopTable(table,cstate,when,historical) i (table="") quit ""
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 n fschema,id,fnd,nval
 ;
 s id=cstate("Id")
 ;
 f  d  q:fnd!(id="")
 . ; get next 
 . s id=$o(^dcRecord(table,id))
 . ; skip if none found
 . q:id=""
 . ; do not include records that are not current to 'when'
 . q:'$$isCurrent(table,id,when,historical)
 . ;
 . s fnd=1
 ;
 s cstate("Id")=id
 ;
 quit id
 ;
 ;
 ; get all (non-unique) id's associated with a range of values
loopRange(table,field,from,to,cstate,when,historical) i (table="")!(field="") quit "" 
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 n fschema,cv,id,fnd,done
 m fschema=^dcSchema($p(table,"#"),"Fields",field)
 quit:fschema("Indexed")'=1 ""		; if not indexed, cannot use
 ;
 s from=$$val2Ndx(from)
 s to=$$val2Ndx(to)
 ;
 s cv=cstate("Current")
 ;
 f  d  q:fnd!done
 . ; if id is null in state then we need to go to next value
 . i cstate("Id")="" d
 . . i (cv="")&(from'="") s cv=from
 . . e  i 'fschema("List")&'fschema("Dynamic") s cv=$o(^dcIndex1(table,field,cv))
 . . e  s cv=$o(^dcIndex2(table,field,cv))
 . i (cv="")!((to'="")&(cv]]to)) s done=1,cv="" q   ; don't go past 'to'
 . s id=$$loopIndex(table,field,cv,.cstate,when,historical)
 . i id'="" s fnd=1
 ;
 s cstate("Current")=cv
 ;
 quit id
 ;
 ;
 ; get all (non-unique) id's associated with a range of values
loopValues(table,field,values,cstate,when,historical) i (table="")!(field="") quit "" 
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 n fschema,cv,id,fnd,done
 m fschema=^dcSchema($p(table,"#"),"Fields",field)
 quit:fschema("Indexed")'=1 ""		; if not indexed, cannot use
 ;
 s cv=cstate("Current")
 ;
 f  d  q:fnd!done
 . ; if id is null in state then we need to go to next value
 . i cstate("Id")="" s cv=$o(values(cv)) 
 . i (cv="") s done=1 q   
 . s id=$$loopIndex(table,field,values(cv),.cstate,when,historical)
 . i id'="" s fnd=1
 ;
 s cstate("Current")=cv
 ;
 quit id
 ;
 ;
 	 * 
	 */
	
	public void indexText() {
		// TODO
	}
	
	/*
 ;
updateTxt n id,field,table,sid,cid,apos,entry,word,line,lp,ch,score
 s id=^dcParams(Pid,"Id"),table=^dcParams(Pid,"Table")
 i (id="")!(table="") d err^dcConn(50011) quit
 i (table'["#")&(Domain'="") s table=table_"#"_Domain     ; support table instances
 ;
 d runFilter("Update") quit:Errors  ; if any violations in filter then do not proceed
 ;
 ; we'll need storage for old stems from previous indexing.  allocate an id from the 
 ; cache global
 ;
 s cid=$$getCacheId^dcDbSelect()
 l +^dcTextRecord(table,id)
 ;
 f  s field=$o(^dcParams(Pid,"Fields",field))  q:field=""  d
 . f  s sid=$o(^dcParams(Pid,"Fields",field,sid))  q:sid=""  d  
 . . ;
 . . ; ^dcCache(cid,word)=score - first fill from existing document, then remove those in new doc - remaining need to be removed
 . . ;
 . . k entry
 . . ;
 . . f  s apos=$o(^dcTextRecord(table,id,field,sid,"Analyzed",apos))  q:apos=""  d  
 . . . s line=^dcTextRecord(table,id,field,sid,"Analyzed",apos)
 . . . f lp=1:1:$l(line) d
 . . . . s ch=$e(line,lp)
 . . . . i ch'="|" s entry=entry_ch q
 . . . . i $l(entry)=0 q
 . . . . s word=$p(entry,":",1)
 . . . . s score=$p(entry,":",2)
 . . . . s ^dcCache(cid,word)=score		; for this table, id, field, sid collect the words
 . . . . k entry
 . . ;
 . . ; do indexing and prune cache
 . . ;
 . . k entry
 . . ;
 . . f  s apos=$o(^dcParams(Pid,"Fields",field,sid,"Analyzed",apos))  q:apos=""  d  
 . . . s line=^dcParams(Pid,"Fields",field,sid,"Analyzed",apos)
 . . . f lp=1:1:$l(line) d
 . . . . s ch=$e(line,lp)
 . . . . i ch'="|" s entry=entry_ch q
 . . . . i $l(entry)=0 q
 . . . . s word=$p(entry,":",1)
 . . . . s score=$p(entry,":",2)
 . . . . s entry=$p(entry,":",3)
 . . . . ;
 . . . . k:^dcCache(cid,word)=score ^dcCache(cid,word)    ; don't need to clean this word	
 . . . . ;
 . . . . s ^dcTextIndex(table,field,word,score,id,sid)=entry
 . . . . k entry
 . . ;
 . . ; remove hanging words
 . . ;
 . . k word  f  s word=$o(^dcCache(cid,word))  q:word=""  d
 . . . k ^dcTextIndex(table,field,word,^dcCache(cid,word),id,sid)		
 . . ;
 . . d clearCache^dcDbSelect(cid)
 . . ;
 . . k ^dcTextRecord(table,id,field,sid)
 . . m ^dcTextRecord(table,id,field,sid)=^dcParams(Pid,"Fields",field,sid)
 ;
 l -^dcTextRecord(table,id)
 d killCache^dcDbSelect(cid)
 ;
 quit 
 ;
 	 * 
	 */
	
	public ListStruct searchText() {
		return null;		// TODO
	}

	public OperationResult executeTrigger(String table, String op, DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		OperationResult or = new OperationResult();
		
		List<DbTrigger> trigs = Hub.instance.getSchema().getDb().getTriggers(table, op);
		
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
 ;
 ; Params("Sources",[table name],"Title")=[field name]
 ; Params("Sources",[table name],"Body")=[field name]
 ; Params("Sources",[table name],"Extras",[field name])=1  
 ;
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
	
}
