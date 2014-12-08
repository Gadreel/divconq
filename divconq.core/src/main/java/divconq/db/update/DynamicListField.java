package divconq.db.update;

import org.joda.time.DateTime;

import divconq.lang.BigDateTime;
import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;

/**
 * Use this field to update DynamicList structures in dcDatabase.
 *  
 * @author Andy
 *
 */
public class DynamicListField extends ListField {
	protected BigDateTime from = null;
	protected BigDateTime to = null;
	
	/**
	 * @return date data became valid (null if unknown, or if from beginning of record's life)
	 */
	public BigDateTime getFrom() {
		return this.from;
	}
	
	/**
	 * @return date data ceased to be valid (null if unknown, or if until end of record's life)
	 */
	public BigDateTime getTo() {
		return this.to;
	}
	
	/**
	 * @param name of field
	 * @param subkey id for this item in the list (leave null to assign new id)
	 * @param value of field
	 * @param from date data became valid (null if unknown, or if from beginning of record's life)
	 * @param to date data ceased to be valid (null if unknown, or if until end of record's life)
	 * @param tags meta data for the field (alphanumeric and ':' are allowed in tag)
	 * 
	 */
	public DynamicListField(String name, String subkey, Object value, BigDateTime from, BigDateTime to, FieldTags tags) {
		super(name, subkey, value, tags);
		this.from = from;
		this.to = to;
	}
	
	/**
	 * @param name of field
	 * @param subkey id for this item in the list (leave null to assign new id)
	 * @param value of field
	 * @param from date data became valid (null if unknown, or if from beginning of record's life)
	 * @param to date data ceased to be valid (null if unknown, or if until end of record's life)
	 * @param tags meta data for the field (alphanumeric and ':' are allowed in tag)
	 * 
	 */
	public DynamicListField(String name, String subkey, Object value, DateTime from, DateTime to, FieldTags tags) {
		super(name, subkey, value, tags);
		this.from = (from != null) ? new BigDateTime(from) : null;
		this.to = (to != null) ? new BigDateTime(to) : null;
	}
	
	/**
	 * @param name of field
	 * @param subkey id for this item in the list (leave null to assign new id)
	 * @param value of field
	 * @param from date data became valid (null if unknown, or if from beginning of record's life)
	 * @param to date data ceased to be valid (null if unknown, or if until end of record's life)
	 * 
	 */
	public DynamicListField(String name, String subkey, Object value, BigDateTime from, BigDateTime to) {
		super(name, subkey, value);
		this.from = from;
		this.to = to;
	}
	
	/**
	 * @param name of field
	 * @param subkey id for this item in the list (leave null to assign new id)
	 * @param value of field
	 * @param from date data became valid (null if unknown, or if from beginning of record's life)
	 * @param to date data ceased to be valid (null if unknown, or if until end of record's life)
	 * 
	 */
	public DynamicListField(String name, String subkey, Object value, DateTime from, DateTime to) {
		super(name, subkey, value);
		this.from = (from != null) ? new BigDateTime(from) : null;
		this.to = (to != null) ? new BigDateTime(to) : null;
	}
	
	/**
	 * @param name of field
	 * @param subkey id for this item in the list (leave null to assign new id)
	 * @param value of field
	 * @param from date data became valid (null if unknown, or if from beginning of record's life)
	 * @param tags meta data for the field (alphanumeric and ':' are allowed in tag)
	 * 
	 */
	public DynamicListField(String name, String subkey, Object value, BigDateTime from, FieldTags tags) {
		super(name, subkey, value, tags);
		this.from = from;
	}
	
	/**
	 * @param name of field
	 * @param subkey id for this item in the list (leave null to assign new id)
	 * @param value of field
	 * @param from date data became valid (null if unknown, or if from beginning of record's life)
	 * @param tags meta data for the field (alphanumeric and ':' are allowed in tag)
	 * 
	 */
	public DynamicListField(String name, String subkey, Object value, DateTime from, FieldTags tags) {
		super(name, subkey, value, tags);
		this.from = (from != null) ? new BigDateTime(from) : null;
	}
	
	/**
	 * @param name of field
	 * @param subkey id for this item in the list (leave null to assign new id)
	 * @param value of field
	 * @param from date data became valid (null if unknown, or if from beginning of record's life)
	 * 
	 */
	public DynamicListField(String name, String subkey, Object value, BigDateTime from) {
		super(name, subkey, value);
		this.from = from;
	}
	
	/**
	 * @param name of field
	 * @param subkey id for this item in the list (leave null to assign new id)
	 * @param value of field
	 * @param from date data became valid (null if unknown, or if from beginning of record's life)
	 * 
	 */
	public DynamicListField(String name, String subkey, Object value, DateTime from) {
		super(name, subkey, value);
		this.from = (from != null) ? new BigDateTime(from) : null;
	}
	
	/**
	 * @param name of field
	 * @param subkey id for this item in the list (leave null to assign new id)
	 * @param value of field
	 * @param tags meta data for the field (alphanumeric and ':' are allowed in tag)
	 * 
	 */
	public DynamicListField(String name, String subkey, Object value, FieldTags tags) {
		super(name, subkey, value, tags);
	}
	
	/**
	 * @param name of field
	 * @param subkey id for this item in the list (leave null to assign new id)
	 * @param value of field
	 * 
	 */
	public DynamicListField(String name, String subkey, Object value) {
		super(name, subkey, value);
	}
	
	/**
	 * Mark a field as Retired.
	 * 
	 * @param name of field
	 * @param subkey id for this item in the list (leave null to assign new id)
	 * @param tags meta data for the field (alphanumeric and ':' are allowed in tag)
	 * 
	 */
	public DynamicListField(String name, String subkey, FieldTags tags) {
		super(name, subkey, tags);
	}
	
	/**
	 * Mark a field as Retired.
	 * 
	 * @param name of field
	 * @param subkey id for this item in the list (leave null to assign new id)
	 * 
	 */
	public DynamicListField(String name, String subkey) {
		super(name, subkey);
	}

	@Override
	public RecordStruct getParams(RecordStruct fields) {
		RecordStruct data = new RecordStruct( 
				new FieldStruct("Data", this.value)
		);
		
		if (this.tags != null)
			data.setField("Tags", this.tags);
		
		if (this.retired)
			data.setField("Retired", true);
		else {
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

	// TODO tags
	public static DbField buildImport(String name, RecordStruct data) {
		DynamicListField fld = new DynamicListField(name, data.getFieldAsString("Sid"), data.getFieldAsAny("Data"));
		
		if (data.hasField("From"))
			fld.from = data.getFieldAsBigDateTime("From");
		
		if (data.hasField("To"))
			fld.to = data.getFieldAsBigDateTime("To");
		
		return fld;
	}
}
