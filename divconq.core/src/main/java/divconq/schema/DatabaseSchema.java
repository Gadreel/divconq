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

import java.util.Collection;
import java.util.HashMap;

import divconq.lang.op.OperationResult;
import divconq.util.StringUtil;
import divconq.xml.XAttribute;
import divconq.xml.XElement;

public class DatabaseSchema {
	protected SchemaManager man = null;
	protected HashMap<String, DbProc> procs = new HashMap<String, DbProc>();
	protected HashMap<String, DbFilter> recfilters = new HashMap<String, DbFilter>();
	protected HashMap<String, DbFilter> whrfilters = new HashMap<String, DbFilter>();
	protected HashMap<String, DbFilter> reccomposers = new HashMap<String, DbFilter>();
	protected HashMap<String, DbFilter> selcomposers = new HashMap<String, DbFilter>();
	protected HashMap<String, DbFilter> whrcomposers = new HashMap<String, DbFilter>();
	protected HashMap<String, DbFilter> collectors = new HashMap<String, DbFilter>();
	protected HashMap<String, DbTable> tables = new HashMap<String, DbTable>();
	
	public Collection<DbProc> getProcedures() {
		return this.procs.values();
	}
	
	public Collection<DbFilter> getRecordFilters() {
		return this.recfilters.values();
	}
	
	public Collection<DbFilter> getWhereFilters() {
		return this.whrfilters.values();
	}

	public Collection<DbFilter> getRecordComposers() {
		return this.reccomposers.values();
	}

	public Collection<DbFilter> getSelectComposers() {
		return this.selcomposers.values();
	}

	public Collection<DbFilter> getWhereComposers() {
		return this.whrcomposers.values();
	}

	public Collection<DbFilter> getCollectors() {
		return this.collectors.values();
	}
	
	public Collection<DbTable> getTables() {
		return this.tables.values();
	}
	
	public DatabaseSchema(SchemaManager man) {
		this.man = man;
	}
	
	public void load(OperationResult or, Schema schema, XElement db) {
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
				dt.load(or, dtel);
			else {
				dt = this.man.loadDataType(or, schema, dtel);
				
				// automatically add Id, Retired, etc to tables 
				dt.load(or,
						new XElement("Table",
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
						)
				);
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
					opt.request = this.man.loadDataType(or, schema, req);
				
				XElement resp = procel.find("Response", "ListResponse", "RecResponse");
				
				if (resp != null)
					opt.response = this.man.loadDataType(or, schema, resp);
			}			
		}			
		
		for (XElement procel : db.selectAll("RecordFilter")) {
			String sname = procel.getAttribute("Name");
			
			if (StringUtil.isEmpty(sname))
				continue;			
			
			DbFilter opt = new DbFilter();
			opt.name = sname;
			opt.execute = procel.getAttribute("Execute");
			opt.table = procel.getAttribute("Table");
			
			this.recfilters.put(sname, opt);
		}			
		
		for (XElement procel : db.selectAll("WhereFilter")) {
			String sname = procel.getAttribute("Name");
			
			if (StringUtil.isEmpty(sname))
				continue;			
			
			DbFilter opt = new DbFilter();
			opt.name = sname;
			opt.execute = procel.getAttribute("Execute");
			opt.table = procel.getAttribute("Table");
			
			this.whrfilters.put(sname, opt);
		}			
		
		for (XElement procel : db.selectAll("RecordComposer")) {
			String sname = procel.getAttribute("Name");
			
			if (StringUtil.isEmpty(sname))
				continue;			
			
			DbFilter opt = new DbFilter();
			opt.name = sname;
			opt.execute = procel.getAttribute("Execute");
			opt.table = procel.getAttribute("Table");
			
			this.reccomposers.put(sname, opt);
		}			
		
		for (XElement procel : db.selectAll("SelectComposer")) {
			String sname = procel.getAttribute("Name");
			
			if (StringUtil.isEmpty(sname))
				continue;			
			
			DbFilter opt = new DbFilter();
			opt.name = sname;
			opt.execute = procel.getAttribute("Execute");
			opt.table = procel.getAttribute("Table");
			
			this.selcomposers.put(sname, opt);
		}			
		
		for (XElement procel : db.selectAll("WhereComposer")) {
			String sname = procel.getAttribute("Name");
			
			if (StringUtil.isEmpty(sname))
				continue;			
			
			DbFilter opt = new DbFilter();
			opt.name = sname;
			opt.execute = procel.getAttribute("Execute");
			opt.table = procel.getAttribute("Table");
			
			this.whrcomposers.put(sname, opt);
		}			
		
		for (XElement procel : db.selectAll("Collector")) {
			String sname = procel.getAttribute("Name");
			
			if (StringUtil.isEmpty(sname))
				continue;			
			
			DbFilter opt = new DbFilter();
			opt.name = sname;
			opt.execute = procel.getAttribute("Execute");
			opt.table = procel.getAttribute("Table");
			
			this.collectors.put(sname, opt);
		}			
	}

	public void compile(OperationResult mr) {
		for (DbProc s : this.procs.values()) {
			if (s.request != null)
				s.request.compile(mr);
			
			if (s.response != null)
				s.response.compile(mr);
		}
		
		for (DbTable t : this.tables.values()) {
			t.compile(mr, this.man);
		}
	}

	public DataType getProcRequestType(String name) {
		DbProc proc = this.getProc(name);
		
		if (proc != null)
			return proc.request;
		
		return null;
	}

	public DataType getResponseType(String name) {
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
		if (StringUtil.isEmpty(table))
			return null;
		
		DbTable tbl = this.tables.get(table);
		
		return tbl.fields.get(field);
	}
	
	public DbProc getProc(String name) {
		if (StringUtil.isEmpty(name))
			return null;
		
		return this.procs.get(name);
	}
}
