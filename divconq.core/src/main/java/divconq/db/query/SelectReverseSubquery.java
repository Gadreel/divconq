package divconq.db.query;

import divconq.struct.RecordStruct;
import divconq.struct.Struct;

/**
 * A database foreign key field to use with a subquery.
 * 
 * @author Andy
 *
 */
public class SelectReverseSubquery extends SelectFields implements ISelectField {
	protected RecordStruct subquery = new RecordStruct();
	
	public SelectReverseSubquery withField(String v) {
		this.subquery.setField("Field", v);		
		return this;
	}
	
	public SelectReverseSubquery withKeyField(String v) {
		this.subquery.setField("KeyField", v);		
		return this;
	}
	
	public SelectReverseSubquery withName(String v) {
		this.subquery.setField("Name", v);		
		return this;
	}
	
	public SelectReverseSubquery withTable(String v) {
		this.subquery.setField("Table", v);		
		return this;
	}
	
	public SelectReverseSubquery withSelect(SelectFields flds) {
		this.fields = flds.fields;
		return this;
	}

	@Override
	public SelectReverseSubquery withSelect(ISelectField... items) {
		super.withSelect(items);		
		return this;
	}
	
	@Override
	public Struct getParams() {
		this.subquery.setField("Select", this.fields);
		
		return this.subquery;
	}
}
