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
package divconq.ds;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import divconq.hub.Hub;
import divconq.lang.OperationResult;
import divconq.log.Logger;
import divconq.scheduler.ISchedule;
import divconq.struct.CompositeParser;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.struct.builder.XmlStreamBuilder;
import divconq.util.StringUtil;

// TODO rework guts so we use LeevlDb instead - provide an index option too
//     [where / is NULL or such]
//
//  [domain]/table/[table]/[record id] = [RecordStruct]
//  [domain]/index/[table]/[value]/[record id] = [record id]
//
//  [domain]/pairs/[key] = [value]
//
//  [root domain]/table/lastid = [last id]   - all id's come from one original
//
// TODO could even expand to do a full database thing like dcDb, etc
//
public class LocalDataStore {
	protected ReentrantLock lock = new ReentrantLock();
	protected Map<String, Table> tables = new HashMap<String, Table>();
	protected boolean changed = false;
	protected String file = null;
	protected ISchedule sched = null;
	
	public void load(String file) {
		this.file = file;
		
		File site = new File(file);
		
		if (!site.exists())
			return;
		
		try {
			InputStream dstrm = new FileInputStream(site);
			ListStruct recs = (ListStruct) CompositeParser.parseXml(dstrm);
			dstrm.close();
			
			if (recs == null)
				return;
			
			for (Struct rec : recs.getItems()) {
				if (!(rec instanceof RecordStruct))
					continue;
				
				RecordStruct r = (RecordStruct)rec;
				
				String name = r.getFieldAsString("Name");
				
				if (StringUtil.isEmpty(name))
					continue;
				
				Table t = new Table();
				t.load(r);
				
				this.tables.put(name, t);
			}
		}
		catch (Exception x) {
			Logger.error("Problem loading data store: " + file + ", error: " + x);
		}
		
		/* TODO restore someday?
		this.sched = Hub.instance.getScheduler().runEvery(new IWork() {
			@Override
			public void run(Task task) {
				try {
					LocalDataStore.this.save();
				}
				finally {
					task.complete();
				}
			}
		}, 30);
		*/
	}
	
	public void stop(OperationResult or) {
		/* TODO restore someday?
		if (this.sched != null)
			this.sched.task().cancel();
		*/
		
		this.save();
	}
	
	public void save() {
		this.lock.lock();
		
		try {
			if (this.changed) {
				ListStruct tables = new ListStruct();
				
				for (Table tbl : this.tables.values()) 
					tables.addItem(tbl.save());
				
				try {
					PrintStream os = new PrintStream(this.file);			
					XmlStreamBuilder builder = new XmlStreamBuilder(os, true);  
					tables.toBuilder(builder);
					os.flush();
					os.close();
				}
				catch (Exception x) {
					Logger.error("Problem saving data store: " + this.file);
				}
			}
		}
		finally {
			this.lock.unlock();
		}
	}
	
	public void setRecord(String domain, String table, RecordStruct record) {
		if (StringUtil.isNotEmpty(domain))
			table = table + "#" + domain;
		
		this.lock.lock();
		
		try {
			Table t = this.tables.get(table);
			
			if (t == null) {
				t = new Table(table);
				this.tables.put(table, t);
			}
				
			t.set(record);
			
			this.changed = true;
		}
		finally {
			this.lock.unlock();
		}
	}
	
	public RecordStruct getRecord(String domain, String table, String id) {
		if (StringUtil.isNotEmpty(domain))
			table = table + "#" + domain;
		
		RecordStruct res = null;
		
		this.lock.lock();
		
		try {
			Table t = this.tables.get(table);
			
			if (t != null)
				res = t.get(id);
		}
		finally {
			this.lock.unlock();
		}
		
		return res;
	}
	
	public Collection<RecordStruct> getAll(String domain, String table) {
		if (StringUtil.isNotEmpty(domain))
			table = table + "#" + domain;
		
		Collection<RecordStruct> res = null;
		
		this.lock.lock();

		try {
			Table t = this.tables.get(table);
			
			if (t != null)
				res = t.getAll();
			else
				res =  new ArrayList<RecordStruct>();
		}
		finally {
			this.lock.unlock();
		}
		
		return res;
	}
	
	public void deleteRecord(String domain, String table, String id) {
		if (StringUtil.isNotEmpty(domain))
			table = table + "#" + domain;
		
		this.lock.lock();
		
		try {
			Table t = this.tables.get(table);
			
			if (t != null)
				t.delete(id);
			
			this.changed = true;
		}
		finally {
			this.lock.unlock();
		}
	}
	
	public RecordStruct newRecord(String table) {
		// use root table - all ids are unique for that table - even across domains
		Table t = this.tables.get(table);
		
		if (t == null) {
			t = new Table(table);
			this.tables.put(table, t);
		}
		
		RecordStruct rec = new RecordStruct( new FieldStruct("Id", t.allocateId()));
		
		this.lock.lock();
		this.changed = true;
		this.lock.unlock();
		
		return rec;
	}
	
	public class Table {
		protected String name = null;
		protected Map<String, RecordStruct> records = new HashMap<String, RecordStruct>();
		protected AtomicLong lastid = new AtomicLong();
		
		public Table() {
		}
		
		public Table(String name) {
			this.name = name;
		}
		
		public void load(RecordStruct table) {
			this.name = table.getFieldAsString("Name");
			
			if (table.hasField("LastId"))
				this.lastid.set(table.getFieldAsInteger("LastId"));
			
			ListStruct recs = table.getFieldAsList("Records");
			
			if (recs == null)
				return;
			
			for (Struct rec : recs.getItems()) {
				if (!(rec instanceof RecordStruct))
					continue;
				
				RecordStruct r = (RecordStruct)rec;
				
				String id = r.getFieldAsString("Id");
				
				if (StringUtil.isEmpty(id))
					continue;
				
				this.records.put(id, r);
			}
		}
		
		public RecordStruct save() {
			RecordStruct rec = new RecordStruct(
					new FieldStruct("Name", this.name),
					new FieldStruct("Records", new ListStruct(this.records.values()))
			);
			
			if (this.lastid.get() > 0)
				rec.setField("LastId", this.lastid);
			
			return rec;
		}
		
		public String allocateId() {
			return Hub.instance.getResources().getHubId() + "_" + StringUtil.leftPad(this.lastid.incrementAndGet() + "", 15, '0'); 
		}
		
		public void set(RecordStruct rec) {
			rec = (RecordStruct)rec.deepCopy();
			
			String id = rec.getFieldAsString("Id");
			
			if (StringUtil.isEmpty(id))
				return;
			
			this.records.put(id, rec);
		}
		
		public Collection<RecordStruct> getAll() {
			List<RecordStruct> recs = new ArrayList<RecordStruct>();
			
			for (RecordStruct rec : this.records.values()) 
				recs.add((RecordStruct)rec.deepCopy());
			
			return recs;
		}
		
		public RecordStruct get(String id) {
			RecordStruct rec = this.records.get(id);
			
			if (rec != null)
				return (RecordStruct) rec.deepCopy();
			
			return null;
		}
		
		public void delete(String id) {
			this.records.remove(id);
		}
	}
}
