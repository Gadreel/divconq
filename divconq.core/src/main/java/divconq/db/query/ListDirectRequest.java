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

import org.joda.time.DateTime;

import divconq.db.DataRequest;
import divconq.lang.BigDateTime;
import divconq.struct.RecordStruct;

public class ListDirectRequest extends DataRequest {
	public void setHistorical(boolean v) {
		((RecordStruct)this.parameters).setField("Historical", v);
	}
	
	public ListDirectRequest(String table, ISelectField select) {
		this(table, select, null, null, (BigDateTime)null);
	}
	
	public ListDirectRequest(String table, ISelectField select, WhereExpression where) {
		this(table, select, where, null, (BigDateTime)null);
	}
	
	public ListDirectRequest(String table, ISelectField select, WhereExpression where, ICollector collector) {
		this(table, select, where, collector, (BigDateTime)null);
	}
	
	public ListDirectRequest(String table, ISelectField select, WhereExpression where, BigDateTime when) {
		this(table, select, where, null, when);
	}
	
	public ListDirectRequest(String table, ISelectField select, WhereExpression where, DateTime when) {
		this(table, select, where, null, (when != null) ? new BigDateTime(when) : null);
	}
	
	public ListDirectRequest(String table, ISelectField select, WhereExpression where, ICollector collector, DateTime when) {
		this(table, select, where, collector, (when != null) ? new BigDateTime(when) : null);
	}
	
	public ListDirectRequest(String table, ISelectField select, WhereExpression where, ICollector collector, BigDateTime when) {
		super("dcListDirect");
		
		RecordStruct params = new RecordStruct();
		
		this.parameters = params;
		
		params.setField("Table", table);
		
		if (select != null)
			params.setField("Select", select.getParams());
		
		if (where != null)
			params.setField("Where", where.getFields());
		
		if (collector != null)
			params.setField("Collector", collector.getParams());
		
		if (when != null)
			params.setField("When", when);
		else
			params.setField("When", new BigDateTime(new DateTime()));
	}
}
