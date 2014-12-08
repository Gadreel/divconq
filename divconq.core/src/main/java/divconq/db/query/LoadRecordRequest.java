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

/**
 * Request that a single database record be loaded.
 * 
 * @author Andy
 *
 */
public class LoadRecordRequest extends DataRequest {	
	public LoadRecordRequest() {
		super("dcLoadRecord");
		
		this.parameters = new RecordStruct();
	}
	
	public LoadRecordRequest withTable(String v) {
		((RecordStruct) this.parameters).setField("Table", v);
		return this;
	}
	
	public LoadRecordRequest withId(String v) {
		((RecordStruct) this.parameters).setField("Id", v);
		return this;
	}
	
	public LoadRecordRequest withFilter(String v) {
		((RecordStruct) this.parameters).setField("Filter", v);
		return this;
	}
	
	public LoadRecordRequest withWhen(BigDateTime v) {
		((RecordStruct) this.parameters).setField("When", v);
		return this;
	}
	
	public LoadRecordRequest withWhen(DateTime v) {
		((RecordStruct) this.parameters).setField("When", new BigDateTime(v));
		return this;
	}
	
	public LoadRecordRequest withNow() {
		((RecordStruct) this.parameters).setField("When", BigDateTime.nowDateTime());
		return this;
	}
	
	public LoadRecordRequest withExtra(Object v) {
		((RecordStruct) this.parameters).setField("Extra", v);
		return this;
	}
	
	public LoadRecordRequest withSelect(SelectFields v) {
		((RecordStruct) this.parameters).setField("Select", v.getFields());
		return this;
	}
	
	public LoadRecordRequest withCompact(boolean v) {
		((RecordStruct)this.parameters).setField("Compact", v);
		return this;
	}
	
	public LoadRecordRequest withHistorical(boolean v) {
		((RecordStruct)this.parameters).setField("Historical", v);
		return this;
	}
	
}
