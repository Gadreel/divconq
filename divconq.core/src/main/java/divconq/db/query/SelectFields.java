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
package divconq.db.query;

import divconq.struct.ListStruct;

/**
 * This is a collection of database fields to be selected by a query.  A selected
 * field may be formated and also may hold a subquery.
 * 
 * @author Andy
 *
 */
public class SelectFields {
	protected ListStruct fields = new ListStruct();
	
	/**
	 * @return the selected fields (uses an internal format)
	 */
	public ListStruct getFields() {
		return this.fields;
	}
	
	/**
	 * @param fields/subqueries to use as initial values for select
	 */
	public SelectFields(ISelectField... items) {
		this.addField(items);
	}	
	
	/**
	 * @param fields/subqueries to add to the select
	 */
	public void addField(ISelectField... items) {
		if (items != null)
			for (ISelectField itm : items)
				this.fields.addItem(itm.getParams());
	}
	
	@Override
	public String toString() {
		return this.fields.toString();
	}
		
	public SelectFields withSelect(ISelectField... items) {
		if (items != null)
			for (ISelectField itm : items)
				this.fields.addItem(itm.getParams());
		
		return this;
	}
	
	/**
	 * @param field name of foreign key field
	 */
	public SelectFields withField(String field) {
		SelectField sub = new SelectField()
			.withField(field);
		
		this.fields.addItem(sub.getParams());
		
		return this;
	}
	
	/**
	 * @param field name of foreign key field
	 * @param name display (return) name
	 */
	public SelectFields withField(String field, String name) {
		SelectField sub = new SelectField()
			.withField(field)
			.withName(name);
		
		this.fields.addItem(sub.getParams());
		
		return this;
	}
	
	/**
	 * @param field name of foreign key field
	 * @param name display (return) name
	 * @param format formatting for return value
	 */
	public SelectFields withField(String field, String name, String format) {
		SelectField sub = new SelectField()
			.withField(field)
			.withName(name)
			.withFormat(format);
		
		this.fields.addItem(sub.getParams());
		
		return this;
	}
	
	public SelectFields withField(String field, String name, String format, boolean full) {
		SelectField sub = new SelectField()
			.withField(field)
			.withName(name)
			.withFormat(format)
			.withFull(full);
		
		this.fields.addItem(sub.getParams());
		
		return this;
	}
	
	/**
	 * @param field foreign key field name
	 * @param fields/subqueries to use as initial values for select
	 */
	public SelectFields withSubquery(String field, ISelectField... items) {
		SelectSubquery sub = new SelectSubquery()
			.withField(field)
			.withSelect(items);
		
		this.fields.addItem(sub.getParams());
		
		return this;
	}
	
	/**
	 * @param field foreign key field name
	 * @param name display name
	 * @param fields/subqueries to use as initial values for select
	 */
	public SelectFields withSubquery(String field, String name, ISelectField... items) {
		SelectSubquery sub = new SelectSubquery()
			.withName(name)
			.withField(field)
			.withSelect(items);
		
		this.fields.addItem(sub.getParams());
		
		return this;
	}	
	
	/**
	 * @param field name of foreign key field
	 * @param name display (return) name
	 * @param foreignfield name of foreign field to use for value
	 */
	public SelectFields withForeignField(String field, String name, String foreignfield) {
		SelectForeignField sub = new SelectForeignField()
			.withField(field)
			.withName(name)
			.withForeignField(foreignfield);
		
		this.fields.addItem(sub.getParams());
		
		return this;
	}
	
	/**
	 * @param field name of foreign key field
	 * @param name display (return) name
	 * @param foreignfield name of foreign field to use for value
	 * @param format formatting for return value
	 */
	public SelectFields withForeignField(String field, String name, String foreignfield, String format) {
		SelectForeignField sub = new SelectForeignField()
			.withField(field)
			.withName(name)
			.withForeignField(foreignfield)
			.withFormat(format);
		
		this.fields.addItem(sub.getParams());
		
		return this;
	}
	
	/**
	 * @param field name of foreign key field
	 * @param name display (return) name
	 * @param table foreign table
	 * @param keyfield name of foreign field to use for id lookup
	 * @param foreignfield name of foreign field to use for value
	 */
	public SelectFields withReverseForeignField(String name, String table, String keyfield, String foreignfield) {
		SelectReverseForeignField sub = new SelectReverseForeignField()
			.withField("Id")		// doesn't really mean anything
			.withName(name)
			.withTable(table)
			.withKeyField(keyfield)
			.withForeignField(foreignfield);
		
		this.fields.addItem(sub.getParams());
		
		return this;
	}
	
	/**
	 * @param field name of foreign key field
	 * @param name display (return) name
	 * @param table foreign table
	 * @param keyfield name of foreign field to use for id lookup
	 * @param foreignfield name of foreign field to use for value
	 * @param format formatting for return value
	 */
	public SelectFields withReverseForeignField(String name, String table, String keyfield, String foreignfield, String format) {
		SelectReverseForeignField sub = new SelectReverseForeignField()
		.withField("Id")		// doesn't really mean anything
			.withName(name)
			.withTable(table)
			.withKeyField(keyfield)
			.withForeignField(foreignfield)
			.withFormat(format);
		
		this.fields.addItem(sub.getParams());
		
		return this;
	}
	
	/**
	 * @param composer function to compose response
	 * @param name display (return) name
	 */
	public SelectFields withComposer(String composer, String name) {
		SelectComposer sub = new SelectComposer()
			.withComposer(composer)
			.withName(name);
		
		this.fields.addItem(sub.getParams());
		
		return this;
	}
	
	/**
	 * @param composer function to compose response
	 * @param name display (return) name
	 * @param format formatting for return value
	 */
	public SelectFields withComposer(String composer, String name, String format) {
		SelectComposer sub = new SelectComposer()
			.withComposer(composer)
			.withName(name)
			.withFormat(format);
		
		this.fields.addItem(sub.getParams());
		
		return this;
	}
}
