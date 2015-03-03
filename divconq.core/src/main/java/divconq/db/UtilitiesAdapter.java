package divconq.db;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.function.Consumer;

import divconq.db.rocks.DatabaseManager;
import divconq.db.util.ByteUtil;
import divconq.hub.DomainInfo;
import divconq.hub.DomainsManager;
import divconq.lang.BigDateTime;
import divconq.lang.op.OperationContext;
import divconq.schema.DbField;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import static divconq.db.Constants.*;

public class UtilitiesAdapter {
	protected DatabaseManager db = null;
	protected DatabaseInterface conn = null;
	protected DatabaseTask task = null;
	protected DomainsManager dm = null;
	protected TablesAdapter tables = null;
	
	// don't call for general code...
	public UtilitiesAdapter(DatabaseManager db, DomainsManager dm) {
		this.db = db;
		this.dm = dm;
		this.conn = db.allocateAdapter();
		
		RecordStruct req = new RecordStruct();
		
		req.setField("Replicate", false);		// means this should replicate, where as Replicating means we are doing replication currently
		req.setField("Name", "dcRebuildIndexes");
		req.setField("Stamp", this.db.allocateStamp(0));
		req.setField("Domain", DB_GLOBAL_ROOT_DOMAIN);
		
		this.task = new DatabaseTask();
		this.task.setRequest(req);
		
		this.tables = new TablesAdapter(conn, task);
	}
	
	public void rebuildIndexes() {
		TablesAdapter ta = new TablesAdapter(conn, task); 
		BigDateTime when = BigDateTime.nowDateTime();
		
		ta.traverseSubIds("dcDomain", DB_GLOBAL_ROOT_DOMAIN, "dcDomainIndex", when, false, new Consumer<Object>() {				
			@Override
			public void accept(Object t) {
				String did = t.toString();
				
				try {
					System.out.println("Indexing domain: " + did);
					UtilitiesAdapter.this.rebuildDomainIndexes(did, when);
				}
				catch (Exception x) {
					System.out.println("dcRebuildIndexes: Unable to index: " + did);
				}
			}
		});
	}
	
	public void rebuildDomainIndexes(String did, BigDateTime when) {
		task.pushDomain(did);
		
		try {
			byte[] traw = this.conn.nextPeerKey(DB_GLOBAL_RECORD, did, null);
			
			while (traw != null) {
				Object table = ByteUtil.extractValue(traw);
				
				UtilitiesAdapter.this.rebuildTableIndex(did, table.toString(), when);
				
				traw = this.conn.nextPeerKey(DB_GLOBAL_RECORD, did, table);
			}
		}
		catch (Exception x) {
			OperationContext.get().error("rebuildDomainIndexes error: " + x);
		}
		finally {
			task.popDomain();
		}
	}
	
	public void rebuildTableIndex(String did, String table, BigDateTime when) {
		try {
			// kill the indexes
			this.conn.kill(DB_GLOBAL_INDEX_SUB, did, table);			
			this.conn.kill(DB_GLOBAL_INDEX, did, table);
			
			// see if there is even such a table in the schema
			DomainInfo di = this.dm.getDomainInfo(did);
			
			if (!di.getSchema().hasTable(table)) {
				System.out.println("Skipping table, not known by this domain: " + table);
			}
			else {
				System.out.println("Indexing table: " + table);
				
				this.tables.traverseRecords(table, when, false, new Consumer<Object>() {
					@Override
					public void accept(Object id) {
						for (DbField schema : di.getSchema().getDbFields(table)) {
							if (!schema.isIndexed())
								continue;
							
							try {
								// --------------------------------------
								// StaticScalar handling 
								// --------------------------------------
								if (!schema.isList() && !schema.isDynamic()) {
									
									// find the first, newest, stamp 
									byte[] nstamp = UtilitiesAdapter.this.conn.nextPeerKey(DB_GLOBAL_RECORD, did, table, id, schema.getName(), null);
									
									if (nstamp == null)
										continue;
									
									BigDecimal stamp = Struct.objectToDecimal(ByteUtil.extractValue(nstamp));
									
									if (stamp == null)
										continue;
									
									if (UtilitiesAdapter.this.conn.getAsBooleanOrFalse(DB_GLOBAL_RECORD, did, table, id, schema.getName(), stamp, "Retired"))
										continue;
									
									if (!UtilitiesAdapter.this.conn.isSet(DB_GLOBAL_RECORD, did, table, id, schema.getName(), stamp, "Data"))
										continue;
										
									Object value = UtilitiesAdapter.this.conn.get(DB_GLOBAL_RECORD, did, table, id, schema.getName(), stamp, "Data");
									
									if (value instanceof String)
										value = value.toString().toLowerCase(Locale.ROOT);
								
									// increment index count
									// set the new index new
									UtilitiesAdapter.this.conn.inc(DB_GLOBAL_INDEX, did, table, schema.getName(), value);
									UtilitiesAdapter.this.conn.set(DB_GLOBAL_INDEX, did, table, schema.getName(), value, id, null);
								}				
								else {
									UtilitiesAdapter.this.tables.traverseSubIds(table, id.toString(), schema.getName(), when, false, new Consumer<Object>() {
										@Override
										public void accept(Object sid) {
											try {
												// find the first, newest, stamp 
												byte[] nstamp = UtilitiesAdapter.this.conn.nextPeerKey(DB_GLOBAL_RECORD, did, table, id, schema.getName(), sid, null);
												
												if (nstamp == null)
													return;
												
												BigDecimal stamp = Struct.objectToDecimal(ByteUtil.extractValue(nstamp));
												
												if (stamp == null)
													return;
												
												if (UtilitiesAdapter.this.conn.getAsBooleanOrFalse(DB_GLOBAL_RECORD, did, table, id, schema.getName(), sid, stamp, "Retired"))
													return;
												
												if (!UtilitiesAdapter.this.conn.isSet(DB_GLOBAL_RECORD, did, table, id, schema.getName(), sid, stamp, "Data"))
													return;
														
												Object value = UtilitiesAdapter.this.conn.get(DB_GLOBAL_RECORD, did, table, id, schema.getName(), sid, stamp, "Data");
												Object from = UtilitiesAdapter.this.conn.get(DB_GLOBAL_RECORD, did, table, id, schema.getName(), sid, stamp, "From");
												Object to = UtilitiesAdapter.this.conn.get(DB_GLOBAL_RECORD, did, table, id, schema.getName(), sid, stamp, "To");
												
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
												UtilitiesAdapter.this.conn.inc(DB_GLOBAL_INDEX_SUB, did, table, schema.getName(), value);
												UtilitiesAdapter.this.conn.set(DB_GLOBAL_INDEX_SUB, did, table, schema.getName(), value, id, sid, range);
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
