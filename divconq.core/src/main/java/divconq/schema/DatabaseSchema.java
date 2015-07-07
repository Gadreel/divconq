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

import divconq.util.StringUtil;
import divconq.xml.XAttribute;
import divconq.xml.XElement;

public class DatabaseSchema {
	protected SchemaManager man = null;
	protected HashMap<String, DbProc> procs = new HashMap<String, DbProc>();
	protected HashMap<String, DbComposer> composers = new HashMap<String, DbComposer>();
	protected HashMap<String, DbCollector> collectors = new HashMap<String, DbCollector>();
	protected HashMap<String, List<DbTrigger>> triggers = new HashMap<String, List<DbTrigger>>();
	protected HashMap<String, DbTable> tables = new HashMap<String, DbTable>();
	
	public Collection<DbProc> getProcedures() {
		return this.procs.values();
	}
	
	public Collection<DbComposer> getComposers() {
		return this.composers.values();
	}
	
	public Collection<DbCollector> getCollectors() {
		return this.collectors.values();
	}
	
	public Collection<DbTable> getTables() {
		return this.tables.values();
	}
	
	public DatabaseSchema(SchemaManager man) {
		this.man = man;
	}
	
	public void load(Schema schema, XElement db) {
		for (XElement dtel : db.selectAll("Table")) {
			String id = dtel.getAttribute("Id");
			
			if (StringUtil.isEmpty(id))
				continue;
			
			DbTable tab = this.tables.get(id);
			
			if (tab == null) {
				tab = new DbTable();
				tab.name = id;
				this.tables.put(id, tab);			
			}
			
			DataType dt = this.man.knownTypes().get(id);
			
			if (dt != null) 
				dt.load(dtel);
			else {
				dt = this.man.loadDataType(schema, dtel);
				
				XElement autoSchema = new XElement("Table",
						new XElement("Field",
								new XAttribute("Name", "Id"),
								new XAttribute("Type", "Id")
						),
						new XElement("Field",
								new XAttribute("Name", "Retired"),
								new XAttribute("Type", "Boolean")
						),
						new XElement("Field",
								new XAttribute("Name", "From"),
								new XAttribute("Type", "BigDateTime"),
								new XAttribute("Indexed", "True")
						),
						new XElement("Field",
								new XAttribute("Name", "To"),
								new XAttribute("Type", "BigDateTime"),
								new XAttribute("Indexed", "True")
						),
						new XElement("Field",
								new XAttribute("Name", "Tags"),
								new XAttribute("Type", "dcTinyString"),
								new XAttribute("List", "True")										
						)
				);

				
				// automatically add Id, Retired, etc to tables 
				dt.load(autoSchema);

				for (XElement fel : autoSchema.selectAll("Field")) 
					tab.addField(fel, dt);
			}

			for (XElement fel : dtel.selectAll("Field")) 
				tab.addField(fel, dt);
		}			
		
		for (XElement secel : db.selectAll("Secure")) {
			String[] tags = secel.hasAttribute("Tags") 
				? secel.getAttribute("Tags").split(",")
				: new String[] { "Guest", "User" };
			
			for (XElement procel : secel.selectAll("Procedure")) {
				String sname = procel.getAttribute("Name");
				
				if (StringUtil.isEmpty(sname))
					continue;			
				
				DbProc opt = new DbProc();
				opt.name = sname;
				opt.execute = procel.getAttribute("Execute");
				opt.securityTags = tags;
				
				this.procs.put(sname, opt);
				
				XElement req = procel.find("Request", "ListRequest", "RecRequest");
				
				if (req != null)
					opt.request = this.man.loadDataType(schema, req);
				
				XElement resp = procel.find("Response", "ListResponse", "RecResponse");
				
				if (resp != null)
					opt.response = this.man.loadDataType(schema, resp);
			}			
			
			for (XElement procel : secel.selectAll("Composer")) {
				String sname = procel.getAttribute("Name");
				
				if (StringUtil.isEmpty(sname))
					continue;			
				
				DbComposer opt = new DbComposer();
				opt.name = sname;
				opt.execute = procel.getAttribute("Execute");
				opt.securityTags = tags;
				
				this.composers.put(sname, opt);
			}			
			
			for (XElement procel : secel.selectAll("Collector")) {
				String sname = procel.getAttribute("Name");
				
				if (StringUtil.isEmpty(sname))
					continue;			
				
				DbCollector opt = new DbCollector();
				opt.name = sname;
				opt.execute = procel.getAttribute("Execute");
				opt.securityTags = tags;
				
				this.collectors.put(sname, opt);
			}			
		}			
		
		for (XElement procel : db.selectAll("Trigger")) {
			String sname = procel.getAttribute("Table");
			
			if (StringUtil.isEmpty(sname))
				continue;			
			
			DbTrigger opt = new DbTrigger();
			opt.op = procel.getAttribute("Operation");
			opt.execute = procel.getAttribute("Execute");
			opt.table = sname;
			
			List<DbTrigger> ll = this.triggers.get(sname);
			
			if (ll == null) {
				ll = new ArrayList<>();
				this.triggers.put(sname, ll);
			}
			
			ll.add(opt);
		}			
	}

	public void compile() {
		for (DbProc s : this.procs.values()) {
			if (s.request != null)
				s.request.compile();
			
			if (s.response != null)
				s.response.compile();
		}
		
		for (DbTable t : this.tables.values()) 
			t.compile(this.man);
	}

	public DataType getProcRequestType(String name) {
		DbProc proc = this.getProc(name);
		
		if (proc != null)
			return proc.request;
		
		return null;
	}

	public DataType getProcResponseType(String name) {
		DbProc proc = this.getProc(name);
		
		if (proc != null)
			return proc.response;
		
		return null;
	}
	
	public DbTable getTable(String table) {
		if (StringUtil.isEmpty(table))
			return null;
		
		return this.tables.get(table);
	}
	
	public DbField getField(String table, String field) {
		if (StringUtil.isEmpty(table) || StringUtil.isEmpty(field))
			return null;
		
		DbTable tbl = this.tables.get(table);
		
		if (tbl != null)		
			return tbl.fields.get(field);
		
		return null;
	}

	public boolean hasTable(String table) {
		if (StringUtil.isEmpty(table))
			return false;
		
		return this.tables.containsKey(table);
	}
	
	// returns a copy list, you (caller) can own the list 
	public List<DbTrigger> getTriggers(String table, String operation) {
		if (StringUtil.isEmpty(table) || StringUtil.isEmpty(operation))
			return null;
		
		List<DbTrigger> ret = new ArrayList<>();
		List<DbTrigger> tbl = this.triggers.get(table);
		
		if (tbl != null)
			for (DbTrigger t : tbl)
				if (t.op.equals(operation))
					ret.add(t);
		
		return ret;
	}
	
	// returns a copy list, you (caller) can own the list 
	public List<DbField> getFields(String table) {
		if (StringUtil.isEmpty(table))
			return null;
		
		DbTable tbl = this.tables.get(table);
		
		if (tbl == null)
			return null;
		
		return new ArrayList<>(tbl.fields.values());
	}
	
	public DbProc getProc(String name) {
		if (StringUtil.isEmpty(name))
			return null;
		
		return this.procs.get(name);
	}
	
	public DbComposer getComposer(String name) {
		if (StringUtil.isEmpty(name))
			return null;
		
		return this.composers.get(name);
	}
	
	public DbCollector getCollector(String name) {
		if (StringUtil.isEmpty(name))
			return null;
		
		return this.collectors.get(name);
	}
}
