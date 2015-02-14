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
import divconq.struct.CompositeStruct;
import divconq.struct.RecordStruct;

public class SelectDirectRequest extends DataRequest {
	public SelectDirectRequest() {
		super("dcSelectDirect");
		
		this.parameters = new RecordStruct();
	}
	
	public SelectDirectRequest withTable(String v) {
		((RecordStruct) this.parameters).setField("Table", v);
		return this;
	}
	
	public SelectDirectRequest withId(String v) {
		((RecordStruct) this.parameters).setField("Id", v);
		return this;
	}
	
	public SelectDirectRequest withWhere(WhereExpression v) {
		((RecordStruct) this.parameters).setField("Where", v.getFields());
		return this;
	}
	
	public SelectDirectRequest withCollector(ICollector v) {
		((RecordStruct) this.parameters).setField("Collector", v.getParams());
		return this;
	}
	
	/*
	public SelectDirectRequest withFilter(String v) {
		((RecordStruct) this.parameters).setField("Filter", v);
		return this;
	}
	*/
	
	public SelectDirectRequest withWhen(BigDateTime v) {
		((RecordStruct) this.parameters).setField("When", v);
		return this;
	}
	
	public SelectDirectRequest withWhen(DateTime v) {
		((RecordStruct) this.parameters).setField("When", new BigDateTime(v));
		return this;
	}
	
	public SelectDirectRequest withNow() {
		((RecordStruct) this.parameters).setField("When", BigDateTime.nowDateTime());
		return this;
	}
	
	/*
	public SelectDirectRequest withExtra(Object v) {
		((RecordStruct) this.parameters).setField("Extra", v);
		return this;
	}
	*/
	
	public SelectDirectRequest withSelect(SelectFields v) {
		((RecordStruct) this.parameters).setField("Select", v.getFields());
		return this;
	}
	
	public SelectDirectRequest withCompact(boolean v) {
		((RecordStruct)this.parameters).setField("Compact", v);
		return this;
	}
	
	public SelectDirectRequest withHistorical(boolean v) {
		((RecordStruct)this.parameters).setField("Historical", v);
		return this;
	}
	
	@Override
	public CompositeStruct buildParams() {
		// default in When
		if (!((RecordStruct)this.parameters).hasField("When"))
			((RecordStruct) this.parameters).setField("When", BigDateTime.nowDateTime());
		
		return super.buildParams();
	}
}
