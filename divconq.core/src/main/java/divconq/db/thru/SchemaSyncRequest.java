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
package divconq.db.thru;

import divconq.db.Constants;
import divconq.db.IDatabaseRequest;
import divconq.hub.Hub;
import divconq.locale.LocaleInfo;
import divconq.schema.DatabaseSchema;
import divconq.schema.DbField;
import divconq.schema.DbFilter;
import divconq.schema.DbProc;
import divconq.schema.DbTable;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;

/**
 * TODO remove - used only with M
 * 
 * This is a convenience class that builds the entire schema update for
 * you.  Just create and submit.
 * 
 * Schema refreshes are to be used by developers only, and are used to take
 * updates to the local (dev) schema and install them to the database.
 * Only data relevant to the database is uploaded, such as stored procedures
 * and filters as well as locale dictionaries.
 * 
 * @author Andy
 *
 */
public class SchemaSyncRequest implements IDatabaseRequest {
	@Override
	public boolean hasDomain() {
		return true;
	}
	
	@Override
	public String getDomain() {
		return Constants.DB_GLOBAL_ROOT_DOMAIN;
	}
	
	public SchemaSyncRequest() {
	}
	
	@Override
	public RecordStruct buildParams() {
		ListStruct procs = new ListStruct();
		ListStruct recfilters = new ListStruct();
		ListStruct whrfilters = new ListStruct();
		ListStruct reccomposers = new ListStruct();
		ListStruct selcomposers = new ListStruct();
		ListStruct whrcomposers = new ListStruct();
		ListStruct collectors = new ListStruct();
		ListStruct tables = new ListStruct();
		ListStruct locales = new ListStruct();
		
		DatabaseSchema dbs = Hub.instance.getSchema().getDb();
		
		for (DbProc proc : dbs.getProcedures()) {
			procs.addItem(new RecordStruct(
					new FieldStruct("Name", proc.name),
					new FieldStruct("Execute", proc.execute)
			));
		}
				
		for (DbFilter filter : dbs.getRecordFilters()) {
			recfilters.addItem(new RecordStruct(
					new FieldStruct("Name", filter.name),
					new FieldStruct("Execute", filter.execute)
			));
		}
		
		for (DbFilter filter : dbs.getWhereFilters()) {
			whrfilters.addItem(new RecordStruct(
					new FieldStruct("Name", filter.name),
					new FieldStruct("Execute", filter.execute)
			));
		}
		
		for (DbFilter filter : dbs.getRecordComposers()) {
			reccomposers.addItem(new RecordStruct(
					new FieldStruct("Name", filter.name),
					new FieldStruct("Execute", filter.execute)
			));
		}
		
		for (DbFilter filter : dbs.getSelectComposers()) {
			selcomposers.addItem(new RecordStruct(
					new FieldStruct("Name", filter.name),
					new FieldStruct("Execute", filter.execute)
			));
		}
		
		for (DbFilter filter : dbs.getWhereComposers()) {
			whrcomposers.addItem(new RecordStruct(
					new FieldStruct("Name", filter.name),
					new FieldStruct("Execute", filter.execute)
			));
		}
		
		for (DbFilter filter : dbs.getCollectors()) {
			collectors.addItem(new RecordStruct(
					new FieldStruct("Name", filter.name),
					new FieldStruct("Execute", filter.execute)
			));
		}
		
		for (DbTable table : dbs.getTables()) {
			ListStruct fields = new ListStruct();
			
			for (DbField fld : table.fields.values()) {
				if (StringUtil.isEmpty(fld.name) || StringUtil.isEmpty("Type"))
					continue;		// TODO error
				
				RecordStruct field = new RecordStruct();
				
				field.setField("Name", fld.name);
				field.setField("Type", fld.type);
				
				if (fld.indexed)
					field.setField("Indexed", true);
				
				if (fld.required)
					field.setField("Required", true);
				
				if (fld.list)
					field.setField("List", true);
				
				if (fld.dynamic)
					field.setField("Dynamic", true);
				
				if (StringUtil.isNotEmpty(fld.typeid) && !fld.type.equals(fld.typeid))
					field.setField("TypeId", fld.typeid);
				
				if (StringUtil.isNotEmpty(fld.fkey))
					field.setField("ForeignKey", fld.fkey);
				
				fields.addItem(field);
			}
			
			tables.addItem(new RecordStruct(
					new FieldStruct("Name", table.name),
					new FieldStruct("Fields", fields)
			));
		}
		
		for (LocaleInfo locale : Hub.instance.getDictionary().getLocales()) {
			ListStruct tokens = new ListStruct();
			
			for (String token : locale.getTokens()) {
				tokens.addItem(new RecordStruct(
						new FieldStruct("Key", token),
						new FieldStruct("Value", locale.get(token))
				));
			}
			
			locales.addItem(new RecordStruct(
					new FieldStruct("Name", locale.getName()),
					new FieldStruct("Tokens", tokens)
			));
		}
		
		return new RecordStruct(
				new FieldStruct("Procs", procs),
				new FieldStruct("RecordFilters", recfilters),
				new FieldStruct("WhereFilters", whrfilters),
				new FieldStruct("RecordComposers", reccomposers),
				new FieldStruct("SelectComposers", selcomposers),
				new FieldStruct("WhereComposers", whrcomposers),
				new FieldStruct("Collectors", collectors),
				new FieldStruct("Tables", tables),
				new FieldStruct("Locales", locales)
		);
	}
	
	@Override
	public boolean isReplicate() {
		return true;
	}

	@Override
	public String getProcedure() {
		return "dcSchemaUpdate";
	}
}
