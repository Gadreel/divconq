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

public class ListRequest extends DataRequest {
	public long getOffset() {
		return ((RecordStruct)this.parameters).hasField("Offset") ? ((RecordStruct)this.parameters).getFieldAsInteger("Offset") : 0;
	}
	
	public void setOffset(long v) {
		((RecordStruct)this.parameters).setField("Offset", v);
	}
	
	public long getPageSize() {
		return ((RecordStruct)this.parameters).hasField("PageSize") ? ((RecordStruct)this.parameters).getFieldAsInteger("PageSize") : 100;
	}
	
	public void setPageSize(long v) {
		((RecordStruct)this.parameters).setField("PageSize", v);
	}
	
	public boolean isCacheEnabled() {
		return ((RecordStruct)this.parameters).hasField("CacheEnabled") ? ((RecordStruct)this.parameters).getFieldAsBoolean("CacheEnabled") : false;
	}
	
	public void setCacheEnabled(boolean v) {
		((RecordStruct)this.parameters).setField("CacheEnabled", v);
	}
	
	public long getCacheId() {
		return ((RecordStruct)this.parameters).hasField("CacheId") ? ((RecordStruct)this.parameters).getFieldAsInteger("CacheId") : null;
	}
	
	public void setCacheId(long v) {
		((RecordStruct)this.parameters).setField("CacheEnabled", true);
		((RecordStruct)this.parameters).setField("CacheId", v);
	}
	
	public void setHistorical(boolean v) {
		((RecordStruct)this.parameters).setField("Historical", v);
	}
	
	public ListRequest(String table, ISelectField select) {
		this(table, select, null, null, null, (BigDateTime)null);
	}
	
	public ListRequest(String table, ISelectField select, OrderFields order) {
		this(table, select, order, null, null, (BigDateTime)null);
	}
	
	public ListRequest(String table, ISelectField select, WhereExpression where) {
		this(table, select, null, where, null, (BigDateTime)null);
	}
	
	public ListRequest(String table, ISelectField select, OrderFields order, WhereExpression where) {
		this(table, select, order, where, null, (BigDateTime)null);
	}
	
	public ListRequest(String table, ISelectField select, OrderFields order, WhereExpression where, ICollector collector) {
		this(table, select, order, where, collector, (BigDateTime)null);
	}
	
	public ListRequest(String table, ISelectField select, OrderFields order, WhereExpression where, BigDateTime when) {
		this(table, select, order, where, null, when);
	}
	
	public ListRequest(String table, ISelectField select, OrderFields order, WhereExpression where, DateTime when) {
		this(table, select, order, where, null, (when != null) ? new BigDateTime(when) : null);
	}
	
	public ListRequest(String table, ISelectField select, OrderFields order, WhereExpression where, ICollector collector, DateTime when) {
		this(table, select, order, where, collector, (when != null) ? new BigDateTime(when) : null);
	}
	
	public ListRequest(String table, ISelectField select, OrderFields order, WhereExpression where, ICollector collector, BigDateTime when) {
		super("dcList");
		
		RecordStruct params = new RecordStruct();
		
		this.parameters = params;
		
		params.setField("Table", table);
		
		if (select != null)
			params.setField("Select", select.getParams());
		
		if (order != null)
			params.setField("Order", order.getFields());
		
		if (where != null)
			params.setField("Where", where.getFields());
		
		if (collector != null)
			params.setField("Collector", collector.getParams());
		
		if (when != null)
			params.setField("When", when);
		else
			params.setField("When", new BigDateTime(new DateTime()));
	}
	
	public void nextPage() {
		if (!this.isCacheEnabled())
			return;
		
		long newoffset = this.getOffset() + this.getPageSize();
		this.setOffset(newoffset);
	}
			
	public void prevPage() {
		if (!this.isCacheEnabled())
			return;
		
		long newoffset = this.getOffset() - this.getPageSize();
		
		if (newoffset < 0)
			newoffset = 0;
		
		this.setOffset(newoffset);
	}
}
