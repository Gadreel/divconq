package divconq.db.query;

import divconq.struct.RecordStruct;
import divconq.struct.Struct;

/**
 * A database foreign key field to use with a subquery.
 * 
 * @author Andy
 *
 */
public class SelectSubquery extends SelectFields implements ISelectField {
	protected RecordStruct subquery = new RecordStruct();
	
	public SelectSubquery withField(String v) {
		this.subquery.setField("Field", v);		
		return this;
	}
	
	public SelectSubquery withName(String v) {
		this.subquery.setField("Name", v);		
		return this;
	}

	@Override
	public SelectSubquery withSelect(ISelectField... items) {
		super.withSelect(items);		
		return this;
	}
	
	@Override
	public Struct getParams() {
		this.subquery.setField("Select", this.fields);
		
		return this.subquery;
	}
}
