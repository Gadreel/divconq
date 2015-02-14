package divconq.db.update;

import java.util.UUID;

import org.joda.time.DateTime;

import divconq.lang.BigDateTime;
import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;

/**
 * Base class for supporting the four database structures: StaticScalar, StaticList,
 * DynamicScalar and DynamicList
 * 
 * @author Andy
 *
 */
public class FieldRequest {
	protected String name = null;
	protected String subkey = null;
	protected Object value = null;
	protected BigDateTime from = null;
	protected BigDateTime to = null;
	protected FieldTags tags = null;
	protected boolean retired = false;
	protected boolean updateOnly = false;
	
	public String getName() {
		return this.name;
	}
	
	public FieldRequest withValue(Object v) {
		this.value = v;
		return this;
	}
	
	public FieldRequest withName(String v) {
		this.name = v;
		return this;
	}
	
	public FieldRequest withTags(FieldTags v) {
		this.tags = v;
		return this;
	}
	
	public FieldRequest withRetired() {
		this.retired = true;
		return this;
	}
	
	public FieldRequest withUpdateOnly() {
		this.updateOnly = true;
		return this;
	}
	
	public FieldRequest withFrom(BigDateTime v) {
		this.from = v;
		return this;
	}
	
	public FieldRequest withTo(BigDateTime v) {
		this.to = v;
		return this;
	}
	
	public FieldRequest withFrom(DateTime v) {
		this.from = new BigDateTime(v);
		return this;
	}
	
	public FieldRequest withTo(DateTime v) {
		this.to = new BigDateTime(v);
		return this;
	}
	
	public FieldRequest withSubKey(String v) {
		this.subkey = v;
		return this;
	}
	
	public FieldRequest withRandomSubKey() {
		this.subkey = UUID.randomUUID().toString().replace("-", "");
		return this;
	}
	
	public FieldRequest() {		
	}
	
	public RecordStruct getParams(RecordStruct fields) {
		RecordStruct data = new RecordStruct( 
				new FieldStruct("Data", this.value)
		);
		
		if (this.tags != null)
			data.setField("Tags", this.tags);
		
		if (this.retired)
			data.setField("Retired", true);
		
		if (this.updateOnly)
			data.setField("UpdateOnly", true);
		
		// static scalar stops here
		if (this.subkey == null)
			return data;
		
		// dynamic or list
		if (!this.retired) {
			if (this.from != null) 
				data.setField("From", this.from);
			
			if (this.to != null) 
				data.setField("To", this.to);
		}
		
		RecordStruct ret = fields.getFieldAsRecord(this.name);
		
		if (ret == null)
			ret = new RecordStruct();

		ret.setField(this.subkey, data);

		return ret;
	}
}
